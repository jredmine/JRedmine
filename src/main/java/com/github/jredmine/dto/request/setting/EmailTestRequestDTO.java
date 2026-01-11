package com.github.jredmine.dto.request.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邮件配置测试请求DTO
 *
 * @author panfeng
 */
@Data
@Schema(description = "邮件配置测试请求")
public class EmailTestRequestDTO {
    
    @NotBlank(message = "收件人邮箱不能为空")
    @Email(message = "收件人邮箱格式不正确")
    @Schema(description = "测试收件人邮箱", example = "test@example.com")
    private String toEmail;
    
    @Schema(description = "测试邮件主题", example = "邮件配置测试")
    private String subject = "JRedmine 邮件配置测试";
    
    @Schema(description = "测试邮件内容", example = "这是一封测试邮件")
    private String content = "如果您收到这封邮件，说明邮件配置正确。";
}
