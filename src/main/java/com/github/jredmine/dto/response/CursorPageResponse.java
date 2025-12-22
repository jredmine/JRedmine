package com.github.jredmine.dto.response;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 游标分页响应
 */
@Data
public class CursorPageResponse<T> {

    /**
     * 当前页数据
     */
    private List<T> records = Collections.emptyList();

    /**
     * 下一页游标（通常为当前页最后一条记录的唯一标识）
     */
    private Long nextCursor;

    /**
     * 是否还有下一页
     */
    private boolean hasNext;

    /**
     * 每页大小
     */
    private Integer size;

    public static <T> CursorPageResponse<T> of(List<T> records, Long nextCursor, boolean hasNext, Integer size) {
        CursorPageResponse<T> response = new CursorPageResponse<>();
        response.setRecords(records);
        response.setNextCursor(nextCursor);
        response.setHasNext(hasNext);
        response.setSize(size);
        return response;
    }

    /**
     * 将游标分页数据转换为新类型
     */
    public <R> CursorPageResponse<R> map(Function<T, R> mapper) {
        CursorPageResponse<R> response = new CursorPageResponse<>();
        response.setRecords(this.records == null ? Collections.emptyList() : this.records.stream().map(mapper).toList());
        response.setNextCursor(this.nextCursor);
        response.setHasNext(this.hasNext);
        response.setSize(this.size);
        return response;
    }
}

