package com.kb.system.module.kb.knowledgebase.vo;

import com.kb.system.module.kb.knowledgebase.entity.KbKnowledgeBase;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库视图对象
 */
@Data
@AllArgsConstructor
public class KbVO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Long ownerId;
    private String ownerName;
    private Integer visibility;
    private Integer docCount;
    private Integer status;
    private LocalDateTime createdAt;

    public static KbVO from(KbKnowledgeBase kb, String ownerName) {
        return new KbVO(kb.getId(), kb.getName(), kb.getCode(), kb.getDescription(),
                kb.getOwnerId(), ownerName, kb.getVisibility(), kb.getDocCount(),
                kb.getStatus(), kb.getCreatedAt());
    }
}
