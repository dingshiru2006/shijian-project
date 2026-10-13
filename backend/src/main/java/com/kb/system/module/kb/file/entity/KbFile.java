package com.kb.system.module.kb.file.entity;

import com.kb.system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 附件文件元数据表
 */
@Entity
@Table(name = "kb_file")
@Getter
@Setter
public class KbFile extends BaseEntity {

    /** 原始文件名 */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** 存储文件名（UUID） */
    @Column(name = "stored_name", nullable = false, length = 100)
    private String storedName;

    /** 存储路径（相对上传目录） */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /** 文件大小（字节） */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** 文件类型（MIME） */
    @Column(name = "file_type", length = 100)
    private String fileType;

    /** 上传人 ID */
    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;
}
