package com.kb.system.module.kb.document.vo;

import com.kb.system.module.kb.document.entity.KbDocument;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档视图对象
 */
@Data
@AllArgsConstructor
public class DocumentVO {

    private Long id;
    private Long kbId;
    private String kbName;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    /** 正文内容（列表不返回，详情/编辑返回） */
    private String content;
    private Long fileId;
    private String fileName;
    private Long authorId;
    private String authorName;
    private Integer viewCount;
    private Integer status;
    /** 标签 ID 列表 */
    private List<Long> tagIds;
    /** 标签名称列表 */
    private List<String> tagNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 列表场景：不返回正文
     */
    public static DocumentVO fromList(KbDocument doc, String kbName, String categoryName,
                                      String authorName, String fileName,
                                      List<Long> tagIds, List<String> tagNames) {
        return new DocumentVO(doc.getId(), doc.getKbId(), kbName, doc.getCategoryId(),
                categoryName, doc.getTitle(), doc.getSummary(), null, doc.getFileId(), fileName,
                doc.getAuthorId(), authorName, doc.getViewCount(), doc.getStatus(),
                tagIds, tagNames, doc.getCreatedAt(), doc.getUpdatedAt());
    }

    /**
     * 详情场景：返回正文
     */
    public static DocumentVO fromDetail(KbDocument doc, String kbName, String categoryName,
                                        String authorName, String fileName,
                                        List<Long> tagIds, List<String> tagNames) {
        return new DocumentVO(doc.getId(), doc.getKbId(), kbName, doc.getCategoryId(),
                categoryName, doc.getTitle(), doc.getSummary(), doc.getContent(), doc.getFileId(),
                fileName, doc.getAuthorId(), authorName, doc.getViewCount(), doc.getStatus(),
                tagIds, tagNames, doc.getCreatedAt(), doc.getUpdatedAt());
    }
}
