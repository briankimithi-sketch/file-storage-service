package com.abcbank.filestorage.services;

import com.abcbank.filestorage.entities.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StorageService {
    StoredFile store(MultipartFile file) throws IOException;
    Resource loadAsResource(StoredFile storedFile);
    void delete(Long fileId) throws IOException;
    List<StoredFile> findAll();
    StoredFile findById(Long fileId);
}
