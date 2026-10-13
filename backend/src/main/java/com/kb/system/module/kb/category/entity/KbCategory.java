package com.kb.system.module.kb.category.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识库分类表
 */
@Entity
@Table(name = "kb_category")
@Getter
@Setter
public class KbCategory extends BaseEntity {

    /** 分类名称（唯一） */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /** 分类描述 */
    @Column(name = "description", length = 255)
    private String description;

    /** 显示排序（升序） */
    @Column(name = "sort", nullable = false)
    private Integer sort;

    /** 状态：1-启用，0-禁用 */
    @Column(name = "status", nullable = false)
    private Integer status;
}
