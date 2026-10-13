package com.kb.system.module.kb.document.repository;

import com.kb.system.module.kb.document.entity.KbDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 文档 Repository（支持组合条件筛选分页 + 统计投影）
 */
public interface KbDocumentRepository extends JpaRepository<KbDocument, Long>,
        JpaSpecificationExecutor<KbDocument> {

    boolean existsByTitleAndKbId(String title, Long kbId);

    List<KbDocument> findByKbId(Long kbId);

    List<KbDocument> findByFileId(Long fileId);

    /** 按分类统计已发布文档数 */
    @Query("select d.categoryId as categoryId, count(d) as cnt from KbDocument d " +
           "where d.status = 1 group by d.categoryId")
    List<CategoryCountProjection> countPublishedByCategory();

    /** 按知识库统计已发布文档数 */
    @Query("select d.kbId as kbId, count(d) as cnt from KbDocument d " +
           "where d.status = 1 group by d.kbId")
    List<KbCountProjection> countPublishedByKb();

    /**
     * 按知识库统计全部文档数（含草稿）。
     * 知识库列表的"文档数"以此动态统计为准，避免 doc_count 冗余列漂移。
     */
    @Query("select d.kbId as kbId, count(d) as cnt from KbDocument d group by d.kbId")
    List<KbCountProjection> countAllByKb();

    /** 单个知识库的文档总数 */
    long countByKbId(Long kbId);

    interface CategoryCountProjection {
        Long getCategoryId();

        Long getCnt();
    }

    interface KbCountProjection {
        Long getKbId();

        Long getCnt();
    }
}
