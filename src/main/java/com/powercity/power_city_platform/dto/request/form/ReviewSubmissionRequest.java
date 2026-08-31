package com.powercity.power_city_platform.dto.request.form;

public class ReviewSubmissionRequest {

    private String reviewerNotes;

    public ReviewSubmissionRequest() {
    }

    public ReviewSubmissionRequest(String reviewerNotes) {
        this.reviewerNotes = reviewerNotes;
    }

    public String getReviewerNotes() {
        return reviewerNotes;
    }

    public void setReviewerNotes(String reviewerNotes) {
        this.reviewerNotes = reviewerNotes;
    }
}
