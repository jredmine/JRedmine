package com.github.jredmine.dto.request.user;

import com.github.jredmine.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户状态更新请求DTO
 *
 * @author panfeng
 */
@Data
public class UserStatusUpdateRequestDTO {

    /**
     * 用户状态
     * 1=启用({@link UserStatus#ACTIVE}), 2=锁定({@link UserStatus#LOCKED}), 3=待激活({@link UserStatus#PENDING})
     */
    @NotNull(message = "用户状态是必填项")
    private Integer status;
}

