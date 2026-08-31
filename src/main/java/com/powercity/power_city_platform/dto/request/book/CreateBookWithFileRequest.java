package com.powercity.power_city_platform.dto.request.book;

import com.powercity.power_city_platform.enums.Currency;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public class CreateBookWithFileRequest {

    @NotBlank(message = "Book title is required")
    @Size(max = 255, message = "Book title must be less than 255 characters")
    private String title;

    @Size(max = 2000, message = "Book description must be less than 2000 characters")
    private String description;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "NGN price must be greater than 0")
    private BigDecimal ngnPrice;

    private Currency baseCurrency = Currency.USD;

    @Size(max = 10, message = "Language code must be less than 10 characters")
    private String language = "en";

    @Size(max = 255, message = "Publisher must be less than 255 characters")
    private String publisher;

    private Boolean isActive = true;

    private Boolean isFeatured = false;

    // Files to upload
    @NotNull(message = "PDF file is required")
    private MultipartFile pdfFile;

    private MultipartFile coverImageFile;

    private MultipartFile backCoverImageFile;

    // URL fields for processed images
    private String coverImageUrl;
    
    private String backCoverImageUrl;

    public CreateBookWithFileRequest() {}

    public CreateBookWithFileRequest(String title, BigDecimal basePrice, MultipartFile pdfFile) {
        this.title = title;
        this.basePrice = basePrice;
        this.pdfFile = pdfFile;
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

    public BigDecimal getNgnPrice() {
        return ngnPrice;
    }

    public void setNgnPrice(BigDecimal ngnPrice) {
        this.ngnPrice = ngnPrice;
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

    public String getBackCoverImageUrl() {
        return backCoverImageUrl;
    }

    public void setBackCoverImageUrl(String backCoverImageUrl) {
        this.backCoverImageUrl = backCoverImageUrl;
    }

    public MultipartFile getPdfFile() {
        return pdfFile;
    }

    public void setPdfFile(MultipartFile pdfFile) {
        this.pdfFile = pdfFile;
    }

    public MultipartFile getCoverImageFile() {
        return coverImageFile;
    }

    public void setCoverImageFile(MultipartFile coverImageFile) {
        this.coverImageFile = coverImageFile;
    }

    public MultipartFile getBackCoverImageFile() {
        return backCoverImageFile;
    }

    public void setBackCoverImageFile(MultipartFile backCoverImageFile) {
        this.backCoverImageFile = backCoverImageFile;
    }
}