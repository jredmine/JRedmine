package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.setting.EmailTestRequestDTO;
import com.github.jredmine.dto.request.setting.SettingUpdateRequestDTO;
import com.github.jredmine.dto.response.setting.SettingGroupResponseDTO;
import com.github.jredmine.dto.response.setting.SettingResponseDTO;
import com.github.jredmine.entity.Setting;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.enums.SettingKey;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.SettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统设置服务
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingMapper settingMapper;
    private final JavaMailSender javaMailSender;

    // 内存缓存，避免频繁数据库查询
    private final Map<String, String> settingCache = new HashMap<>();

    /**
     * 获取所有系统设置（按分类分组）
     *
     * @return 分组的系统设置列表
     */
    public List<SettingGroupResponseDTO> getAllSettings() {
        MDC.put("operation", "get_all_settings");

        try {
            log.debug("开始查询所有系统设置");

            // 从数据库查询所有设置
            List<Setting> settings = settingMapper.selectList(null);
            Map<String, Setting> settingMap = settings.stream()
                    .collect(Collectors.toMap(Setting::getName, s -> s));

            // 按分类分组
            Map<SettingKey.SettingCategory, List<SettingResponseDTO>> groupedSettings = new HashMap<>();

            for (SettingKey settingKey : SettingKey.values()) {
                Setting setting = settingMap.get(settingKey.getKey());
                SettingResponseDTO dto = SettingResponseDTO.builder()
                        .id(setting != null ? setting.getId() : null)
                        .name(settingKey.getKey())
                        .value(setting != null ? setting.getValue() : settingKey.getDefaultValue())
                        .description(settingKey.getDescription())
                        .category(settingKey.getCategory().getCode())
                        .defaultValue(settingKey.getDefaultValue())
                        .updatedOn(setting != null ? setting.getUpdatedOn() : null)
                        .build();

                groupedSettings.computeIfAbsent(settingKey.getCategory(), k -> new ArrayList<>()).add(dto);
            }

            // 转换为分组响应
            List<SettingGroupResponseDTO> result = groupedSettings.entrySet().stream()
                    .map(entry -> SettingGroupResponseDTO.builder()
                            .category(entry.getKey().getCode())
                            .categoryName(entry.getKey().getName())
                            .settings(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            log.info("查询到系统设置分组数量: {}", result.size());
            return result;
        } catch (Exception e) {
            log.error("查询系统设置失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "查询系统设置失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 根据分类获取系统设置
     *
     * @param category 分类代码
     * @return 设置列表
     */
    public List<SettingResponseDTO> getSettingsByCategory(String category) {
        MDC.put("operation", "get_settings_by_category");
        MDC.put("category", category);

        try {
            log.debug("查询分类设置，分类: {}", category);

            SettingKey.SettingCategory settingCategory = null;
            for (SettingKey.SettingCategory cat : SettingKey.SettingCategory.values()) {
                if (cat.getCode().equals(category)) {
                    settingCategory = cat;
                    break;
                }
            }

            if (settingCategory == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "无效的设置分类");
            }

            // 查询该分类的所有设置
            List<Setting> settings = settingMapper.selectList(null);
            Map<String, Setting> settingMap = settings.stream()
                    .collect(Collectors.toMap(Setting::getName, s -> s));

            List<SettingResponseDTO> result = new ArrayList<>();
            for (SettingKey settingKey : SettingKey.values()) {
                if (settingKey.getCategory() == settingCategory) {
                    Setting setting = settingMap.get(settingKey.getKey());
                    SettingResponseDTO dto = SettingResponseDTO.builder()
                            .id(setting != null ? setting.getId() : null)
                            .name(settingKey.getKey())
                            .value(setting != null ? setting.getValue() : settingKey.getDefaultValue())
                            .description(settingKey.getDescription())
                            .category(settingKey.getCategory().getCode())
                            .defaultValue(settingKey.getDefaultValue())
                            .updatedOn(setting != null ? setting.getUpdatedOn() : null)
                            .build();
                    result.add(dto);
                }
            }

            log.info("查询到设置数量: {}", result.size());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询分类设置失败，分类: {}", category, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "查询分类设置失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 获取单个系统设置
     *
     * @param name 设置项名称
     * @return 设置项值
     */
    public String getSetting(String name) {
        // 先从缓存获取
        if (settingCache.containsKey(name)) {
            return settingCache.get(name);
        }

        // 从数据库查询
        Setting setting = settingMapper.selectOne(
                new LambdaQueryWrapper<Setting>().eq(Setting::getName, name));

        String value;
        if (setting != null) {
            value = setting.getValue();
        } else {
            // 如果数据库没有，返回默认值
            SettingKey settingKey = SettingKey.fromKey(name);
            value = settingKey != null ? settingKey.getDefaultValue() : null;
        }

        // 放入缓存
        if (value != null) {
            settingCache.put(name, value);
        }

        return value;
    }

    /**
     * 更新系统设置
     *
     * @param requestDTO 更新请求
     * @return 更新后的设置
     */
    @Transactional(rollbackFor = Exception.class)
    public SettingResponseDTO updateSetting(SettingUpdateRequestDTO requestDTO) {
        MDC.put("operation", "update_setting");
        MDC.put("name", requestDTO.getName());

        try {
            log.info("开始更新系统设置，名称: {}", requestDTO.getName());

            // 验证设置项是否存在
            SettingKey settingKey = SettingKey.fromKey(requestDTO.getName());
            if (settingKey == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "未知的设置项: " + requestDTO.getName());
            }

            // 查询是否已存在
            Setting setting = settingMapper.selectOne(
                    new LambdaQueryWrapper<Setting>().eq(Setting::getName, requestDTO.getName()));

            if (setting == null) {
                // 新增
                setting = new Setting();
                setting.setName(requestDTO.getName());
                setting.setValue(requestDTO.getValue());
                setting.setUpdatedOn(LocalDateTime.now());
                settingMapper.insert(setting);
                log.info("新增系统设置成功，名称: {}", requestDTO.getName());
            } else {
                // 更新
                setting.setValue(requestDTO.getValue());
                setting.setUpdatedOn(LocalDateTime.now());
                settingMapper.updateById(setting);
                log.info("更新系统设置成功，名称: {}", requestDTO.getName());
            }

            // 更新缓存
            settingCache.put(requestDTO.getName(), requestDTO.getValue());

            // 构建响应
            return SettingResponseDTO.builder()
                    .id(setting.getId())
                    .name(setting.getName())
                    .value(setting.getValue())
                    .description(settingKey.getDescription())
                    .category(settingKey.getCategory().getCode())
                    .defaultValue(settingKey.getDefaultValue())
                    .updatedOn(setting.getUpdatedOn())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新系统设置失败，名称: {}", requestDTO.getName(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "更新系统设置失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 重置设置为默认值
     *
     * @param name 设置项名称
     * @return 重置后的设置
     */
    @Transactional(rollbackFor = Exception.class)
    public SettingResponseDTO resetSetting(String name) {
        MDC.put("operation", "reset_setting");
        MDC.put("name", name);

        try {
            log.info("开始重置系统设置，名称: {}", name);

            // 验证设置项是否存在
            SettingKey settingKey = SettingKey.fromKey(name);
            if (settingKey == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "未知的设置项: " + name);
            }

            // 删除数据库记录（将使用默认值）
            settingMapper.delete(new LambdaQueryWrapper<Setting>().eq(Setting::getName, name));

            // 清除缓存
            settingCache.remove(name);

            log.info("重置系统设置成功，名称: {}", name);

            // 返回默认值
            return SettingResponseDTO.builder()
                    .name(name)
                    .value(settingKey.getDefaultValue())
                    .description(settingKey.getDescription())
                    .category(settingKey.getCategory().getCode())
                    .defaultValue(settingKey.getDefaultValue())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("重置系统设置失败，名称: {}", name, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "重置系统设置失败");
        } finally {
            MDC.clear();
        }
    }

    /**
     * 测试邮件配置
     *
     * @param requestDTO 测试请求
     */
    public void testEmailConfiguration(EmailTestRequestDTO requestDTO) {
        MDC.put("operation", "test_email");
        MDC.put("toEmail", requestDTO.getToEmail());

        try {
            log.info("开始测试邮件配置，收件人: {}", requestDTO.getToEmail());

            // 检查邮件是否启用
            String emailEnabled = getSetting(SettingKey.EMAIL_ENABLED.getKey());
            if (!"true".equalsIgnoreCase(emailEnabled)) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "邮件发送未启用");
            }

            // 发送测试邮件
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getSetting(SettingKey.EMAIL_FROM.getKey()));
            message.setTo(requestDTO.getToEmail());
            message.setSubject(requestDTO.getSubject());
            message.setText(requestDTO.getContent());

            javaMailSender.send(message);

            log.info("测试邮件发送成功，收件人: {}", requestDTO.getToEmail());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("测试邮件发送失败，收件人: {}", requestDTO.getToEmail(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "邮件发送失败: " + e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    /**
     * 清除设置缓存
     */
    public void clearCache() {
        settingCache.clear();
        log.info("系统设置缓存已清除");
    }
}
