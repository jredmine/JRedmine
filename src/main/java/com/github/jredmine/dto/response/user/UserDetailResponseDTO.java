package com.github.jredmine.dto.response.user;

import lombok.Data;

import java.util.Date;

/**
 * 用户详情响应DTO
 *
 * @author panfeng
 */
@Data
public class UserDetailResponseDTO {
    private Long id;
    private String login;
    private String firstname;
    private String lastname;
    private Boolean admin;
    private Integer status;
    private Date lastLoginOn;
    private String language;
    private Date createdOn;
    private Date updatedOn;
    private String mailNotification;
}

