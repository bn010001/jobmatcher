package com.jobmatcher.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CvParseTextRequest {

    @NotBlank
    private String text;

    public CvParseTextRequest() {}

    public CvParseTextRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
