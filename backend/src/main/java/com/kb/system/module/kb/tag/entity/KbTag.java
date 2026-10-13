package com.kb.system.module.kb.tag.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识库标签表
 */
@Entity
@Table(name = "kb_tag")
@Getter
@Setter
public class KbTag extends BaseEntity {

    /** 标签名称（唯一） */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /** 标签颜色 */
    @Column(name = "color", length = 20)
    private String color;
}
