package com.abcbank.file_storage_service.services;

import com.abcbank.file_storage_service.entities.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StorageService {
    StoredFile store(MultipartFile file) throws IOException;
    Resource loadAsResource(Long fileId);
    void delete(Long fileId) throws IOException;
    List<StoredFile> findAll();
    StoredFile findById(Long fileId);
}
