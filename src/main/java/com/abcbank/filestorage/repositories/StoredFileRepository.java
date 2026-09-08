package com.abcbank.filestorage.repositories;

import com.abcbank.filestorage.entities.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredFileRepository
        extends JpaRepository<StoredFile, Long> {

    Optional<StoredFile> findByOriginalName(String originalName);
}
