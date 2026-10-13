package com.kb.system.module.kb.category.vo;

import com.kb.system.module.kb.category.entity.KbCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类视图对象
 */
@Data
@AllArgsConstructor
public class CategoryVO {

    private Long id;
    private String name;
    private String description;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;

    public static CategoryVO from(KbCategory category) {
        return new CategoryVO(category.getId(), category.getName(), category.getDescription(),
                category.getSort(), category.getStatus(), category.getCreatedAt());
    }
}
