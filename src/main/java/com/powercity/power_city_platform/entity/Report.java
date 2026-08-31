package com.powercity.power_city_platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "national_leader")
    private String nationalLeader;

    @Column(name = "campus", nullable = false)
    private String campus;

    @Column(name = "coordinator", nullable = false)
    private String coordinator;

    @Column(name = "zonal_leader")
    private String zonalLeader;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // Income fields
    @Column(name = "partnership")
    private Double partnership = 0.0;

    @Column(name = "papa_honour")
    private Double papaHonour = 0.0;

    @Column(name = "offerings")
    private Double offerings = 0.0;

    @Column(name = "income")
    private Double income = 0.0;

    @Column(name = "expenditure")
    private Double expenditure = 0.0;

    @Column(name = "balance")
    private Double balance = 0.0;

    @Column(name = "status", nullable = false)
    private String status = "Pending";

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<ReportExpense> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<ReportReceipt> receipts = new ArrayList<>();
}
