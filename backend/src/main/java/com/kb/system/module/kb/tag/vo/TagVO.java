package com.kb.system.module.kb.tag.vo;

import com.kb.system.module.kb.tag.entity.KbTag;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签视图对象
 */
@Data
@AllArgsConstructor
public class TagVO {

    private Long id;
    private String name;
    private String color;
    private LocalDateTime createdAt;

    public static TagVO from(KbTag tag) {
        return new TagVO(tag.getId(), tag.getName(), tag.getColor(), tag.getCreatedAt());
    }
}
