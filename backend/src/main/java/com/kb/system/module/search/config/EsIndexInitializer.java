package com.kb.system.module.search.config;

import com.kb.system.module.search.service.DocumentSearchService;
import com.kb.system.module.search.service.IndexRebuildService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后初始化 ES 索引：
 * 1. 索引不存在则创建；
 * 2. 索引为空但数据库有文档时，自动执行一次全量索引（开箱即可检索）。
 */
@Component
public class EsIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EsIndexInitializer.class);

    private final DocumentSearchService documentSearchService;
    private final IndexRebuildService indexRebuildService;

    public EsIndexInitializer(DocumentSearchService documentSearchService,
                              IndexRebuildService indexRebuildService) {
        this.documentSearchService = documentSearchService;
        this.indexRebuildService = indexRebuildService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            documentSearchService.initIndex();
        } catch (Exception e) {
            log.error("ES 索引初始化失败（不影响启动，检索时再报错）", e);
        }
        try {
            indexRebuildService.ensureIndexedOnStartup();
        } catch (Exception e) {
            log.warn("ES 启动自动索引失败（不影响启动，可手动触发重建）", e);
        }
    }
}
