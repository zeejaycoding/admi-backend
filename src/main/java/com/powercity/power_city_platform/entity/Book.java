package com.powercity.power_city_platform.entity;

import com.powercity.power_city_platform.enums.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Entity
@Table(name = "books")
public class Book extends BaseEntity {

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Book title is required")
    @Size(max = 255, message = "Book title must be less than 255 characters")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Book description must be less than 2000 characters")
    private String description;

    @Column(name = "base_price", nullable = false)
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal basePrice;

    @Column(name = "ngn_price")
    @DecimalMin(value = "0.0", inclusive = false, message = "NGN price must be greater than 0")
    private BigDecimal ngnPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false)
    private Currency baseCurrency = Currency.USD;

    @Column(name = "language")
    @Size(max = 10, message = "Language code must be less than 10 characters")
    private String language = "en";

    @Column(name = "publisher")
    @Size(max = 255, message = "Publisher must be less than 255 characters")
    private String publisher;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "cover_image_key")
    private String coverImageKey;

    @Column(name = "back_cover_image_url")
    private String backCoverImageUrl;

    @Column(name = "back_cover_image_key")
    private String backCoverImageKey;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "pdf_key")
    private String pdfKey;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "sales_count")
    private Integer salesCount = 0;

    public Book() {}

    public Book(String title, BigDecimal basePrice, Currency baseCurrency) {
        this.title = title;
        this.basePrice = basePrice;
        this.baseCurrency = baseCurrency;
        this.isActive = true;
        this.downloadCount = 0;
        this.viewCount = 0;
        this.salesCount = 0;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public void incrementSalesCount() {
        this.salesCount++;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(Currency baseCurrency) {
        this.baseCurrency = baseCurrency;
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

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public void setCoverImageKey(String coverImageKey) {
        this.coverImageKey = coverImageKey;
    }

    public String getBackCoverImageUrl() {
        return backCoverImageUrl;
    }

    public void setBackCoverImageUrl(String backCoverImageUrl) {
        this.backCoverImageUrl = backCoverImageUrl;
    }

    public String getBackCoverImageKey() {
        return backCoverImageKey;
    }

    public void setBackCoverImageKey(String backCoverImageKey) {
        this.backCoverImageKey = backCoverImageKey;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getPdfKey() {
        return pdfKey;
    }

    public void setPdfKey(String pdfKey) {
        this.pdfKey = pdfKey;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Integer salesCount) {
        this.salesCount = salesCount;
    }

    public BigDecimal getNgnPrice() {
        return ngnPrice;
    }

    public void setNgnPrice(BigDecimal ngnPrice) {
        this.ngnPrice = ngnPrice;
    }
}
