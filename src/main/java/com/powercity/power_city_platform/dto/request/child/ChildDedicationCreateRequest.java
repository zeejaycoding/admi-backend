package com.powercity.power_city_platform.dto.request.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ChildDedicationCreateRequest {

    @NotBlank(message = "Child's name is required")
    private String childName;

    @NotNull(message = "Dedication date is required")
    private LocalDate dedicationDate;

    @NotBlank(message = "Parent/guardian name(s) is required")
    private String parentName;

    @NotBlank(message = "Campus is required")
    private String campus;

    @NotBlank(message = "Officiating minister is required")
    private String minister;

    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }

    public LocalDate getDedicationDate() { return dedicationDate; }
    public void setDedicationDate(LocalDate dedicationDate) { this.dedicationDate = dedicationDate; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getMinister() { return minister; }
    public void setMinister(String minister) { this.minister = minister; }
}
