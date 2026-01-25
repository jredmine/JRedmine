package com.github.jredmine.service;

import com.github.jredmine.dto.request.timeentry.TimeEntryBatchImportRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryCreateRequestDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryBatchImportResponseDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.entity.Enumeration;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.User;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.mapper.workflow.EnumerationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 工时记录批量导入服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeEntryImportService {
    
    private final TimeEntryService timeEntryService;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final EnumerationMapper enumerationMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 从Excel文件批量导入工时记录
     */
    public TimeEntryBatchImportResponseDTO importFromExcel(MultipartFile file) {
        log.info("开始从Excel导入工时记录，文件名：{}", file.getOriginalFilename());
        
        List<TimeEntryBatchImportRequestDTO> records = new ArrayList<>();
        List<TimeEntryBatchImportResponseDTO.FailureDetail> failures = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过表头（第一行）
            int rowNum = 0;
            for (Row row : sheet) {
                rowNum++;
                
                // 跳过表头
                if (rowNum == 1) {
                    continue;
                }
                
                // 跳过空行
                if (isEmptyRow(row)) {
                    continue;
                }
                
                try {
                    TimeEntryBatchImportRequestDTO record = parseExcelRow(row, rowNum);
                    records.add(record);
                } catch (Exception e) {
                    log.warn("解析Excel第{}行失败: {}", rowNum, e.getMessage());
                    failures.add(TimeEntryBatchImportResponseDTO.FailureDetail.builder()
                            .rowNumber(rowNum)
                            .reason("数据格式错误: " + e.getMessage())
                            .rawData(getRowData(row))
                            .build());
                }
            }
            
        } catch (Exception e) {
            log.error("读取Excel文件失败", e);
            throw new BusinessException("读取Excel文件失败: " + e.getMessage());
        }
        
        return processImport(records, failures);
    }
    
    /**
     * 从CSV文件批量导入工时记录
     */
    public TimeEntryBatchImportResponseDTO importFromCSV(MultipartFile file) {
        log.info("开始从CSV导入工时记录，文件名：{}", file.getOriginalFilename());
        
        List<TimeEntryBatchImportRequestDTO> records = new ArrayList<>();
        List<TimeEntryBatchImportResponseDTO.FailureDetail> failures = new ArrayList<>();
        
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            int rowNum = 1; // 表头是第1行，数据从第2行开始
            for (CSVRecord record : csvParser) {
                rowNum++;
                
                try {
                    TimeEntryBatchImportRequestDTO dto = parseCSVRecord(record, rowNum);
                    records.add(dto);
                } catch (Exception e) {
                    log.warn("解析CSV第{}行失败: {}", rowNum, e.getMessage());
                    failures.add(TimeEntryBatchImportResponseDTO.FailureDetail.builder()
                            .rowNumber(rowNum)
                            .reason("数据格式错误: " + e.getMessage())
                            .rawData(record.toString())
                            .build());
                }
            }
            
        } catch (Exception e) {
            log.error("读取CSV文件失败", e);
            throw new BusinessException("读取CSV文件失败: " + e.getMessage());
        }
        
        return processImport(records, failures);
    }
    
    /**
     * 生成Excel导入模板
     */
    public byte[] generateExcelTemplate() {
        log.info("生成Excel导入模板");
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("工时记录导入模板");
            
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle exampleStyle = createExampleStyle(workbook);
            
            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "项目ID或标识*", "任务ID", "用户ID或登录名*", "活动类型ID或名称*", 
                "工作日期(yyyy-MM-dd)*", "工时(小时)*", "备注"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 示例数据
            Row exampleRow = sheet.createRow(1);
            String[] examples = {
                "1", "100", "admin", "开发", "2026-01-14", "8.0", "完成用户登录功能"
            };
            
            for (int i = 0; i < examples.length; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(examples[i]);
                cell.setCellStyle(exampleStyle);
            }
            
            // 说明行
            Row noteRow = sheet.createRow(3);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("说明：带*为必填项。项目和用户可以填ID或标识/登录名。活动类型可以填ID或名称。");
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }
            
            workbook.write(out);
            log.info("Excel导入模板生成完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("生成Excel导入模板失败", e);
            throw new BusinessException("生成Excel导入模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成CSV导入模板
     */
    public byte[] generateCSVTemplate() {
        log.info("生成CSV导入模板");
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            
            // 添加BOM
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);
            
            // 表头
            csvPrinter.printRecord(
                "项目ID或标识*", "任务ID", "用户ID或登录名*", "活动类型ID或名称*",
                "工作日期(yyyy-MM-dd)*", "工时(小时)*", "备注"
            );
            
            // 示例数据
            csvPrinter.printRecord(
                "1", "100", "admin", "开发", "2026-01-14", "8.0", "完成用户登录功能"
            );
            
            csvPrinter.printRecord(
                "demo-project", "101", "developer", "测试", "2026-01-15", "4.0", "编写单元测试"
            );
            
            csvPrinter.flush();
            log.info("CSV导入模板生成完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("生成CSV导入模板失败", e);
            throw new BusinessException("生成CSV导入模板失败: " + e.getMessage());
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 处理导入
     */
    private TimeEntryBatchImportResponseDTO processImport(
            List<TimeEntryBatchImportRequestDTO> records,
            List<TimeEntryBatchImportResponseDTO.FailureDetail> failures) {
        
        int successCount = 0;
        int totalCount = records.size() + failures.size();
        
        for (int i = 0; i < records.size(); i++) {
            TimeEntryBatchImportRequestDTO record = records.get(i);
            
            try {
                // 转换为创建请求DTO
                TimeEntryCreateRequestDTO createDTO = convertToCreateDTO(record);
                
                // 调用创建服务
                timeEntryService.createTimeEntry(createDTO);
                successCount++;
                
            } catch (Exception e) {
                log.warn("导入第{}条记录失败: {}", i + 1, e.getMessage());
                failures.add(TimeEntryBatchImportResponseDTO.FailureDetail.builder()
                        .rowNumber(i + 2) // 加2是因为跳过了表头
                        .reason(e.getMessage())
                        .rawData(record.toString())
                        .build());
            }
        }
        
        log.info("批量导入完成：总数={}, 成功={}, 失败={}", totalCount, successCount, failures.size());
        
        return TimeEntryBatchImportResponseDTO.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failures.size())
                .failures(failures)
                .build();
    }
    
    /**
     * 转换为创建请求DTO
     */
    private TimeEntryCreateRequestDTO convertToCreateDTO(TimeEntryBatchImportRequestDTO importDTO) {
        // 解析项目ID
        Long projectId = parseProjectId(importDTO.getProject());
        
        // 解析用户ID
        Long userId = parseUserId(importDTO.getUser());
        
        // 解析活动类型ID
        Long activityId = parseActivityId(importDTO.getActivity());
        
        TimeEntryCreateRequestDTO dto = new TimeEntryCreateRequestDTO();
        dto.setProjectId(projectId);
        dto.setIssueId(importDTO.getIssueId());
        dto.setUserId(userId);
        dto.setActivityId(activityId);
        dto.setSpentOn(importDTO.getSpentOn());
        dto.setHours(importDTO.getHours());
        dto.setComments(importDTO.getComments());
        
        return dto;
    }
    
    /**
     * 解析项目ID
     */
    private Long parseProjectId(String projectStr) {
        if (projectStr == null || projectStr.trim().isEmpty()) {
            throw new BusinessException("项目ID不能为空");
        }
        
        // 尝试作为ID解析
        try {
            return Long.parseLong(projectStr.trim());
        } catch (NumberFormatException e) {
            // 作为项目标识查询
            Project project = projectMapper.selectOne(
                    new LambdaQueryWrapper<Project>()
                            .eq(Project::getIdentifier, projectStr.trim())
            );
            
            if (project == null) {
                throw new BusinessException("项目不存在: " + projectStr);
            }
            
            return project.getId();
        }
    }
    
    /**
     * 解析用户ID
     */
    private Long parseUserId(String userStr) {
        if (userStr == null || userStr.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }
        
        // 尝试作为ID解析
        try {
            return Long.parseLong(userStr.trim());
        } catch (NumberFormatException e) {
            // 作为登录名查询
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getLogin, userStr.trim())
            );
            
            if (user == null) {
                throw new BusinessException("用户不存在: " + userStr);
            }
            
            return user.getId();
        }
    }
    
    /**
     * 解析活动类型ID
     */
    private Long parseActivityId(String activityStr) {
        if (activityStr == null || activityStr.trim().isEmpty()) {
            throw new BusinessException("活动类型不能为空");
        }
        
        // 尝试作为ID解析
        try {
            return Long.parseLong(activityStr.trim());
        } catch (NumberFormatException e) {
            // 作为活动类型名称查询
            Enumeration enumeration = enumerationMapper.selectOne(
                    new LambdaQueryWrapper<Enumeration>()
                            .eq(Enumeration::getName, activityStr.trim())
                            .eq(Enumeration::getType, "TimeEntryActivity")
            );
            
            if (enumeration == null) {
                throw new BusinessException("活动类型不存在: " + activityStr);
            }
            
            return enumeration.getId().longValue();
        }
    }
    
    /**
     * 解析Excel行
     */
    private TimeEntryBatchImportRequestDTO parseExcelRow(Row row, int rowNum) {
        return TimeEntryBatchImportRequestDTO.builder()
                .project(getCellStringValue(row.getCell(0)))
                .issueId(getCellLongValue(row.getCell(1)))
                .user(getCellStringValue(row.getCell(2)))
                .activity(getCellStringValue(row.getCell(3)))
                .spentOn(getCellDateValue(row.getCell(4)))
                .hours(getCellFloatValue(row.getCell(5)))
                .comments(getCellStringValue(row.getCell(6)))
                .build();
    }
    
    /**
     * 解析CSV记录
     */
    private TimeEntryBatchImportRequestDTO parseCSVRecord(CSVRecord record, int rowNum) {
        return TimeEntryBatchImportRequestDTO.builder()
                .project(getCSVValue(record, 0))
                .issueId(getCSVLongValue(record, 1))
                .user(getCSVValue(record, 2))
                .activity(getCSVValue(record, 3))
                .spentOn(getCSVDateValue(record, 4))
                .hours(getCSVFloatValue(record, 5))
                .comments(getCSVValue(record, 6))
                .build();
    }
    
    /**
     * 判断是否为空行
     */
    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }
        
        for (int i = 0; i < 7; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 获取行数据字符串
     */
    private String getRowData(Row row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getCellStringValue(row.getCell(i)));
        }
        return sb.toString();
    }
    
    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DATE_FORMATTER.format(cell.getLocalDateTimeCellValue().toLocalDate());
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == (long) numValue) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }
    
    /**
     * 获取单元格Long值
     */
    private Long getCellLongValue(Cell cell) {
        String value = getCellStringValue(cell);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("无效的数字: " + value);
        }
    }
    
    /**
     * 获取单元格Float值
     */
    private Float getCellFloatValue(Cell cell) {
        String value = getCellStringValue(cell);
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("工时不能为空");
        }
        
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("无效的工时值: " + value);
        }
    }
    
    /**
     * 获取单元格日期值
     */
    private LocalDate getCellDateValue(Cell cell) {
        if (cell == null) {
            throw new BusinessException("工作日期不能为空");
        }
        
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        
        String value = getCellStringValue(cell);
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("工作日期不能为空");
        }
        
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException("无效的日期格式: " + value + "，应为 yyyy-MM-dd");
        }
    }
    
    /**
     * 获取CSV值
     */
    private String getCSVValue(CSVRecord record, int index) {
        try {
            String value = record.get(index);
            return value == null || value.trim().isEmpty() ? null : value.trim();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取CSV Long值
     */
    private Long getCSVLongValue(CSVRecord record, int index) {
        String value = getCSVValue(record, index);
        if (value == null) {
            return null;
        }
        
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("无效的数字: " + value);
        }
    }
    
    /**
     * 获取CSV Float值
     */
    private Float getCSVFloatValue(CSVRecord record, int index) {
        String value = getCSVValue(record, index);
        if (value == null) {
            throw new BusinessException("工时不能为空");
        }
        
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("无效的工时值: " + value);
        }
    }
    
    /**
     * 获取CSV日期值
     */
    private LocalDate getCSVDateValue(CSVRecord record, int index) {
        String value = getCSVValue(record, index);
        if (value == null) {
            throw new BusinessException("工作日期不能为空");
        }
        
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException("无效的日期格式: " + value + "，应为 yyyy-MM-dd");
        }
    }
    
    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    /**
     * 创建示例样式
     */
    private CellStyle createExampleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
