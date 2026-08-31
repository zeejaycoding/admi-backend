package com.powercity.power_city_platform.dto.request.campus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class CampusSearchRequest {

    private String searchTerm;

    @Pattern(regexp = "^(UK|South Africa|USA|Ghana|Nigeria)$", message = "Region must be one of: UK, South Africa, USA, Ghana, Nigeria")
    private String region;

    private String city;

    private String country;

    private Boolean isActive;

    private Boolean isFeatured;

    @Min(value = 0, message = "Page number must be non-negative")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be positive")
    private Integer size = 10;

    @Pattern(regexp = "^(name|region|city|createdAt|lastActivity|totalMembers|totalEvents|totalDonations)$", 
             message = "Sort by must be one of: name, region, city, createdAt, lastActivity, totalMembers, totalEvents, totalDonations")
    private String sortBy = "name";

    @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
    private String sortDirection = "asc";

    public CampusSearchRequest() {}

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
} 