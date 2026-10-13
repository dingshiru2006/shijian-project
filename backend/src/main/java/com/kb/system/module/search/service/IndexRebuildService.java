package com.kb.system.module.search.service;

import com.kb.system.common.PageResult;
import com.kb.system.module.kb.document.entity.KbDocument;
import com.kb.system.module.kb.document.repository.KbDocumentRepository;
import com.kb.system.module.search.entity.EsRebuildTask;
import com.kb.system.module.search.repository.EsRebuildTaskRepository;
import com.kb.system.module.search.vo.DocumentIndexVO;
import com.kb.system.module.search.vo.RebuildTaskVO;
import com.kb.system.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * ES 索引全量重建服务：
 * 提交任务（发 MQ 消息）-> 后台消费执行 -> 进度/结果落库可查询
 */
@Service
public class IndexRebuildService {

    private static final Logger log = LoggerFactory.getLogger(IndexRebuildService.class);

    private final EsRebuildTaskRepository taskRepository;
    private final KbDocumentRepository documentRepository;
    private final DocumentSearchService searchService;
    private final IndexDocumentBuilder indexDocumentBuilder;
    private final RabbitTemplate rabbitTemplate;
    private final CurrentUser currentUser;

    @Value("${app.es.rebuild-batch-size}")
    private int batchSize;

    public IndexRebuildService(EsRebuildTaskRepository taskRepository,
                               KbDocumentRepository documentRepository,
                               DocumentSearchService searchService,
                               IndexDocumentBuilder indexDocumentBuilder,
                               RabbitTemplate rabbitTemplate,
                               CurrentUser currentUser) {
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.searchService = searchService;
        this.indexDocumentBuilder = indexDocumentBuilder;
        this.rabbitTemplate = rabbitTemplate;
        this.currentUser = currentUser;
    }

    /**
     * 提交全量重建任务：落库 + 发送 RabbitMQ 消息（异步消费）
     */
    @Transactional
    public RebuildTaskVO submitTask() {
        EsRebuildTask task = new EsRebuildTask();
        task.setTaskNo(generateTaskNo());
        task.setTriggerType(1);
        task.setStatus(0);
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setCreatedBy(currentUser.id());
        EsRebuildTask saved = taskRepository.save(task);

        rabbitTemplate.convertAndSend(
                com.kb.system.config.RabbitMQConfig.EXCHANGE_ES_REBUILD,
                com.kb.system.config.RabbitMQConfig.ROUTING_ES_REBUILD,
                saved.getId());
        log.info("索引重建任务已提交: taskNo={}, 消息已发送", saved.getTaskNo());
        return RebuildTaskVO.from(saved);
    }

    /**
     * 启动时自检：ES 索引为空但数据库有文档时，自动执行一次全量索引，
     * 保证全新环境下检索功能开箱即用（无需手动触发重建）。
     */
    public void ensureIndexedOnStartup() {
        try {
            long indexed = searchService.countDocuments();
            long total = documentRepository.count();
            if (indexed > 0 || total == 0) {
                return;
            }
            log.info("ES 索引为空且数据库存在 {} 个文档，启动时自动执行全量索引", total);
            EsRebuildTask task = new EsRebuildTask();
            task.setTaskNo(generateTaskNo());
            task.setTriggerType(2); // 系统启动触发
            task.setStatus(0);
            task.setTotalCount(0);
            task.setSuccessCount(0);
            task.setFailCount(0);
            EsRebuildTask saved = taskRepository.save(task);
            executeRebuild(saved.getId());
        } catch (Exception e) {
            log.warn("启动时自动索引失败（不影响启动，可手动触发重建）", e);
        }
    }

    /**
     * 执行重建（由 RabbitMQ 消费者调用）：清空索引 -> 分批全量索引 -> 更新任务状态
     */
    public void executeRebuild(Long taskId) {
        EsRebuildTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("索引重建任务不存在: taskId={}", taskId);
            return;
        }
        if (task.getStatus() != null && task.getStatus() != 0) {
            // 幂等控制：仅处理待执行任务
            log.info("任务已处理过，跳过: taskNo={}, status={}", task.getTaskNo(), task.getStatus());
            return;
        }
        task.setStatus(1);
        task.setStartedAt(LocalDateTime.now());
        task.setMessage("正在重建索引...");
        taskRepository.save(task);

        int successCount = 0;
        try {
            long total = documentRepository.count();
            task.setTotalCount((int) total);
            taskRepository.save(task);

            searchService.clearIndex();

            int page = 0;
            while (true) {
                List<KbDocument> documents = documentRepository
                        .findAll(PageRequest.of(page, batchSize, Sort.by(Sort.Direction.ASC, "id")))
                        .getContent();
                if (documents.isEmpty()) {
                    break;
                }
                List<DocumentIndexVO> indexDocs = indexDocumentBuilder.buildBatch(documents);
                successCount += searchService.bulkIndex(indexDocs);
                if (documents.size() < batchSize) {
                    break;
                }
                page++;
            }
            searchService.refreshIndex();

            task.setStatus(2);
            task.setSuccessCount(successCount);
            task.setFailCount(Math.max(0, (int) total - successCount));
            task.setMessage("索引重建完成，共索引 " + successCount + " 个文档");
            log.info("索引重建完成: taskNo={}, total={}, success={}",
                    task.getTaskNo(), total, successCount);
        } catch (Exception e) {
            log.error("索引重建失败: taskNo={}", task.getTaskNo(), e);
            task.setStatus(3);
            task.setSuccessCount(successCount);
            task.setFailCount(Math.max(0, task.getTotalCount() - successCount));
            task.setMessage("索引重建失败: " + e.getMessage());
        } finally {
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }

    public PageResult<RebuildTaskVO> page(int pageNum, int pageSize) {
        List<EsRebuildTask> tasks = taskRepository.findTop10ByOrderByCreatedAtDesc();
        // 任务量小，内存分页即可
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(tasks.size(), from + pageSize);
        List<RebuildTaskVO> list = from >= tasks.size() ? List.of()
                : tasks.subList(from, to).stream().map(RebuildTaskVO::from).toList();
        return PageResult.of(list, tasks.size(), pageNum, pageSize);
    }

    public RebuildTaskVO detail(Long id) {
        EsRebuildTask task = taskRepository.findById(id)
                .orElseThrow(() -> new com.kb.system.common.BusinessException("任务不存在"));
        return RebuildTaskVO.from(task);
    }

    private String generateTaskNo() {
        return "REBUILD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
