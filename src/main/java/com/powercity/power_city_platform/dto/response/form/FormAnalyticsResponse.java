package com.powercity.power_city_platform.dto.response.form;

import java.util.Map;

public class FormAnalyticsResponse {

    private Long formId;
    private String formTitle;
    private Long totalSubmissions;
    private Long submissionsToday;
    private Long submissionsThisWeek;
    private Long submissionsThisMonth;
    private Map<String, Object> additionalMetrics;

    public FormAnalyticsResponse() {
    }

    public FormAnalyticsResponse(Long formId, String formTitle, Long totalSubmissions,
                                Long submissionsToday, Long submissionsThisWeek,
                                Long submissionsThisMonth, Map<String, Object> additionalMetrics) {
        this.formId = formId;
        this.formTitle = formTitle;
        this.totalSubmissions = totalSubmissions;
        this.submissionsToday = submissionsToday;
        this.submissionsThisWeek = submissionsThisWeek;
        this.submissionsThisMonth = submissionsThisMonth;
        this.additionalMetrics = additionalMetrics;
    }

    public Long getFormId() {
        return formId;
    }

    public void setFormId(Long formId) {
        this.formId = formId;
    }

    public String getFormTitle() {
        return formTitle;
    }

    public void setFormTitle(String formTitle) {
        this.formTitle = formTitle;
    }

    public Long getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(Long totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public Long getSubmissionsToday() {
        return submissionsToday;
    }

    public void setSubmissionsToday(Long submissionsToday) {
        this.submissionsToday = submissionsToday;
    }

    public Long getSubmissionsThisWeek() {
        return submissionsThisWeek;
    }

    public void setSubmissionsThisWeek(Long submissionsThisWeek) {
        this.submissionsThisWeek = submissionsThisWeek;
    }

    public Long getSubmissionsThisMonth() {
        return submissionsThisMonth;
    }

    public void setSubmissionsThisMonth(Long submissionsThisMonth) {
        this.submissionsThisMonth = submissionsThisMonth;
    }

    public Map<String, Object> getAdditionalMetrics() {
        return additionalMetrics;
    }

    public void setAdditionalMetrics(Map<String, Object> additionalMetrics) {
        this.additionalMetrics = additionalMetrics;
    }
}
