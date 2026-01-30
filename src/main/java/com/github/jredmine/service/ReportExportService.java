package com.github.jredmine.service;

import com.github.jredmine.dto.request.report.BurndownReportRequestDTO;
import com.github.jredmine.dto.request.report.UserWorkloadReportRequestDTO;
import com.github.jredmine.dto.request.timeentry.TimeEntryReportRequestDTO;
import com.github.jredmine.dto.response.project.ProjectStatisticsResponseDTO;
import com.github.jredmine.dto.response.report.BurndownReportResponseDTO;
import com.github.jredmine.dto.response.report.UserWorkloadReportResponseDTO;
import com.github.jredmine.dto.response.timeentry.TimeEntryReportResponseDTO;
import com.github.jredmine.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 报表导出服务：项目统计、工时统计、用户工作量、燃尽图等报表的 Excel/CSV 导出
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final ProjectService projectService;
    private final TimeEntryService timeEntryService;
    private final ReportService reportService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== 项目统计报表 ====================

    public byte[] exportProjectReportToExcel(Long projectId) {
        log.info("开始导出项目统计报表为Excel: projectId={}", projectId);
        ProjectStatisticsResponseDTO dto = projectService.getProjectStatistics(projectId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("项目统计报表");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("项目统计报表");
            titleRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowNum++;

            addInfoRow(sheet, rowNum++, "项目名称", nullToEmpty(dto.getProjectName()), dataStyle);
            addInfoRow(sheet, rowNum++, "成员数量", nullToStr(dto.getMemberCount()), dataStyle);
            addInfoRow(sheet, rowNum++, "子项目数", nullToStr(dto.getChildrenCount()), dataStyle);
            addInfoRow(sheet, rowNum++, "跟踪器数", nullToStr(dto.getTrackerCount()), dataStyle);
            addInfoRow(sheet, rowNum++, "版本数", nullToStr(dto.getVersionCount()), dataStyle);
            rowNum++;

            ProjectStatisticsResponseDTO.IssueStatistics issueStats = dto.getIssueStatistics();
            if (issueStats != null) {
                addInfoRow(sheet, rowNum++, "任务总数", nullToStr(issueStats.getTotalCount()), dataStyle);
                addInfoRow(sheet, rowNum++, "待处理", nullToStr(issueStats.getPendingCount()), dataStyle);
                addInfoRow(sheet, rowNum++, "进行中", nullToStr(issueStats.getInProgressCount()), dataStyle);
                addInfoRow(sheet, rowNum++, "已完成", nullToStr(issueStats.getCompletedCount()), dataStyle);
                addInfoRow(sheet, rowNum++, "完成率(%)", nullToStr(issueStats.getCompletionRate()), dataStyle);
                rowNum++;
            }

            ProjectStatisticsResponseDTO.TimeEntryStatistics teStats = dto.getTimeEntryStatistics();
            if (teStats != null) {
                addInfoRow(sheet, rowNum++, "总工时(小时)", nullToStr(teStats.getTotalHours()), dataStyle);
                addInfoRow(sheet, rowNum++, "本月工时", nullToStr(teStats.getMonthlyHours()), dataStyle);
                addInfoRow(sheet, rowNum++, "本周工时", nullToStr(teStats.getWeeklyHours()), dataStyle);
                addInfoRow(sheet, rowNum++, "工时记录条数", nullToStr(teStats.getEntryCount()), dataStyle);
            }

            autoSizeColumns(sheet, 2);
            workbook.write(out);
            log.info("项目统计报表Excel导出完成");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出项目统计报表Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }

    public byte[] exportProjectReportToCSV(Long projectId) {
        log.info("开始导出项目统计报表为CSV: projectId={}", projectId);
        ProjectStatisticsResponseDTO dto = projectService.getProjectStatistics(projectId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("项目", "指标", "值"))) {
            writeBOM(out);
            printer.printRecord("项目统计", "项目名称", nullToEmpty(dto.getProjectName()));
            printer.printRecord("项目统计", "成员数量", nullToStr(dto.getMemberCount()));
            printer.printRecord("项目统计", "子项目数", nullToStr(dto.getChildrenCount()));
            printer.printRecord("项目统计", "跟踪器数", nullToStr(dto.getTrackerCount()));
            printer.printRecord("项目统计", "版本数", nullToStr(dto.getVersionCount()));
            ProjectStatisticsResponseDTO.IssueStatistics issueStats = dto.getIssueStatistics();
            if (issueStats != null) {
                printer.printRecord("任务统计", "任务总数", nullToStr(issueStats.getTotalCount()));
                printer.printRecord("任务统计", "待处理", nullToStr(issueStats.getPendingCount()));
                printer.printRecord("任务统计", "进行中", nullToStr(issueStats.getInProgressCount()));
                printer.printRecord("任务统计", "已完成", nullToStr(issueStats.getCompletedCount()));
                printer.printRecord("任务统计", "完成率(%)", nullToStr(issueStats.getCompletionRate()));
            }
            ProjectStatisticsResponseDTO.TimeEntryStatistics teStats = dto.getTimeEntryStatistics();
            if (teStats != null) {
                printer.printRecord("工时统计", "总工时(小时)", nullToStr(teStats.getTotalHours()));
                printer.printRecord("工时统计", "本月工时", nullToStr(teStats.getMonthlyHours()));
                printer.printRecord("工时统计", "本周工时", nullToStr(teStats.getWeeklyHours()));
                printer.printRecord("工时统计", "记录条数", nullToStr(teStats.getEntryCount()));
            }
            printer.flush();
            log.info("项目统计报表CSV导出完成");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出项目统计报表CSV失败", e);
            throw new BusinessException("导出CSV失败: " + e.getMessage());
        }
    }

    // ==================== 工时统计报表 ====================

    public byte[] exportTimeEntryReportToExcel(TimeEntryReportRequestDTO request) {
        log.info("开始导出工时统计报表为Excel");
        TimeEntryReportResponseDTO dto = timeEntryService.getTimeEntryReport(request);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 汇总
            Sheet summarySheet = workbook.createSheet("汇总");
            int rowNum = 0;
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("工时统计报表");
            summarySheet.getRow(0).getCell(0).setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
            rowNum++;
            TimeEntryReportResponseDTO.Summary sum = dto.getSummary();
            if (sum != null) {
                addInfoRow(summarySheet, rowNum++, "总工时(小时)", nullToStr(sum.getTotalHours()), dataStyle);
                addInfoRow(summarySheet, rowNum++, "记录总数", nullToStr(sum.getTotalCount()), dataStyle);
                addInfoRow(summarySheet, rowNum++, "参与用户数", nullToStr(sum.getUserCount()), dataStyle);
                addInfoRow(summarySheet, rowNum++, "涉及项目数", nullToStr(sum.getProjectCount()), dataStyle);
                addInfoRow(summarySheet, rowNum++, "时间范围", nullToEmpty(sum.getEarliestDate()) + " ~ " + nullToEmpty(sum.getLatestDate()), dataStyle);
            }
            autoSizeColumns(summarySheet, 2);

            // 按用户
            if (dto.getByUser() != null && !dto.getByUser().isEmpty()) {
                Sheet userSheet = workbook.createSheet("按用户");
                writeGroupSheet(userSheet, dto.getByUser(), "用户", headerStyle, dataStyle);
            }
            // 按项目
            if (dto.getByProject() != null && !dto.getByProject().isEmpty()) {
                Sheet projectSheet = workbook.createSheet("按项目");
                writeGroupSheet(projectSheet, dto.getByProject(), "项目", headerStyle, dataStyle);
            }
            // 按活动类型
            if (dto.getByActivity() != null && !dto.getByActivity().isEmpty()) {
                Sheet activitySheet = workbook.createSheet("按活动类型");
                writeGroupSheet(activitySheet, dto.getByActivity(), "活动类型", headerStyle, dataStyle);
            }

            workbook.write(out);
            log.info("工时统计报表Excel导出完成");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出工时统计报表Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }

    public byte[] exportTimeEntryReportToCSV(TimeEntryReportRequestDTO request) {
        log.info("开始导出工时统计报表为CSV");
        TimeEntryReportResponseDTO dto = timeEntryService.getTimeEntryReport(request);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("维度", "名称", "工时", "记录数", "占比%"))) {
            writeBOM(out);
            if (dto.getByUser() != null) {
                for (TimeEntryReportResponseDTO.GroupItem item : dto.getByUser()) {
                    printer.printRecord("按用户", item.getGroupName(), item.getHours(), item.getCount(), item.getPercentage());
                }
            }
            if (dto.getByProject() != null) {
                for (TimeEntryReportResponseDTO.GroupItem item : dto.getByProject()) {
                    printer.printRecord("按项目", item.getGroupName(), item.getHours(), item.getCount(), item.getPercentage());
                }
            }
            if (dto.getByActivity() != null) {
                for (TimeEntryReportResponseDTO.GroupItem item : dto.getByActivity()) {
                    printer.printRecord("按活动类型", item.getGroupName(), item.getHours(), item.getCount(), item.getPercentage());
                }
            }
            printer.flush();
            log.info("工时统计报表CSV导出完成");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出工时统计报表CSV失败", e);
            throw new BusinessException("导出CSV失败: " + e.getMessage());
        }
    }

    // ==================== 用户工作量报表 ====================

    public byte[] exportUserWorkloadReportToExcel(UserWorkloadReportRequestDTO request) {
        log.info("开始导出用户工作量报表为Excel");
        UserWorkloadReportResponseDTO dto = reportService.getUserWorkloadReport(request);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户工作量");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;
            sheet.createRow(rowNum++).createCell(0).setCellValue("用户工作量统计报表");
            sheet.getRow(0).getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
            rowNum++;
            UserWorkloadReportResponseDTO.Summary sum = dto.getSummary();
            if (sum != null) {
                addInfoRow(sheet, rowNum++, "统计用户数", nullToStr(sum.getUserCount()), dataStyle);
                addInfoRow(sheet, rowNum++, "任务总数", nullToStr(sum.getTotalIssues()), dataStyle);
                addInfoRow(sheet, rowNum++, "已完成数", nullToStr(sum.getCompletedIssues()), dataStyle);
                addInfoRow(sheet, rowNum++, "总工时(小时)", nullToStr(sum.getTotalHours()), dataStyle);
                rowNum++;
            }
            String[] headers = {"序号", "登录名", "姓名", "分配任务数", "已完成", "进行中", "待处理", "完成率%", "工时", "记录数"};
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            List<UserWorkloadReportResponseDTO.UserWorkloadItem> items = dto.getItems();
            if (items != null) {
                int idx = 1;
                for (UserWorkloadReportResponseDTO.UserWorkloadItem item : items) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(idx++);
                    row.createCell(1).setCellValue(nullToEmpty(item.getLogin()));
                    row.createCell(2).setCellValue(nullToEmpty(item.getDisplayName()));
                    row.createCell(3).setCellValue(item.getIssueCount() != null ? item.getIssueCount() : 0);
                    row.createCell(4).setCellValue(item.getCompletedCount() != null ? item.getCompletedCount() : 0);
                    row.createCell(5).setCellValue(item.getInProgressCount() != null ? item.getInProgressCount() : 0);
                    row.createCell(6).setCellValue(item.getPendingCount() != null ? item.getPendingCount() : 0);
                    row.createCell(7).setCellValue(item.getCompletionRate() != null ? item.getCompletionRate() : 0);
                    row.createCell(8).setCellValue(item.getTotalHours() != null ? item.getTotalHours() : 0);
                    row.createCell(9).setCellValue(item.getTimeEntryCount() != null ? item.getTimeEntryCount() : 0);
                    for (int i = 0; i < 10; i++) row.getCell(i).setCellStyle(dataStyle);
                }
            }
            autoSizeColumns(sheet, 10);
            workbook.write(out);
            log.info("用户工作量报表Excel导出完成");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出用户工作量报表Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }

    public byte[] exportUserWorkloadReportToCSV(UserWorkloadReportRequestDTO request) {
        log.info("开始导出用户工作量报表为CSV");
        UserWorkloadReportResponseDTO dto = reportService.getUserWorkloadReport(request);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     "登录名", "姓名", "分配任务数", "已完成", "进行中", "待处理", "完成率%", "工时", "记录数"))) {
            writeBOM(out);
            List<UserWorkloadReportResponseDTO.UserWorkloadItem> items = dto.getItems();
            if (items != null) {
                for (UserWorkloadReportResponseDTO.UserWorkloadItem item : items) {
                    printer.printRecord(
                            nullToEmpty(item.getLogin()),
                            nullToEmpty(item.getDisplayName()),
                            item.getIssueCount() != null ? item.getIssueCount() : 0,
                            item.getCompletedCount() != null ? item.getCompletedCount() : 0,
                            item.getInProgressCount() != null ? item.getInProgressCount() : 0,
                            item.getPendingCount() != null ? item.getPendingCount() : 0,
                            item.getCompletionRate() != null ? item.getCompletionRate() : 0,
                            item.getTotalHours() != null ? item.getTotalHours() : 0,
                            item.getTimeEntryCount() != null ? item.getTimeEntryCount() : 0
                    );
                }
            }
            printer.flush();
            log.info("用户工作量报表CSV导出完成");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出用户工作量报表CSV失败", e);
            throw new BusinessException("导出CSV失败: " + e.getMessage());
        }
    }

    // ==================== 燃尽图报表 ====================

    public byte[] exportBurndownReportToExcel(BurndownReportRequestDTO request) {
        log.info("开始导出燃尽图报表为Excel: projectId={}, versionId={}", request.getProjectId(), request.getVersionId());
        BurndownReportResponseDTO dto = reportService.getBurndownReport(request);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("燃尽图");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;
            sheet.createRow(rowNum++).createCell(0).setCellValue("燃尽图报表");
            sheet.getRow(0).getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
            rowNum++;
            addInfoRow(sheet, rowNum++, "项目", nullToEmpty(dto.getProjectName()), dataStyle);
            if (dto.getVersionName() != null) addInfoRow(sheet, rowNum++, "版本", dto.getVersionName(), dataStyle);
            addInfoRow(sheet, rowNum++, "总任务数", nullToStr(dto.getTotalIssues()), dataStyle);
            addInfoRow(sheet, rowNum++, "已完成", nullToStr(dto.getCompletedIssues()), dataStyle);
            addInfoRow(sheet, rowNum++, "剩余", nullToStr(dto.getRemainingIssues()), dataStyle);
            addInfoRow(sheet, rowNum++, "日期范围", (dto.getStartDate() != null ? dto.getStartDate().format(DATE_FORMAT) : "") + " ~ " + (dto.getEndDate() != null ? dto.getEndDate().format(DATE_FORMAT) : ""), dataStyle);
            rowNum++;

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"日期", "理想剩余", "实际剩余", "当日完成"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            List<BurndownReportResponseDTO.BurndownPoint> actualLine = dto.getActualLine();
            List<BurndownReportResponseDTO.BurndownPoint> idealLine = dto.getIdealLine();
            if (actualLine != null) {
                for (int i = 0; i < actualLine.size(); i++) {
                    BurndownReportResponseDTO.BurndownPoint actual = actualLine.get(i);
                    BurndownReportResponseDTO.BurndownPoint ideal = idealLine != null && i < idealLine.size() ? idealLine.get(i) : null;
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(actual.getDate() != null ? actual.getDate().format(DATE_FORMAT) : "");
                    row.createCell(1).setCellValue(ideal != null && ideal.getRemaining() != null ? ideal.getRemaining() : 0);
                    row.createCell(2).setCellValue(actual.getRemaining() != null ? actual.getRemaining() : 0);
                    row.createCell(3).setCellValue(actual.getCompletedThatDay() != null ? actual.getCompletedThatDay() : 0);
                    for (int j = 0; j < 4; j++) row.getCell(j).setCellStyle(dataStyle);
                }
            }
            autoSizeColumns(sheet, 4);
            workbook.write(out);
            log.info("燃尽图报表Excel导出完成");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出燃尽图报表Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        }
    }

    public byte[] exportBurndownReportToCSV(BurndownReportRequestDTO request) {
        log.info("开始导出燃尽图报表为CSV: projectId={}, versionId={}", request.getProjectId(), request.getVersionId());
        BurndownReportResponseDTO dto = reportService.getBurndownReport(request);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("日期", "理想剩余", "实际剩余", "当日完成"))) {
            writeBOM(out);
            List<BurndownReportResponseDTO.BurndownPoint> actualLine = dto.getActualLine();
            List<BurndownReportResponseDTO.BurndownPoint> idealLine = dto.getIdealLine();
            if (actualLine != null) {
                for (int i = 0; i < actualLine.size(); i++) {
                    BurndownReportResponseDTO.BurndownPoint actual = actualLine.get(i);
                    BurndownReportResponseDTO.BurndownPoint ideal = idealLine != null && i < idealLine.size() ? idealLine.get(i) : null;
                    printer.printRecord(
                            actual.getDate() != null ? actual.getDate().format(DATE_FORMAT) : "",
                            ideal != null && ideal.getRemaining() != null ? ideal.getRemaining() : 0,
                            actual.getRemaining() != null ? actual.getRemaining() : 0,
                            actual.getCompletedThatDay() != null ? actual.getCompletedThatDay() : 0
                    );
                }
            }
            printer.flush();
            log.info("燃尽图报表CSV导出完成");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出燃尽图报表CSV失败", e);
            throw new BusinessException("导出CSV失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助 ====================

    private void writeGroupSheet(Sheet sheet, List<TimeEntryReportResponseDTO.GroupItem> items, String nameColumn, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {nameColumn, "工时", "记录数", "占比%"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        for (TimeEntryReportResponseDTO.GroupItem item : items) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nullToEmpty(item.getGroupName()));
            row.createCell(1).setCellValue(item.getHours() != null ? item.getHours() : 0);
            row.createCell(2).setCellValue(item.getCount() != null ? item.getCount() : 0);
            row.createCell(3).setCellValue(item.getPercentage() != null ? item.getPercentage() : 0);
            for (int i = 0; i < 4; i++) row.getCell(i).setCellStyle(dataStyle);
        }
        autoSizeColumns(sheet, 4);
    }

    private static void writeBOM(ByteArrayOutputStream out) {
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String nullToStr(Object o) {
        return o == null ? "" : Objects.toString(o);
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
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

    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static void addInfoRow(Sheet sheet, int rowNum, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(style);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
        valueCell.setCellStyle(style);
    }

    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Exception ignored) {
            }
        }
    }
}
