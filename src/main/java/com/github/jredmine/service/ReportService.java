package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.report.UserWorkloadReportRequestDTO;
import com.github.jredmine.dto.response.report.UserWorkloadReportResponseDTO;
import com.github.jredmine.entity.Issue;
import com.github.jredmine.entity.IssueStatus;
import com.github.jredmine.entity.Member;
import com.github.jredmine.entity.Project;
import com.github.jredmine.entity.TimeEntry;
import com.github.jredmine.entity.User;
import com.github.jredmine.enums.ResultCode;
import com.github.jredmine.exception.BusinessException;
import com.github.jredmine.mapper.TimeEntryMapper;
import com.github.jredmine.mapper.issue.IssueMapper;
import com.github.jredmine.mapper.project.MemberMapper;
import com.github.jredmine.mapper.project.ProjectMapper;
import com.github.jredmine.mapper.user.UserMapper;
import com.github.jredmine.mapper.workflow.IssueStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统计报表服务（用户工作量等）
 *
 * @author panfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserMapper userMapper;
    private final IssueMapper issueMapper;
    private final TimeEntryMapper timeEntryMapper;
    private final MemberMapper memberMapper;
    private final ProjectMapper projectMapper;
    private final IssueStatusMapper issueStatusMapper;

    /**
     * 用户工作量统计报表
     */
    public UserWorkloadReportResponseDTO getUserWorkloadReport(UserWorkloadReportRequestDTO request) {
        MDC.put("operation", "get_user_workload_report");

        try {
            if (request.getProjectId() != null) {
                if (projectMapper.selectById(request.getProjectId()) == null) {
                    throw new BusinessException(ResultCode.PROJECT_NOT_FOUND);
                }
            }

            Set<Long> userIds = collectUserIds(request);
            if (userIds.isEmpty()) {
                return emptyUserWorkloadReport(request);
            }

            List<IssueStatus> allStatuses = issueStatusMapper.selectList(null);
            Map<Integer, Boolean> statusClosedMap = allStatuses.stream()
                    .collect(Collectors.toMap(IssueStatus::getId, s -> Boolean.TRUE.equals(s.getIsClosed()), (a, b) -> a));

            List<UserWorkloadReportResponseDTO.UserWorkloadItem> items = new ArrayList<>();
            for (Long userId : userIds) {
                User user = userMapper.selectById(userId);
                if (user == null) continue;

                LambdaQueryWrapper<Issue> issueWrapper = new LambdaQueryWrapper<>();
                issueWrapper.eq(Issue::getAssignedToId, userId);
                if (request.getProjectId() != null) {
                    issueWrapper.eq(Issue::getProjectId, request.getProjectId());
                }
                List<Issue> issues = issueMapper.selectList(issueWrapper);

                long issueCount = issues.size();
                long completedCount = issues.stream()
                        .filter(i -> i.getStatusId() != null && Boolean.TRUE.equals(statusClosedMap.get(i.getStatusId())))
                        .count();
                long inProgressCount = issues.stream()
                        .filter(i -> i.getStatusId() != null && !Boolean.TRUE.equals(statusClosedMap.get(i.getStatusId())))
                        .filter(i -> i.getDoneRatio() != null && i.getDoneRatio() > 0 && i.getDoneRatio() < 100)
                        .count();
                long pendingCount = issueCount - completedCount - inProgressCount;
                if (pendingCount < 0) pendingCount = 0;

                double completionRate = issueCount > 0 ? (completedCount * 100.0 / issueCount) : 0.0;

                LambdaQueryWrapper<TimeEntry> timeWrapper = new LambdaQueryWrapper<>();
                timeWrapper.eq(TimeEntry::getUserId, userId).isNotNull(TimeEntry::getHours);
                if (request.getProjectId() != null) {
                    timeWrapper.eq(TimeEntry::getProjectId, request.getProjectId());
                }
                addTimeEntryDateConditions(timeWrapper, request);
                List<TimeEntry> timeEntries = timeEntryMapper.selectList(timeWrapper);

                double totalHours = timeEntries.stream()
                        .map(e -> e.getHours() != null ? e.getHours().doubleValue() : 0.0)
                        .reduce(0.0, Double::sum);
                long timeEntryCount = timeEntries.size();

                String displayName = (user.getFirstname() != null ? user.getFirstname() : "") + " " + (user.getLastname() != null ? user.getLastname() : "").trim();
                if (displayName.trim().isEmpty()) displayName = user.getLogin() != null ? user.getLogin() : "";

                items.add(UserWorkloadReportResponseDTO.UserWorkloadItem.builder()
                        .userId(userId)
                        .login(user.getLogin())
                        .displayName(displayName.trim().isEmpty() ? user.getLogin() : displayName.trim())
                        .issueCount(issueCount)
                        .completedCount(completedCount)
                        .inProgressCount(inProgressCount)
                        .pendingCount(pendingCount)
                        .completionRate(round2(completionRate))
                        .totalHours(round2(totalHours))
                        .timeEntryCount(timeEntryCount)
                        .build());
            }

            items.sort((a, b) -> Double.compare(b.getTotalHours() != null ? b.getTotalHours() : 0, a.getTotalHours() != null ? a.getTotalHours() : 0));

            long totalIssues = items.stream().mapToLong(i -> i.getIssueCount() != null ? i.getIssueCount() : 0).sum();
            long completedIssues = items.stream().mapToLong(i -> i.getCompletedCount() != null ? i.getCompletedCount() : 0).sum();
            double totalHours = items.stream().mapToDouble(i -> i.getTotalHours() != null ? i.getTotalHours() : 0).sum();
            long userCount = items.size();
            double averageHoursPerUser = userCount > 0 ? totalHours / userCount : 0;

            UserWorkloadReportResponseDTO.Summary summary = UserWorkloadReportResponseDTO.Summary.builder()
                    .userCount(userCount)
                    .totalIssues(totalIssues)
                    .completedIssues(completedIssues)
                    .totalHours(round2(totalHours))
                    .averageHoursPerUser(round2(averageHoursPerUser))
                    .build();

            log.info("用户工作量报表生成完成: userCount={}, totalIssues={}, totalHours={}", userCount, totalIssues, totalHours);

            return UserWorkloadReportResponseDTO.builder()
                    .summary(summary)
                    .items(items)
                    .queryDescription(buildQueryDescription(request))
                    .build();
        } finally {
            MDC.remove("operation");
        }
    }

    private Set<Long> collectUserIds(UserWorkloadReportRequestDTO request) {
        Set<Long> userIds = new HashSet<>();
        if (request.getProjectId() != null) {
            LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(Member::getProjectId, request.getProjectId());
            List<Member> members = memberMapper.selectList(memberWrapper);
            members.forEach(m -> userIds.add(m.getUserId()));
        } else {
            LambdaQueryWrapper<Issue> issueWrapper = new LambdaQueryWrapper<>();
            issueWrapper.isNotNull(Issue::getAssignedToId);
            issueMapper.selectList(issueWrapper).forEach(i -> userIds.add(i.getAssignedToId()));
            LambdaQueryWrapper<TimeEntry> timeWrapper = new LambdaQueryWrapper<>();
            addTimeEntryDateConditions(timeWrapper, request);
            timeEntryMapper.selectList(timeWrapper).forEach(te -> userIds.add(te.getUserId()));
        }
        return userIds;
    }

    private void addTimeEntryDateConditions(LambdaQueryWrapper<TimeEntry> wrapper, UserWorkloadReportRequestDTO request) {
        if (request.getStartDate() != null) {
            wrapper.ge(TimeEntry::getSpentOn, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(TimeEntry::getSpentOn, request.getEndDate());
        }
        if (request.getYear() != null) {
            wrapper.eq(TimeEntry::getTyear, request.getYear());
        }
        if (request.getMonth() != null) {
            wrapper.eq(TimeEntry::getTmonth, request.getMonth());
        }
    }

    private String buildQueryDescription(UserWorkloadReportRequestDTO request) {
        if (request.getProjectId() != null) {
            Project project = projectMapper.selectById(request.getProjectId());
            String projectName = project != null ? project.getName() : "项目" + request.getProjectId();
            if (request.getYear() != null && request.getMonth() != null) {
                return projectName + " · " + request.getYear() + "年" + request.getMonth() + "月";
            }
            if (request.getStartDate() != null && request.getEndDate() != null) {
                return projectName + " · " + request.getStartDate() + " ~ " + request.getEndDate();
            }
            if (request.getYear() != null) {
                return projectName + " · " + request.getYear() + "年";
            }
            return projectName;
        }
        if (request.getYear() != null && request.getMonth() != null) {
            return request.getYear() + "年" + request.getMonth() + "月";
        }
        if (request.getStartDate() != null && request.getEndDate() != null) {
            return request.getStartDate() + " ~ " + request.getEndDate();
        }
        if (request.getYear() != null) {
            return request.getYear() + "年";
        }
        return "全部";
    }

    private UserWorkloadReportResponseDTO emptyUserWorkloadReport(UserWorkloadReportRequestDTO request) {
        return UserWorkloadReportResponseDTO.builder()
                .summary(UserWorkloadReportResponseDTO.Summary.builder()
                        .userCount(0L)
                        .totalIssues(0L)
                        .completedIssues(0L)
                        .totalHours(0.0)
                        .averageHoursPerUser(0.0)
                        .build())
                .items(Collections.emptyList())
                .queryDescription(buildQueryDescription(request))
                .build();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
