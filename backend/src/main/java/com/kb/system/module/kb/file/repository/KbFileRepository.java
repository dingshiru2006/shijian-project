package com.kb.system.module.kb.file.repository;

import com.kb.system.module.kb.file.entity.KbFile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 文件元数据 Repository
 */
public interface KbFileRepository extends JpaRepository<KbFile, Long> {
}
