package com.github.jredmine.dto.response.user;

import lombok.Data;

import java.util.Date;

/**
 * 用户列表项响应DTO
 * 用于用户列表接口，包含列表展示所需的基本信息
 *
 * @author panfeng
 */
@Data
public class UserListItemResponseDTO {
    private Long id;
    private String login;
    private String firstname;
    private String lastname;
    private Boolean admin;
    private Integer status;
    private Date createdOn;
}

