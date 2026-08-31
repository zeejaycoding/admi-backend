package com.powercity.power_city_platform.service;

import com.powercity.power_city_platform.entity.*;
import com.powercity.power_city_platform.exception.ResourceNotFoundException;
import com.powercity.power_city_platform.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final S3FileService s3FileService;
    private final EntityManager entityManager;

    private boolean isAdminOrSuperAdmin(User user) {
        return user.getUserRoles().stream()
                .anyMatch(ur -> ur.getIsActive() && (
                        ur.getRole().getName().equals("ADMIN") ||
                        ur.getRole().getName().equals("SUPER_ADMIN")));
    }

    private boolean isCoordinator(User user) {
        return user.getUserRoles().stream()
                .anyMatch(ur -> ur.getIsActive() && ur.getRole().getName().equals("COORDINATOR"));
    }

    public Report createReport(
            LocalDate date,
            String country,
            String nationalLeader,
            String campus,
            String coordinator,
            String zonalLeader,
            String summary,
            Double partnership,
            Double papaHonour,
            Double offerings,
            List<Map<String, Object>> rawExpenses,
            List<MultipartFile> files,
            User currentUser) {

        double totalIncome = (partnership != null ? partnership : 0.0) +
                (papaHonour != null ? papaHonour : 0.0) +
                (offerings != null ? offerings : 0.0);

        double totalExpenditure = 0.0;
        List<ReportExpense> expenses = new ArrayList<>();

        Report report = Report.builder()
                .date(date)
                .country(country)
                .nationalLeader(nationalLeader)
                .campus(campus)
                .coordinator(coordinator)
                .zonalLeader(zonalLeader)
                .summary(summary)
                .partnership(partnership != null ? partnership : 0.0)
                .papaHonour(papaHonour != null ? papaHonour : 0.0)
                .offerings(offerings != null ? offerings : 0.0)
                .income(totalIncome)
                .status("Pending")
                .expenses(expenses)
                .receipts(new ArrayList<>())
                .build();

        report.setCreatedBy(currentUser.getId());
        report.setUpdatedBy(currentUser.getId());

        if (rawExpenses != null) {
            for (Map<String, Object> expenseMap : rawExpenses) {
                LocalDate expDate = LocalDate.parse(expenseMap.get("date").toString());
                String desc = expenseMap.get("description").toString();
                double amount = Double.parseDouble(expenseMap.get("amount").toString());
                totalExpenditure += amount;

                expenses.add(ReportExpense.builder()
                        .date(expDate)
                        .description(desc)
                        .amount(amount)
                        .report(report)
                        .build());
            }
        }

        report.setExpenditure(totalExpenditure);
        report.setBalance(totalIncome - totalExpenditure);

        // Save report to generate ID
        Report savedReport = reportRepository.save(report);

        // Save files
        if (files != null && !files.isEmpty()) {
            List<ReportReceipt> receipts = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String s3Key = s3FileService.uploadDocument("report", file);
                String fileUrl = s3FileService.getPublicUrl(s3Key);
                receipts.add(ReportReceipt.builder()
                        .s3Key(s3Key)
                        .fileUrl(fileUrl)
                        .fileName(file.getOriginalFilename())
                        .report(savedReport)
                        .build());
            }
            savedReport.getReceipts().addAll(receipts);
            savedReport = reportRepository.save(savedReport);
        }

        return savedReport;
    }

    public List<Report> getAllReports(User currentUser) {
        List<Report> reports;
        if (isAdminOrSuperAdmin(currentUser)) {
            reports = reportRepository.findAll();
        } else if (isCoordinator(currentUser)) {
            reports = reportRepository.findByCreatedBy(currentUser.getId());
        } else {
            return Collections.emptyList();
        }
        // Initialize lazy collections within the transaction so Jackson can serialize
        for (Report r : reports) {
            Hibernate.initialize(r.getExpenses());
            Hibernate.initialize(r.getReceipts());
        }
        return reports;
    }

    public Report getReportById(Long id, User currentUser) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (!isAdminOrSuperAdmin(currentUser)) {
            if (!report.getCreatedBy().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized access to this report");
            }
        }
        Hibernate.initialize(report.getExpenses());
        Hibernate.initialize(report.getReceipts());
        return report;
    }

    public Report updateReportStatus(Long id, String status, User currentUser) {
        if (!isAdminOrSuperAdmin(currentUser)) {
            throw new RuntimeException("Only admins can approve/reject reports");
        }

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(status);
        report.setUpdatedBy(currentUser.getId());
        return reportRepository.save(report);
    }

    public void deleteReport(Long id, User currentUser) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (!isAdminOrSuperAdmin(currentUser)) {
            if (!report.getCreatedBy().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized access to delete this report");
            }
        }
        reportRepository.delete(report);
    }

    public Map<String, Object> getReportAnalytics(User currentUser) {
        List<Report> reports = getAllReports(currentUser);
        double totalIncome = 0.0;
        double totalExpenditure = 0.0;

        for (Report r : reports) {
            totalIncome += r.getIncome() != null ? r.getIncome() : 0.0;
            totalExpenditure += r.getExpenditure() != null ? r.getExpenditure() : 0.0;
        }

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalIncome", totalIncome);
        analytics.put("totalExpenditure", totalExpenditure);
        analytics.put("totalBalance", totalIncome - totalExpenditure);
        analytics.put("totalReports", reports.size());
        analytics.put("approvedReports", reports.stream().filter(r -> "Approved".equalsIgnoreCase(r.getStatus())).count());
        analytics.put("pendingReports", reports.stream().filter(r -> "Pending".equalsIgnoreCase(r.getStatus())).count());

        return analytics;
    }
}
