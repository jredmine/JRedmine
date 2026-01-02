package com.github.jredmine.dto.request;

import com.github.jredmine.dto.request.validator.ValidPageParam;
import lombok.Data;

/**
 * 分页请求基础DTO
 * 所有分页查询的 RequestDTO 都应该继承此类
 * 
 * 注意：对于 GET 请求的查询参数，如果参数未传入，字段值为 null。
 * 自定义验证器会允许 null 值（使用默认值），但非 null 值必须 >= 1。
 *
 * @author panfeng
 */
@Data
public class PageRequestDTO {
    /**
     * 当前页码（从1开始）
     * 如果为 null，将使用默认值 1；如果不为 null，必须 >= 1
     */
    @ValidPageParam(message = "页码必须大于0")
    private Integer current = 1;

    /**
     * 每页数量
     * 如果为 null，将使用默认值 10；如果不为 null，必须 >= 1
     */
    @ValidPageParam(message = "每页数量必须大于0")
    private Integer size = 10;
}
