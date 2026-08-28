package com.abcbank.filestorage.repositories;

import com.abcbank.filestorage.entities.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    /**
     * Find a file record by its original filename.
     * Spring Data JPA will generate the query automatically:
     * SELECT * FROM stored_files WHERE original_name = ?
     */
    Optional<StoredFile> findByOriginalName(String originalName);
}
