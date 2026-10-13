package com.kb.system.module.kb.category.repository;

import com.kb.system.module.kb.category.entity.KbCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 分类 Repository
 */
public interface KbCategoryRepository extends JpaRepository<KbCategory, Long>,
        JpaSpecificationExecutor<KbCategory> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<KbCategory> findAllByOrderBySortAsc();
}
