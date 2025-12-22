package com.github.jredmine.dto.response;

import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * 分页响应类
 */
@Data
public class PageResponse<T> {

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 当前页码
     */
    private Integer current;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 总页数
     */
    private Integer pages;

    public PageResponse() {
    }

    public PageResponse(List<T> records, Integer total, Integer current, Integer size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = size > 0 ? (total + size - 1) / size : 0;
    }

    /**
     * 创建分页响应
     */
    public static <T> PageResponse<T> of(List<T> records, Integer total, Integer current, Integer size) {
        return new PageResponse<>(records, total, current, size);
    }

    /**
     * 创建分页响应（兼容 Long 类型参数，自动转换为 Integer）
     */
    public static <T> PageResponse<T> of(List<T> records, Long total, Long current, Long size) {
        return new PageResponse<>(records, total.intValue(), current.intValue(), size.intValue());
    }

    /**
     * 将分页记录转换为新类型，同时保留分页元数据
     */
    public <R> PageResponse<R> map(Function<T, R> mapper) {
        PageResponse<R> response = new PageResponse<>();
        response.setRecords(this.records == null ? null : this.records.stream().map(mapper).toList());
        response.setTotal(this.total);
        response.setCurrent(this.current);
        response.setSize(this.size);
        response.setPages(this.pages);
        return response;
    }
}

