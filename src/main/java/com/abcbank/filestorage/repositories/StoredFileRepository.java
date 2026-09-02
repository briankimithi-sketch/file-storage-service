package com.abcbank.filestorage.repositories;

import com.abcbank.filestorage.entities.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    /**
     * Find a file record by its original filename.
     * Useful for legacy lookups, but may return duplicates.
     */
    Optional<StoredFile> findByOriginalName(String originalName);

    /**
     * Find a file record by its UUID-based filename.
     * This is guaranteed to be unique and is the preferred lookup.
     */
    Optional<StoredFile> findByUuidFilename(String uuidFilename);
}
