package com.github.jredmine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.reset-password.url:http://localhost:8088/reset-password}")
    private String resetPasswordUrl;

    /**
     * 发送密码重置邮件
     *
     * @param toEmail 收件人邮箱
     * @param username 用户名
     * @param resetToken 重置Token
     */
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("密码重置请求");
            
            String resetLink = resetPasswordUrl + "?token=" + resetToken;
            String emailContent = String.format(
                    "您好 %s，\n\n" +
                    "您请求重置密码。请点击以下链接重置您的密码：\n\n" +
                    "%s\n\n" +
                    "此链接将在1小时后失效。\n\n" +
                    "如果您没有请求重置密码，请忽略此邮件。\n\n" +
                    "此邮件由系统自动发送，请勿回复。",
                    username, resetLink
            );
            
            message.setText(emailContent);
            
            mailSender.send(message);
            log.info("密码重置邮件发送成功，收件人: {}", toEmail);
        } catch (Exception e) {
            log.error("密码重置邮件发送失败，收件人: {}", toEmail, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}

