package com.jobmatcher.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "jobmatcher.cv")
public class CvProperties {
    private List<String> allowedContentTypes = new ArrayList<>();
    private long maxSizeBytes = 10 * 1024 * 1024;

    public List<String> getAllowedContentTypes() { return allowedContentTypes; }
    public void setAllowedContentTypes(List<String> allowedContentTypes) { this.allowedContentTypes = allowedContentTypes; }

    public long getMaxSizeBytes() { return maxSizeBytes; }
    public void setMaxSizeBytes(long maxSizeBytes) { this.maxSizeBytes = maxSizeBytes; }
}

