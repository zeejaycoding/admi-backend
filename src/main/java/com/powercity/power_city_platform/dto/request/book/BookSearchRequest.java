package com.powercity.power_city_platform.dto.request.book;

import java.math.BigDecimal;

public class BookSearchRequest {

    private String searchTerm;

    private String language = "en";

    private String publisher;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Boolean isFeatured;

    private Boolean isActive = true;

    private String sortBy = "createdAt"; // createdAt, title, price, salesCount, viewCount

    private String sortDirection = "DESC"; // ASC, DESC

    private Integer page = 0;

    private Integer size = 20;

    public BookSearchRequest() {}

    public boolean hasSearchCriteria() {
        return searchTerm != null || minPrice != null || maxPrice != null ||
               isFeatured != null || language != null || publisher != null;
    }

    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
} 