package com.powercity.power_city_platform.entity;

import com.powercity.power_city_platform.enums.Currency;
import com.powercity.power_city_platform.enums.ProductType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    @NotNull(message = "Product type is required")
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "quantity", nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false)
    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Subtotal must be greater than 0")
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency = Currency.USD;

    @Column(name = "discount_amount")
    @DecimalMin(value = "0.0", message = "Discount amount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "notes")
    @Size(max = 500, message = "Item notes must be less than 500 characters")
    private String notes;

    public OrderItem() {}

    public OrderItem(Order order, Book book, Integer quantity, BigDecimal unitPrice, Currency currency) {
        this.order = order;
        this.productType = ProductType.BOOK;
        this.book = book;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currency = currency;
        calculateSubtotal();
    }

    public OrderItem(Order order, Course course, Integer quantity, BigDecimal unitPrice, Currency currency) {
        this.order = order;
        this.productType = ProductType.COURSE;
        this.course = course;
        this.quantity = 1;
        this.unitPrice = unitPrice;
        this.currency = currency;
        calculateSubtotal();
    }

    public void calculateSubtotal() {
        this.subtotal = unitPrice.multiply(new BigDecimal(quantity)).subtract(discountAmount);
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        calculateSubtotal();
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
