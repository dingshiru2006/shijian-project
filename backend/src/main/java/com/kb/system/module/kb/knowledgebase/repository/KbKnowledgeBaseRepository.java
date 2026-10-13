package com.kb.system.module.kb.knowledgebase.repository;

import com.kb.system.module.kb.knowledgebase.entity.KbKnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 知识库 Repository
 */
public interface KbKnowledgeBaseRepository extends JpaRepository<KbKnowledgeBase, Long>,
        JpaSpecificationExecutor<KbKnowledgeBase> {

    Optional<KbKnowledgeBase> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
