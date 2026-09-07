package com.abcbank.filestorage.services;

import com.abcbank.filestorage.entities.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface StorageService {


    StoredFile store(MultipartFile file) throws IOException;


    Resource loadAsResource(StoredFile storedFile);


    void deleteByFilename(String filename) throws IOException;


    List<StoredFile> findAll();

    Optional<StoredFile> findByOriginalName(String filename);


    StoredFile findByOriginalNameOrThrow(String filename);
}


