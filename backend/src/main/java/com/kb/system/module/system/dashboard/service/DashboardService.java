package com.kb.system.module.system.dashboard.service;

import com.kb.system.common.PageResult;
import com.kb.system.module.kb.category.entity.KbCategory;
import com.kb.system.module.kb.category.repository.KbCategoryRepository;
import com.kb.system.module.kb.document.repository.KbDocumentRepository;
import com.kb.system.module.kb.knowledgebase.entity.KbKnowledgeBase;
import com.kb.system.module.kb.knowledgebase.repository.KbKnowledgeBaseRepository;
import com.kb.system.module.kb.tag.repository.KbTagRepository;
import com.kb.system.module.search.service.DocumentSearchService;
import com.kb.system.module.system.dashboard.vo.DashboardVO;
import com.kb.system.module.system.log.dto.LogQueryDTO;
import com.kb.system.module.system.log.service.SysOperationLogService;
import com.kb.system.module.system.log.vo.OperationLogVO;
import com.kb.system.module.system.user.repository.SysUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计服务
 */
@Service
public class DashboardService {

    private final KbKnowledgeBaseRepository kbRepository;
    private final KbDocumentRepository documentRepository;
    private final KbCategoryRepository categoryRepository;
    private final KbTagRepository tagRepository;
    private final SysUserRepository userRepository;
    private final DocumentSearchService searchService;
    private final SysOperationLogService operationLogService;

    public DashboardService(KbKnowledgeBaseRepository kbRepository,
                            KbDocumentRepository documentRepository,
                            KbCategoryRepository categoryRepository,
                            KbTagRepository tagRepository,
                            SysUserRepository userRepository,
                            DocumentSearchService searchService,
                            SysOperationLogService operationLogService) {
        this.kbRepository = kbRepository;
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.searchService = searchService;
        this.operationLogService = operationLogService;
    }

    public DashboardVO stats() {
        long documentCount = documentRepository.count();
        long publishedCount = documentRepository.findAll().stream()
                .filter(doc -> doc.getStatus() != null && doc.getStatus() == 1).count();
        long esDocCount = searchService.countDocuments();

        // 今日操作日志数
        LogQueryDTO logQuery = new LogQueryDTO();
        logQuery.setPageNum(1);
        logQuery.setPageSize(1);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        logQuery.setStartTime(startOfDay.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        PageResult<OperationLogVO> todayLogs = operationLogService.page(logQuery);

        List<DashboardVO.ChartItem> categoryChart = buildCategoryChart();
        List<DashboardVO.ChartItem> kbChart = buildKbChart();

        // 最近 8 条操作日志
        LogQueryDTO recentQuery = new LogQueryDTO();
        recentQuery.setPageNum(1);
        recentQuery.setPageSize(8);
        List<OperationLogVO> recentLogs = operationLogService.page(recentQuery).getList();

        return new DashboardVO(
                kbRepository.count(),
                documentCount,
                publishedCount,
                documentCount - publishedCount,
                userRepository.count(),
                categoryRepository.count(),
                tagRepository.count(),
                esDocCount,
                todayLogs.getTotal(),
                categoryChart,
                kbChart,
                recentLogs);
    }

    private List<DashboardVO.ChartItem> buildCategoryChart() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        documentRepository.countPublishedByCategory().forEach(projection ->
                counts.put(projection.getCategoryId(), projection.getCnt()));
        List<DashboardVO.ChartItem> items = new ArrayList<>();
        for (KbCategory category : categoryRepository.findAllByOrderBySortAsc()) {
            Long count = counts.getOrDefault(category.getId(), 0L);
            if (count > 0) {
                items.add(new DashboardVO.ChartItem(category.getName(), count));
            }
        }
        return items;
    }

    private List<DashboardVO.ChartItem> buildKbChart() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        documentRepository.countPublishedByKb().forEach(projection ->
                counts.put(projection.getKbId(), projection.getCnt()));
        List<DashboardVO.ChartItem> items = new ArrayList<>();
        for (KbKnowledgeBase kb : kbRepository.findAll()) {
            Long count = counts.getOrDefault(kb.getId(), 0L);
            if (count > 0) {
                items.add(new DashboardVO.ChartItem(kb.getName(), count));
            }
        }
        return items;
    }
}
