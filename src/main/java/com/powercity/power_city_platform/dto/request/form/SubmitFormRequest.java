package com.powercity.power_city_platform.dto.request.form;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public class SubmitFormRequest {

    @NotNull(message = "Submission data is required")
    private JsonNode submissionData;

    public SubmitFormRequest() {
    }

    public SubmitFormRequest(JsonNode submissionData) {
        this.submissionData = submissionData;
    }

    public JsonNode getSubmissionData() {
        return submissionData;
    }

    public void setSubmissionData(JsonNode submissionData) {
        this.submissionData = submissionData;
    }
}
