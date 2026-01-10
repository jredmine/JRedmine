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
     * @param toEmail    收件人邮箱
     * @param username   用户名
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
                    """
                            您好 %s，
                            
                            您请求重置密码。请点击以下链接重置您的密码：
                            
                            %s
                            
                            此链接将在1小时后失效。
                            
                            如果您没有请求重置密码，请忽略此邮件。
                            
                            此邮件由系统自动发送，请勿回复。""",
                    username, resetLink);

            message.setText(emailContent);

            mailSender.send(message);
            log.info("密码重置邮件发送成功，收件人: {}", toEmail);
        } catch (Exception e) {
            log.error("密码重置邮件发送失败，收件人: {}", toEmail, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 发送任务分配通知邮件
     *
     * @param toEmail      收件人邮箱
     * @param assigneeName 指派人姓名
     * @param issueId      任务ID
     * @param issueSubject 任务标题
     * @param projectName  项目名称
     * @param assignerName 分配人姓名（可选）
     */
    public void sendIssueAssignmentEmail(String toEmail, String assigneeName, Long issueId,
            String issueSubject, String projectName, String assignerName) {
        try {
            // 检查邮件配置是否有效
            if (fromEmail == null || fromEmail.trim().isEmpty()) {
                log.warn("邮件配置未设置（spring.mail.username），跳过邮件发送，任务ID: {}", issueId);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(String.format("任务分配通知 - %s", issueSubject));

            String emailContent = String.format(
                    """
                            您好 %s，
                            
                            您已被分配了一个新任务：
                            
                            任务ID: #%d
                            任务标题: %s
                            所属项目: %s
                            """,
                    assigneeName, issueId, issueSubject, projectName);

            if (assignerName != null && !assignerName.trim().isEmpty()) {
                emailContent += String.format("分配人: %s\n", assignerName);
            }

            emailContent += """
                    
                    请及时查看并处理该任务。
                    
                    此邮件由系统自动发送，请勿回复。""";

            message.setText(emailContent);

            mailSender.send(message);
            log.info("任务分配通知邮件发送成功，收件人: {}, 任务ID: {}", toEmail, issueId);
        } catch (org.springframework.mail.MailException e) {
            // 邮件发送失败不应该影响任务分配流程，只记录错误日志
            log.error("任务分配通知邮件发送失败，收件人: {}, 任务ID: {}。错误: {}。请检查邮件服务器配置（spring.mail.host, spring.mail.port等）",
                    toEmail, issueId, e.getMessage(), e);
        } catch (Exception e) {
            // 其他异常
            log.error("任务分配通知邮件发送失败，收件人: {}, 任务ID: {}", toEmail, issueId, e);
        }
    }

    /**
     * 发送任务变更通知邮件
     *
     * @param toEmail      收件人邮箱
     * @param recipientName 收件人姓名
     * @param issueId      任务ID
     * @param issueSubject 任务标题
     * @param projectName  项目名称
     * @param updaterName  更新人姓名
     * @param changesSummary 变更摘要
     * @param notes        备注信息（可选）
     */
    public void sendIssueUpdateEmail(String toEmail, String recipientName, Long issueId,
            String issueSubject, String projectName, String updaterName, 
            String changesSummary, String notes) {
        try {
            // 检查邮件配置是否有效
            if (fromEmail == null || fromEmail.trim().isEmpty()) {
                log.warn("邮件配置未设置（spring.mail.username），跳过邮件发送，任务ID: {}", issueId);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(String.format("任务更新通知 - %s", issueSubject));

            StringBuilder emailContent = new StringBuilder();
            emailContent.append(String.format(
                    """
                            您好 %s，
                            
                            任务已更新：
                            
                            任务ID: #%d
                            任务标题: %s
                            所属项目: %s
                            更新人: %s
                            """,
                    recipientName, issueId, issueSubject, projectName, updaterName));

            // 添加变更摘要
            if (changesSummary != null && !changesSummary.trim().isEmpty()) {
                emailContent.append("\n变更内容:\n");
                emailContent.append(changesSummary);
                emailContent.append("\n");
            }

            // 添加备注信息
            if (notes != null && !notes.trim().isEmpty()) {
                emailContent.append("\n备注: ");
                emailContent.append(notes);
                emailContent.append("\n");
            }

            emailContent.append("""
                    
                    请查看任务详情。
                    
                    此邮件由系统自动发送，请勿回复。""");

            message.setText(emailContent.toString());

            mailSender.send(message);
            log.info("任务更新通知邮件发送成功，收件人: {}, 任务ID: {}", toEmail, issueId);
        } catch (org.springframework.mail.MailException e) {
            // 邮件发送失败不应该影响任务更新流程，只记录错误日志
            log.error("任务更新通知邮件发送失败，收件人: {}, 任务ID: {}。错误: {}。请检查邮件服务器配置（spring.mail.host, spring.mail.port等）",
                    toEmail, issueId, e.getMessage(), e);
        } catch (Exception e) {
            // 其他异常
            log.error("任务更新通知邮件发送失败，收件人: {}, 任务ID: {}", toEmail, issueId, e);
        }
    }
}
