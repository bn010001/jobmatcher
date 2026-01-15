package com.jobmatcher.api.service.storage;

import org.springframework.core.io.Resource;

public interface StorageService {
    String save(byte[] bytes, String storedFilename);
    Resource loadAsResource(String storedFilename);
    void delete(String storedFilename);
}

