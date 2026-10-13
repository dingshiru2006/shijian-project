package com.kb.system.module.kb.file.vo;

import com.kb.system.module.kb.file.entity.KbFile;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件文件视图对象
 */
@Data
@AllArgsConstructor
public class FileVO {

    private Long id;
    private String originalName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime createdAt;

    public static FileVO from(KbFile file) {
        return new FileVO(file.getId(), file.getOriginalName(), file.getFileSize(),
                file.getFileType(), file.getCreatedAt());
    }
}
