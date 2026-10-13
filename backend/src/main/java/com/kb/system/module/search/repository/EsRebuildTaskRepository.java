package com.kb.system.module.search.repository;

import com.kb.system.module.search.entity.EsRebuildTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 索引重建任务 Repository
 */
public interface EsRebuildTaskRepository extends JpaRepository<EsRebuildTask, Long> {

    Optional<EsRebuildTask> findByTaskNo(String taskNo);

    List<EsRebuildTask> findTop10ByOrderByCreatedAtDesc();

    long countByStatus(Integer status);
}
