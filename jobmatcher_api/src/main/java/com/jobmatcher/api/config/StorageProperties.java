package com.jobmatcher.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobmatcher.storage")
public class StorageProperties {
    private String provider = "local";
    private Local local = new Local();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Local getLocal() { return local; }
    public void setLocal(Local local) { this.local = local; }

    public static class Local {
        private String rootDir = "./data/cv";
        public String getRootDir() { return rootDir; }
        public void setRootDir(String rootDir) { this.rootDir = rootDir; }
    }
}
