package com.jobmatcher.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import java.util.Map;

public class CvParseResponse {

    private String text;
    private Map<String, Object> sections;
    private List<Double> embedding;

    @JsonAlias({"model_used", "modelUsed"})
    private String modelUsed;

    public CvParseResponse() {}

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Map<String, Object> getSections() { return sections; }
    public void setSections(Map<String, Object> sections) { this.sections = sections; }

    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
}
