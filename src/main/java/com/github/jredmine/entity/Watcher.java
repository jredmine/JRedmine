package com.github.jredmine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 关注者实体类
 * 对应数据库中的 `watchers` 表
 *
 * @author panfeng
 */
@Data
@TableName("watchers")
public class Watcher {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 可关注类型（'Issue'、'Document'等）
     */
    private String watchableType;

    /**
     * 可关注对象ID
     */
    private Integer watchableId;

    /**
     * 用户ID
     */
    private Integer userId;
}
