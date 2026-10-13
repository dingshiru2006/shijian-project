package com.kb.system.module.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.kb.system.common.PageResult;
import com.kb.system.module.search.dto.SearchQueryDTO;
import com.kb.system.module.search.vo.DocumentIndexVO;
import com.kb.system.module.search.vo.SearchHitVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 检索服务：
 * 索引初始化 / 单条索引 / 批量索引 / 删除 / 清空 / 关键词+组合筛选+高亮搜索
 */
@Service
public class DocumentSearchService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final co.elastic.clients.elasticsearch.ElasticsearchClient esClient;

    @Value("${app.es.index-name}")
    private String indexName;

    @Value("${app.es.rebuild-batch-size}")
    private int batchSize;

    public DocumentSearchService(co.elastic.clients.elasticsearch.ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * 启动时初始化索引（存在则跳过）
     */
    public void initIndex() {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return;
            }
            esClient.indices().create(c -> c.index(indexName).mappings(m -> m
                    .properties("id", p -> p.long_(l -> l))
                    .properties("title", p -> p.text(t -> t
                            .fields("keyword", f -> f.keyword(k -> k))))
                    .properties("summary", p -> p.text(t -> t))
                    .properties("content", p -> p.text(t -> t))
                    .properties("kbId", p -> p.long_(l -> l))
                    .properties("kbName", p -> p.keyword(k -> k))
                    .properties("categoryId", p -> p.long_(l -> l))
                    .properties("categoryName", p -> p.keyword(k -> k))
                    .properties("tagNames", p -> p.keyword(k -> k))
                    .properties("authorId", p -> p.long_(l -> l))
                    .properties("authorName", p -> p.keyword(k -> k))
                    .properties("status", p -> p.integer(i -> i))
                    .properties("createdAt", p -> p.date(d -> d.format("epoch_millis")))));
            log.info("ES 索引创建成功: {}", indexName);
        } catch (IOException e) {
            log.error("ES 索引初始化失败", e);
            throw new RuntimeException("ES 索引初始化失败: " + e.getMessage());
        }
    }

    /**
     * 单条索引（文档新增/修改时实时同步）
     */
    public void indexDocument(DocumentIndexVO document) {
        try {
            esClient.index(i -> i.index(indexName)
                    .id(String.valueOf(document.getId()))
                    .document(document));
        } catch (IOException e) {
            log.error("ES 索引文档失败: id={}", document.getId(), e);
        }
    }

    /**
     * 批量索引（索引重建使用），返回成功条数
     */
    public int bulkIndex(List<DocumentIndexVO> documents) {
        if (documents.isEmpty()) {
            return 0;
        }
        try {
            co.elastic.clients.elasticsearch.core.BulkResponse response = esClient.bulk(b -> {
                for (DocumentIndexVO document : documents) {
                    b.operations(op -> op.index(i -> i.index(indexName)
                            .id(String.valueOf(document.getId()))
                            .document(document)));
                }
                return b;
            });
            long errors = response.items().stream()
                    .filter(item -> item.error() != null).count();
            if (errors > 0) {
                log.warn("ES 批量索引存在失败条目: {}", errors);
            }
            return (int) (documents.size() - errors);
        } catch (IOException e) {
            log.error("ES 批量索引失败", e);
            return 0;
        }
    }

    /**
     * 删除单条索引
     */
    public void deleteDocument(Long id) {
        try {
            esClient.delete(d -> d.index(indexName).id(String.valueOf(id)));
        } catch (IOException e) {
            log.error("ES 删除索引文档失败: id={}", id, e);
        }
    }

    /**
     * 清空索引数据（重建前置）
     */
    public void clearIndex() {
        try {
            esClient.deleteByQuery(d -> d.index(indexName)
                    .query(q -> q.matchAll(m -> m)));
        } catch (IOException e) {
            log.error("ES 清空索引失败", e);
            throw new RuntimeException("ES 清空索引失败: " + e.getMessage());
        }
    }

    /**
     * 刷新索引（重建后立即生效）
     */
    public void refreshIndex() {
        try {
            esClient.indices().refresh(r -> r.index(indexName));
        } catch (IOException e) {
            log.error("ES 刷新索引失败", e);
        }
    }

    /**
     * 索引文档总数
     */
    public long countDocuments() {
        try {
            return esClient.count(c -> c.index(indexName)).count();
        } catch (IOException e) {
            log.error("ES 文档计数失败", e);
            return 0L;
        }
    }

    /**
     * 全文检索：关键词（标题^3、摘要^2、正文 权重匹配）+ 组合条件筛选 + 高亮 + 分页
     */
    public PageResult<SearchHitVO> search(SearchQueryDTO query) {
        Query finalQuery = buildQuery(query);
        int from = (query.getPageNum() - 1) * query.getPageSize();
        SearchResponse<DocumentIndexVO> response;
        try {
            response = esClient.search(s -> s
                    .index(indexName)
                    .query(finalQuery)
                    .highlight(h -> h
                            .fields("title", f -> f
                                    .preTags(List.of("<em>"))
                                    .postTags(List.of("</em>"))
                                    .numberOfFragments(0))
                            .fields("summary", f -> f
                                    .preTags(List.of("<em>"))
                                    .postTags(List.of("</em>"))
                                    .fragmentSize(200)
                                    .numberOfFragments(3))
                            .fields("content", f -> f
                                    .preTags(List.of("<em>"))
                                    .postTags(List.of("</em>"))
                                    .fragmentSize(200)
                                    .numberOfFragments(3)))
                    .from(from)
                    .size(query.getPageSize())
                    .sort(so -> so.score(sc -> sc.order(SortOrder.Desc))),
                    DocumentIndexVO.class);
        } catch (IOException e) {
            log.error("ES 检索失败", e);
            throw new RuntimeException("检索失败: " + e.getMessage());
        }

        List<SearchHitVO> hits = new ArrayList<>();
        for (Hit<DocumentIndexVO> hit : response.hits().hits()) {
            DocumentIndexVO doc = hit.source();
            if (doc == null) {
                continue;
            }
            Map<String, List<String>> highlight = hit.highlight();
            String hlTitle = firstFragment(highlight, "title");
            String hlSummary = firstFragment(highlight, "summary");
            String hlContent = firstFragment(highlight, "content");
            hits.add(new SearchHitVO(doc.getId(), doc.getTitle(), doc.getSummary(), doc.getContent(),
                    doc.getKbId(), doc.getKbName(), doc.getCategoryId(), doc.getCategoryName(),
                    doc.getTagNames(), doc.getAuthorName(), doc.getStatus(),
                    formatMillis(doc.getCreatedAt()), hit.score(),
                    hlTitle != null ? hlTitle : doc.getTitle(),
                    hlSummary != null ? hlSummary : doc.getSummary(),
                    hlContent != null ? hlContent : truncate(doc.getContent(), 300)));
        }
        long total = response.hits().total() == null ? 0L : response.hits().total().value();
        return PageResult.of(hits, total, query.getPageNum(), query.getPageSize());
    }

    // ---------------- 内部方法 ----------------

    private Query buildQuery(SearchQueryDTO query) {
        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        if (StringUtils.hasText(query.getKeyword())) {
            must.add(Query.of(q -> q.multiMatch(mm -> mm
                    .query(query.getKeyword())
                    .fields("title^3", "summary^2", "content"))));
        }
        if (query.getKbId() != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("kbId").value(query.getKbId()))));
        }
        if (query.getCategoryId() != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("categoryId").value(query.getCategoryId()))));
        }
        if (StringUtils.hasText(query.getTagName())) {
            filter.add(Query.of(q -> q.term(t -> t.field("tagNames").value(query.getTagName()))));
        }
        if (query.getStatus() != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("status").value(query.getStatus()))));
        }
        if (StringUtils.hasText(query.getStartDate())) {
            filter.add(Query.of(q -> q.range(r -> r
                    .field("createdAt")
                    .gte(co.elastic.clients.json.JsonData.of(
                            toMillis(query.getStartDate(), false))))));
        }
        if (StringUtils.hasText(query.getEndDate())) {
            filter.add(Query.of(q -> q.range(r -> r
                    .field("createdAt")
                    .lte(co.elastic.clients.json.JsonData.of(
                            toMillis(query.getEndDate(), true))))));
        }

        return Query.of(q -> q.bool(b -> {
            if (!must.isEmpty()) {
                b.must(must);
            }
            if (!filter.isEmpty()) {
                b.filter(filter);
            }
            return b;
        }));
    }

    private Long toMillis(String date, boolean endOfDay) {
        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime dateTime = endOfDay ? localDate.atTime(23, 59, 59) : localDate.atStartOfDay();
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String firstFragment(Map<String, List<String>> highlight, String field) {
        if (highlight == null) {
            return null;
        }
        List<String> fragments = highlight.get(field);
        if (fragments == null || fragments.isEmpty()) {
            return null;
        }
        return String.join(" … ", fragments);
    }

    private String formatMillis(Long millis) {
        if (millis == null) {
            return null;
        }
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(DATE_FORMATTER);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
