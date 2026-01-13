package com.github.jredmine.service;

import com.github.jredmine.dto.request.timeentry.TimeEntryReportRequestDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryPeriodReportDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryProjectReportDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryUserReportDTO;
import com.github.jredmine.exception.BusinessException;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 工时记录导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeEntryExportService {
    
    private final TimeEntryService timeEntryService;
    
    /**
     * 导出项目工时报表为Excel
     */
    public byte[] exportProjectReportToExcel(TimeEntryReportRequestDTO request) {
        log.info("开始导出项目工时报表为Excel: projectId={}", request.getProjectId());
        
        TimeEntryProjectReportDTO report = timeEntryService.generateProjectReport(request);
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("项目工时报表");
            
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            int rowNum = 0;
            
            // 标题
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("项目工时报表");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));
            
            // 空行
            rowNum++;
            
            // 基础信息
            addInfoRow(sheet, rowNum++, "项目名称", report.getProject().getName(), dataStyle);
            addInfoRow(sheet, rowNum++, "项目标识", report.getProject().getIdentifier(), dataStyle);
            addInfoRow(sheet, rowNum++, "总工时", report.getTotalHours() + " 小时", dataStyle);
            addInfoRow(sheet, rowNum++, "记录总数", report.getTotalCount().toString(), dataStyle);
            addInfoRow(sheet, rowNum++, "参与人数", report.getUserCount().toString(), dataStyle);
            addInfoRow(sheet, rowNum++, "平均工时", report.getAverageHours() + " 小时", dataStyle);
            addInfoRow(sheet, rowNum++, "时间范围", report.getEarliestDate() + " 至 " + report.getLatestDate(), dataStyle);
            
            // 空行
            rowNum++;
            
            // 成员工时详情
            Row userHeaderRow = sheet.createRow(rowNum++);
            userHeaderRow.createCell(0).setCellValue("成员工时详情");
            Cell userHeaderCell = userHeaderRow.getCell(0);
            userHeaderCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));
            
            Row userColumnRow = sheet.createRow(rowNum++);
            String[] userColumns = {"序号", "用户名", "工时(小时)", "记录数", "占比(%)"};
            for (int i = 0; i < userColumns.length; i++) {
                Cell cell = userColumnRow.createCell(i);
                cell.setCellValue(userColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int userIndex = 1;
            for (TimeEntryProjectReportDTO.UserTimeDetail detail : report.getUserDetails()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(userIndex++);
                row.createCell(1).setCellValue(detail.getUserName());
                row.createCell(2).setCellValue(detail.getHours());
                row.createCell(3).setCellValue(detail.getCount());
                row.createCell(4).setCellValue(detail.getPercentage());
                
                for (int i = 0; i < 5; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
            
            // 空行
            rowNum++;
            
            // 活动类型工时详情
            Row activityHeaderRow = sheet.createRow(rowNum++);
            activityHeaderRow.createCell(0).setCellValue("活动类型工时详情");
            Cell activityHeaderCell = activityHeaderRow.getCell(0);
            activityHeaderCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));
            
            Row activityColumnRow = sheet.createRow(rowNum++);
            String[] activityColumns = {"序号", "活动类型", "工时(小时)", "记录数", "占比(%)"};
            for (int i = 0; i < activityColumns.length; i++) {
                Cell cell = activityColumnRow.createCell(i);
                cell.setCellValue(activityColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int activityIndex = 1;
            for (TimeEntryProjectReportDTO.ActivityTimeDetail detail : report.getActivityDetails()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(activityIndex++);
                row.createCell(1).setCellValue(detail.getActivityName());
                row.createCell(2).setCellValue(detail.getHours());
                row.createCell(3).setCellValue(detail.getCount());
                row.createCell(4).setCellValue(detail.getPercentage());
                
                for (int i = 0; i < 5; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            log.info("项目工时报表Excel导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出项目工时报表Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出用户工时报表为Excel
     */
    public byte[] exportUserReportToExcel(TimeEntryReportRequestDTO request) {
        log.info("开始导出用户工时报表为Excel: userId={}", request.getUserId());
        
        TimeEntryUserReportDTO report = timeEntryService.generateUserReport(request);
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("用户工时报表");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            int rowNum = 0;
            
            // 标题
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("用户工时报表");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));
            
            rowNum++;
            
            // 基础信息
            String userName = report.getUser().getFirstname() + report.getUser().getLastname() + 
                            " (" + report.getUser().getLogin() + ")";
            addInfoRow(sheet, rowNum++, "用户", userName, dataStyle);
            addInfoRow(sheet, rowNum++, "总工时", report.getTotalHours() + " 小时", dataStyle);
            addInfoRow(sheet, rowNum++, "记录总数", report.getTotalCount().toString(), dataStyle);
            addInfoRow(sheet, rowNum++, "参与项目数", report.getProjectCount().toString(), dataStyle);
            addInfoRow(sheet, rowNum++, "平均工时", report.getAverageHours() + " 小时", dataStyle);
            addInfoRow(sheet, rowNum++, "时间范围", report.getEarliestDate() + " 至 " + report.getLatestDate(), dataStyle);
            
            rowNum++;
            
            // 项目工时详情
            Row projectHeaderRow = sheet.createRow(rowNum++);
            projectHeaderRow.createCell(0).setCellValue("项目工时详情");
            projectHeaderRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));
            
            Row projectColumnRow = sheet.createRow(rowNum++);
            String[] projectColumns = {"序号", "项目名称", "工时(小时)", "记录数", "占比(%)"};
            for (int i = 0; i < projectColumns.length; i++) {
                Cell cell = projectColumnRow.createCell(i);
                cell.setCellValue(projectColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int projectIndex = 1;
            for (TimeEntryUserReportDTO.ProjectTimeDetail detail : report.getProjectDetails()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(projectIndex++);
                row.createCell(1).setCellValue(detail.getProjectName());
                row.createCell(2).setCellValue(detail.getHours());
                row.createCell(3).setCellValue(detail.getCount());
                row.createCell(4).setCellValue(detail.getPercentage());
                
                for (int i = 0; i < 5; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
            
            rowNum++;
            
            // 活动类型工时详情
            Row activityHeaderRow = sheet.createRow(rowNum++);
            activityHeaderRow.createCell(0).setCellValue("活动类型工时详情");
            activityHeaderRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));
            
            Row activityColumnRow = sheet.createRow(rowNum++);
            String[] activityColumns = {"序号", "活动类型", "工时(小时)", "记录数", "占比(%)"};
            for (int i = 0; i < activityColumns.length; i++) {
                Cell cell = activityColumnRow.createCell(i);
                cell.setCellValue(activityColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int activityIndex = 1;
            for (TimeEntryUserReportDTO.ActivityTimeDetail detail : report.getActivityDetails()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(activityIndex++);
                row.createCell(1).setCellValue(detail.getActivityName());
                row.createCell(2).setCellValue(detail.getHours());
                row.createCell(3).setCellValue(detail.getCount());
                row.createCell(4).setCellValue(detail.getPercentage());
                
                for (int i = 0; i < 5; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
            
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            log.info("用户工时报表Excel导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出用户工时报表Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出时间段工时报表为Excel
     */
    public byte[] exportPeriodReportToExcel(TimeEntryReportRequestDTO request) {
        log.info("开始导出时间段工时报表为Excel: periodType={}", request.getPeriodType());
        
        TimeEntryPeriodReportDTO report = timeEntryService.generatePeriodReport(request);
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("时间段工时报表");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            int rowNum = 0;
            
            // 标题
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("时间段工时报表");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));
            
            rowNum++;
            
            // 基础信息
            addInfoRow(sheet, rowNum++, "时间段类型", getPeriodTypeName(report.getPeriodType()), dataStyle);
            addInfoRow(sheet, rowNum++, "开始日期", report.getStartDate(), dataStyle);
            addInfoRow(sheet, rowNum++, "结束日期", report.getEndDate(), dataStyle);
            addInfoRow(sheet, rowNum++, "总工时", report.getTotalHours() + " 小时", dataStyle);
            addInfoRow(sheet, rowNum++, "记录总数", report.getTotalCount().toString(), dataStyle);
            addInfoRow(sheet, rowNum++, "平均每日工时", report.getAverageDailyHours() + " 小时", dataStyle);
            addInfoRow(sheet, rowNum++, "最高单日工时", report.getMaxDailyHours() + " 小时", dataStyle);
            addInfoRow(sheet, rowNum++, "最低单日工时", report.getMinDailyHours() + " 小时", dataStyle);
            
            rowNum++;
            
            // 工时趋势详情
            Row trendHeaderRow = sheet.createRow(rowNum++);
            trendHeaderRow.createCell(0).setCellValue("工时趋势详情");
            trendHeaderRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));
            
            Row columnRow = sheet.createRow(rowNum++);
            String[] columns = {"序号", "时间段", "工时(小时)", "记录数", "参与人数"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = columnRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int index = 1;
            for (TimeEntryPeriodReportDTO.PeriodTimeDetail detail : report.getPeriodDetails()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(index++);
                row.createCell(1).setCellValue(detail.getPeriodName());
                row.createCell(2).setCellValue(detail.getHours());
                row.createCell(3).setCellValue(detail.getCount());
                row.createCell(4).setCellValue(detail.getUserCount());
                
                for (int i = 0; i < 5; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
            
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            log.info("时间段工时报表Excel导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出时间段工时报表Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出项目工时报表为CSV
     */
    public byte[] exportProjectReportToCSV(TimeEntryReportRequestDTO request) {
        log.info("开始导出项目工时报表为CSV: projectId={}", request.getProjectId());
        
        TimeEntryProjectReportDTO report = timeEntryService.generateProjectReport(request);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     "类别", "字段", "值"))) {
            
            // 添加BOM以支持Excel正确识别UTF-8
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);
            
            // 基础信息
            csvPrinter.printRecord("基础信息", "项目名称", report.getProject().getName());
            csvPrinter.printRecord("基础信息", "项目标识", report.getProject().getIdentifier());
            csvPrinter.printRecord("基础信息", "总工时", report.getTotalHours() + " 小时");
            csvPrinter.printRecord("基础信息", "记录总数", report.getTotalCount());
            csvPrinter.printRecord("基础信息", "参与人数", report.getUserCount());
            csvPrinter.printRecord("基础信息", "平均工时", report.getAverageHours() + " 小时");
            csvPrinter.printRecord("基础信息", "时间范围", report.getEarliestDate() + " 至 " + report.getLatestDate());
            
            csvPrinter.println();
            
            // 成员工时详情
            csvPrinter.printRecord("成员工时", "序号", "用户名", "工时(小时)", "记录数", "占比(%)");
            int userIndex = 1;
            for (TimeEntryProjectReportDTO.UserTimeDetail detail : report.getUserDetails()) {
                csvPrinter.printRecord("成员工时", userIndex++, detail.getUserName(), 
                        detail.getHours(), detail.getCount(), detail.getPercentage());
            }
            
            csvPrinter.println();
            
            // 活动类型工时详情
            csvPrinter.printRecord("活动类型", "序号", "活动类型", "工时(小时)", "记录数", "占比(%)");
            int activityIndex = 1;
            for (TimeEntryProjectReportDTO.ActivityTimeDetail detail : report.getActivityDetails()) {
                csvPrinter.printRecord("活动类型", activityIndex++, detail.getActivityName(),
                        detail.getHours(), detail.getCount(), detail.getPercentage());
            }
            
            csvPrinter.flush();
            log.info("项目工时报表CSV导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出项目工时报表CSV失败", e);
            throw new BusinessException("导出CSV失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出用户工时报表为CSV
     */
    public byte[] exportUserReportToCSV(TimeEntryReportRequestDTO request) {
        log.info("开始导出用户工时报表为CSV: userId={}", request.getUserId());
        
        TimeEntryUserReportDTO report = timeEntryService.generateUserReport(request);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);
            
            String userName = report.getUser().getFirstname() + report.getUser().getLastname() + 
                            " (" + report.getUser().getLogin() + ")";
            
            csvPrinter.printRecord("类别", "字段", "值");
            csvPrinter.printRecord("基础信息", "用户", userName);
            csvPrinter.printRecord("基础信息", "总工时", report.getTotalHours() + " 小时");
            csvPrinter.printRecord("基础信息", "记录总数", report.getTotalCount());
            csvPrinter.printRecord("基础信息", "参与项目数", report.getProjectCount());
            csvPrinter.printRecord("基础信息", "平均工时", report.getAverageHours() + " 小时");
            csvPrinter.printRecord("基础信息", "时间范围", report.getEarliestDate() + " 至 " + report.getLatestDate());
            
            csvPrinter.println();
            
            csvPrinter.printRecord("项目工时", "序号", "项目名称", "工时(小时)", "记录数", "占比(%)");
            int projectIndex = 1;
            for (TimeEntryUserReportDTO.ProjectTimeDetail detail : report.getProjectDetails()) {
                csvPrinter.printRecord("项目工时", projectIndex++, detail.getProjectName(),
                        detail.getHours(), detail.getCount(), detail.getPercentage());
            }
            
            csvPrinter.println();
            
            csvPrinter.printRecord("活动类型", "序号", "活动类型", "工时(小时)", "记录数", "占比(%)");
            int activityIndex = 1;
            for (TimeEntryUserReportDTO.ActivityTimeDetail detail : report.getActivityDetails()) {
                csvPrinter.printRecord("活动类型", activityIndex++, detail.getActivityName(),
                        detail.getHours(), detail.getCount(), detail.getPercentage());
            }
            
            csvPrinter.flush();
            log.info("用户工时报表CSV导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出用户工时报表CSV失败", e);
            throw new BusinessException("导出CSV失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出时间段工时报表为CSV
     */
    public byte[] exportPeriodReportToCSV(TimeEntryReportRequestDTO request) {
        log.info("开始导出时间段工时报表为CSV: periodType={}", request.getPeriodType());
        
        TimeEntryPeriodReportDTO report = timeEntryService.generatePeriodReport(request);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);
            
            csvPrinter.printRecord("类别", "字段", "值");
            csvPrinter.printRecord("基础信息", "时间段类型", getPeriodTypeName(report.getPeriodType()));
            csvPrinter.printRecord("基础信息", "开始日期", report.getStartDate());
            csvPrinter.printRecord("基础信息", "结束日期", report.getEndDate());
            csvPrinter.printRecord("基础信息", "总工时", report.getTotalHours() + " 小时");
            csvPrinter.printRecord("基础信息", "记录总数", report.getTotalCount());
            csvPrinter.printRecord("基础信息", "平均每日工时", report.getAverageDailyHours() + " 小时");
            csvPrinter.printRecord("基础信息", "最高单日工时", report.getMaxDailyHours() + " 小时");
            csvPrinter.printRecord("基础信息", "最低单日工时", report.getMinDailyHours() + " 小时");
            
            csvPrinter.println();
            
            csvPrinter.printRecord("工时趋势", "序号", "时间段", "工时(小时)", "记录数", "参与人数");
            int index = 1;
            for (TimeEntryPeriodReportDTO.PeriodTimeDetail detail : report.getPeriodDetails()) {
                csvPrinter.printRecord("工时趋势", index++, detail.getPeriodName(),
                        detail.getHours(), detail.getCount(), detail.getUserCount());
            }
            
            csvPrinter.flush();
            log.info("时间段工时报表CSV导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出时间段工时报表CSV失败", e);
            throw new BusinessException("导出CSV失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出项目工时报表为PDF
     */
    public byte[] exportProjectReportToPDF(TimeEntryReportRequestDTO request) {
        log.info("开始导出项目工时报表为PDF: projectId={}", request.getProjectId());
        
        TimeEntryProjectReportDTO report = timeEntryService.generateProjectReport(request);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // 标题
            Paragraph title = new Paragraph("Project Time Entry Report")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            
            document.add(new Paragraph("\n"));
            
            // 基础信息表格
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            infoTable.setWidth(UnitValue.createPercentValue(100));
            
            addPdfInfoRow(infoTable, "Project Name", report.getProject().getName());
            addPdfInfoRow(infoTable, "Project Identifier", report.getProject().getIdentifier());
            addPdfInfoRow(infoTable, "Total Hours", report.getTotalHours() + " hours");
            addPdfInfoRow(infoTable, "Total Count", report.getTotalCount().toString());
            addPdfInfoRow(infoTable, "User Count", report.getUserCount().toString());
            addPdfInfoRow(infoTable, "Average Hours", report.getAverageHours() + " hours");
            addPdfInfoRow(infoTable, "Date Range", report.getEarliestDate() + " to " + report.getLatestDate());
            
            document.add(infoTable);
            document.add(new Paragraph("\n"));
            
            // 成员工时详情
            Paragraph userTitle = new Paragraph("User Time Details")
                    .setFontSize(14)
                    .setBold();
            document.add(userTitle);
            
            Table userTable = new Table(UnitValue.createPercentArray(new float[]{10, 30, 20, 20, 20}));
            userTable.setWidth(UnitValue.createPercentValue(100));
            
            addPdfHeaderCell(userTable, "No.");
            addPdfHeaderCell(userTable, "User Name");
            addPdfHeaderCell(userTable, "Hours");
            addPdfHeaderCell(userTable, "Count");
            addPdfHeaderCell(userTable, "Percentage(%)");
            
            int userIndex = 1;
            for (TimeEntryProjectReportDTO.UserTimeDetail detail : report.getUserDetails()) {
                userTable.addCell(String.valueOf(userIndex++));
                userTable.addCell(detail.getUserName());
                userTable.addCell(detail.getHours().toString());
                userTable.addCell(detail.getCount().toString());
                userTable.addCell(detail.getPercentage().toString());
            }
            
            document.add(userTable);
            document.add(new Paragraph("\n"));
            
            // 活动类型工时详情
            Paragraph activityTitle = new Paragraph("Activity Time Details")
                    .setFontSize(14)
                    .setBold();
            document.add(activityTitle);
            
            Table activityTable = new Table(UnitValue.createPercentArray(new float[]{10, 30, 20, 20, 20}));
            activityTable.setWidth(UnitValue.createPercentValue(100));
            
            addPdfHeaderCell(activityTable, "No.");
            addPdfHeaderCell(activityTable, "Activity Name");
            addPdfHeaderCell(activityTable, "Hours");
            addPdfHeaderCell(activityTable, "Count");
            addPdfHeaderCell(activityTable, "Percentage(%)");
            
            int activityIndex = 1;
            for (TimeEntryProjectReportDTO.ActivityTimeDetail detail : report.getActivityDetails()) {
                activityTable.addCell(String.valueOf(activityIndex++));
                activityTable.addCell(detail.getActivityName());
                activityTable.addCell(detail.getHours().toString());
                activityTable.addCell(detail.getCount().toString());
                activityTable.addCell(detail.getPercentage().toString());
            }
            
            document.add(activityTable);
            
            // 添加页脚
            addPdfFooter(document);
            
            document.close();
            log.info("项目工时报表PDF导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出项目工时报表PDF失败", e);
            throw new BusinessException("导出PDF失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出用户工时报表为PDF
     */
    public byte[] exportUserReportToPDF(TimeEntryReportRequestDTO request) {
        log.info("开始导出用户工时报表为PDF: userId={}", request.getUserId());
        
        TimeEntryUserReportDTO report = timeEntryService.generateUserReport(request);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            Paragraph title = new Paragraph("User Time Entry Report")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));
            
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            infoTable.setWidth(UnitValue.createPercentValue(100));
            
            String userName = report.getUser().getFirstname() + " " + report.getUser().getLastname() + 
                            " (" + report.getUser().getLogin() + ")";
            addPdfInfoRow(infoTable, "User", userName);
            addPdfInfoRow(infoTable, "Total Hours", report.getTotalHours() + " hours");
            addPdfInfoRow(infoTable, "Total Count", report.getTotalCount().toString());
            addPdfInfoRow(infoTable, "Project Count", report.getProjectCount().toString());
            addPdfInfoRow(infoTable, "Average Hours", report.getAverageHours() + " hours");
            addPdfInfoRow(infoTable, "Date Range", report.getEarliestDate() + " to " + report.getLatestDate());
            
            document.add(infoTable);
            document.add(new Paragraph("\n"));
            
            Paragraph projectTitle = new Paragraph("Project Time Details")
                    .setFontSize(14)
                    .setBold();
            document.add(projectTitle);
            
            Table projectTable = new Table(UnitValue.createPercentArray(new float[]{10, 30, 20, 20, 20}));
            projectTable.setWidth(UnitValue.createPercentValue(100));
            
            addPdfHeaderCell(projectTable, "No.");
            addPdfHeaderCell(projectTable, "Project Name");
            addPdfHeaderCell(projectTable, "Hours");
            addPdfHeaderCell(projectTable, "Count");
            addPdfHeaderCell(projectTable, "Percentage(%)");
            
            int projectIndex = 1;
            for (TimeEntryUserReportDTO.ProjectTimeDetail detail : report.getProjectDetails()) {
                projectTable.addCell(String.valueOf(projectIndex++));
                projectTable.addCell(detail.getProjectName());
                projectTable.addCell(detail.getHours().toString());
                projectTable.addCell(detail.getCount().toString());
                projectTable.addCell(detail.getPercentage().toString());
            }
            
            document.add(projectTable);
            document.add(new Paragraph("\n"));
            
            Paragraph activityTitle = new Paragraph("Activity Time Details")
                    .setFontSize(14)
                    .setBold();
            document.add(activityTitle);
            
            Table activityTable = new Table(UnitValue.createPercentArray(new float[]{10, 30, 20, 20, 20}));
            activityTable.setWidth(UnitValue.createPercentValue(100));
            
            addPdfHeaderCell(activityTable, "No.");
            addPdfHeaderCell(activityTable, "Activity Name");
            addPdfHeaderCell(activityTable, "Hours");
            addPdfHeaderCell(activityTable, "Count");
            addPdfHeaderCell(activityTable, "Percentage(%)");
            
            int activityIndex = 1;
            for (TimeEntryUserReportDTO.ActivityTimeDetail detail : report.getActivityDetails()) {
                activityTable.addCell(String.valueOf(activityIndex++));
                activityTable.addCell(detail.getActivityName());
                activityTable.addCell(detail.getHours().toString());
                activityTable.addCell(detail.getCount().toString());
                activityTable.addCell(detail.getPercentage().toString());
            }
            
            document.add(activityTable);
            addPdfFooter(document);
            
            document.close();
            log.info("用户工时报表PDF导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出用户工时报表PDF失败", e);
            throw new BusinessException("导出PDF失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出时间段工时报表为PDF
     */
    public byte[] exportPeriodReportToPDF(TimeEntryReportRequestDTO request) {
        log.info("开始导出时间段工时报表为PDF: periodType={}", request.getPeriodType());
        
        TimeEntryPeriodReportDTO report = timeEntryService.generatePeriodReport(request);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            Paragraph title = new Paragraph("Period Time Entry Report")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));
            
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            infoTable.setWidth(UnitValue.createPercentValue(100));
            
            addPdfInfoRow(infoTable, "Period Type", getPeriodTypeName(report.getPeriodType()));
            addPdfInfoRow(infoTable, "Start Date", report.getStartDate());
            addPdfInfoRow(infoTable, "End Date", report.getEndDate());
            addPdfInfoRow(infoTable, "Total Hours", report.getTotalHours() + " hours");
            addPdfInfoRow(infoTable, "Total Count", report.getTotalCount().toString());
            addPdfInfoRow(infoTable, "Average Daily Hours", report.getAverageDailyHours() + " hours");
            addPdfInfoRow(infoTable, "Max Daily Hours", report.getMaxDailyHours() + " hours");
            addPdfInfoRow(infoTable, "Min Daily Hours", report.getMinDailyHours() + " hours");
            
            document.add(infoTable);
            document.add(new Paragraph("\n"));
            
            Paragraph trendTitle = new Paragraph("Time Trend Details")
                    .setFontSize(14)
                    .setBold();
            document.add(trendTitle);
            
            Table trendTable = new Table(UnitValue.createPercentArray(new float[]{10, 30, 20, 20, 20}));
            trendTable.setWidth(UnitValue.createPercentValue(100));
            
            addPdfHeaderCell(trendTable, "No.");
            addPdfHeaderCell(trendTable, "Period");
            addPdfHeaderCell(trendTable, "Hours");
            addPdfHeaderCell(trendTable, "Count");
            addPdfHeaderCell(trendTable, "User Count");
            
            int index = 1;
            for (TimeEntryPeriodReportDTO.PeriodTimeDetail detail : report.getPeriodDetails()) {
                trendTable.addCell(String.valueOf(index++));
                trendTable.addCell(detail.getPeriodName());
                trendTable.addCell(detail.getHours().toString());
                trendTable.addCell(detail.getCount().toString());
                trendTable.addCell(detail.getUserCount().toString());
            }
            
            document.add(trendTable);
            addPdfFooter(document);
            
            document.close();
            log.info("时间段工时报表PDF导出完成");
            return out.toByteArray();
            
        } catch (Exception e) {
            log.error("导出时间段工时报表PDF失败", e);
            throw new BusinessException("导出PDF失败: " + e.getMessage());
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
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
    
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private void addInfoRow(Sheet sheet, int rowNum, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(style);
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(style);
    }
    
    private void addPdfInfoRow(Table table, String label, String value) {
        com.itextpdf.layout.element.Cell labelCell = new com.itextpdf.layout.element.Cell().add(new Paragraph(label).setBold());
        labelCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        table.addCell(labelCell);
        table.addCell(value);
    }
    
    private void addPdfHeaderCell(Table table, String text) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell().add(new Paragraph(text).setBold());
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell.setTextAlignment(TextAlignment.CENTER);
        table.addHeaderCell(cell);
    }
    
    private void addPdfFooter(Document document) {
        document.add(new Paragraph("\n"));
        String footer = "Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Paragraph footerPara = new Paragraph(footer)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(footerPara);
    }
    
    private String getPeriodTypeName(String periodType) {
        switch (periodType.toLowerCase()) {
            case "day":
                return "Daily";
            case "week":
                return "Weekly";
            case "month":
                return "Monthly";
            default:
                return periodType;
        }
    }
}
