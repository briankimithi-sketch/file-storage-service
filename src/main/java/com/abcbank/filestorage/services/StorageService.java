package com.abcbank.filestorage.services;

import com.abcbank.filestorage.entities.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface StorageService {

    /**
     * Store a new file and persist its metadata.
     */
    StoredFile store(MultipartFile file) throws IOException;

    /**
     * Load a file as a Spring Resource for download/view.
     */
    Resource loadAsResource(StoredFile storedFile);

    /**
     * Delete a file by its original filename.
     */
    void deleteByFilename(String filename) throws IOException;

    /**
     * Find all stored files.
     */
    List<StoredFile> findAll();

    /**
     * Find a file by its original filename.
     */
    Optional<StoredFile> findByOriginalName(String filename);

    /**
     * Find a file by its original filename or throw if not found.
     */
    StoredFile findByOriginalNameOrThrow(String filename);
}
