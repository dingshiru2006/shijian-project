package com.kb.system.module.kb.tag.repository;

import com.kb.system.module.kb.tag.entity.KbTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 标签 Repository
 */
public interface KbTagRepository extends JpaRepository<KbTag, Long>,
        JpaSpecificationExecutor<KbTag> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<KbTag> findAllByOrderByIdAsc();
}
