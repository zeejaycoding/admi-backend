package com.powercity.power_city_platform.dto.request.travel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class TravelFormCreateRequest {

    @NotBlank(message = "Country is required")
    private String country;

    @NotNull(message = "Travel date is required")
    private LocalDate travelDate;

    @NotNull(message = "Return date is required")
    private LocalDate returnDate;

    @NotNull(message = "Days of stay is required")
    @Min(value = 1, message = "Days must be at least 1")
    private Integer days;

    @NotBlank(message = "Reason for travelling is required")
    private String reason;

    private String campus;

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
}
