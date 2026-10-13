package com.kb.system.module.search.controller;

import com.kb.system.common.PageResult;
import com.kb.system.common.Result;
import com.kb.system.module.search.dto.SearchQueryDTO;
import com.kb.system.module.search.service.DocumentSearchService;
import com.kb.system.module.search.vo.SearchHitVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全文检索控制器：关键词 + 组合筛选 + 高亮
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    private final DocumentSearchService searchService;

    public SearchController(DocumentSearchService searchService) {
        this.searchService = searchService;
    }

    @PreAuthorize("hasAuthority('kb:search')")
    @GetMapping("/documents")
    public Result<PageResult<SearchHitVO>> search(@Valid SearchQueryDTO query) {
        return Result.ok(searchService.search(query));
    }
}
