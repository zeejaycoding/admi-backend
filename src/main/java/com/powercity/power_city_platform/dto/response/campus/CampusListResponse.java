package com.powercity.power_city_platform.dto.response.campus;

import java.util.List;

public class CampusListResponse {

    private List<CampusSummaryResponse> campuses;
    private Integer totalPages;
    private Long totalElements;
    private Integer currentPage;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;

    public CampusListResponse() {}

    public CampusListResponse(List<CampusSummaryResponse> campuses, Integer totalPages, Long totalElements, 
                             Integer currentPage, Integer pageSize, Boolean hasNext, Boolean hasPrevious) {
        this.campuses = campuses;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public List<CampusSummaryResponse> getCampuses() {
        return campuses;
    }

    public void setCampuses(List<CampusSummaryResponse> campuses) {
        this.campuses = campuses;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
} 