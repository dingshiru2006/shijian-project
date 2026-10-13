package com.kb.system.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 分页查询基类，各模块查询 DTO 继承此类
 */
@Data
public class PageQuery {

    @Min(value = 1, message = "页码不能小于 1")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private int pageSize = 10;

    public PageRequest toPageable() {
        return PageRequest.of(pageNum - 1, pageSize);
    }

    public PageRequest toPageable(Sort sort) {
        return PageRequest.of(pageNum - 1, pageSize, sort);
    }
}
