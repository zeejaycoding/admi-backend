package com.powercity.power_city_platform.dto.request.marriage;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class MarriageCertificateCreateRequest {

    @NotBlank(message = "Groom's full name is required")
    private String groomName;

    @NotBlank(message = "Groom's email is required")
    @Email(message = "Invalid groom email format")
    private String groomEmail;

    @NotBlank(message = "Bride's full name is required")
    private String brideName;

    @NotBlank(message = "Bride's email is required")
    @Email(message = "Invalid bride email format")
    private String brideEmail;

    @NotNull(message = "Date of marriage is required")
    private LocalDate marriageDate;

    @NotBlank(message = "Campus is required")
    private String campus;

    @NotBlank(message = "Officiating minister is required")
    private String minister;

    private String maidOfHonor;

    private String bestMan;

    private String subject;

    private String additionalMessage;

    public String getGroomName() { return groomName; }
    public void setGroomName(String groomName) { this.groomName = groomName; }

    public String getGroomEmail() { return groomEmail; }
    public void setGroomEmail(String groomEmail) { this.groomEmail = groomEmail; }

    public String getBrideName() { return brideName; }
    public void setBrideName(String brideName) { this.brideName = brideName; }

    public String getBrideEmail() { return brideEmail; }
    public void setBrideEmail(String brideEmail) { this.brideEmail = brideEmail; }

    public LocalDate getMarriageDate() { return marriageDate; }
    public void setMarriageDate(LocalDate marriageDate) { this.marriageDate = marriageDate; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getMinister() { return minister; }
    public void setMinister(String minister) { this.minister = minister; }

    public String getMaidOfHonor() { return maidOfHonor; }
    public void setMaidOfHonor(String maidOfHonor) { this.maidOfHonor = maidOfHonor; }

    public String getBestMan() { return bestMan; }
    public void setBestMan(String bestMan) { this.bestMan = bestMan; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getAdditionalMessage() { return additionalMessage; }
    public void setAdditionalMessage(String additionalMessage) { this.additionalMessage = additionalMessage; }
}
