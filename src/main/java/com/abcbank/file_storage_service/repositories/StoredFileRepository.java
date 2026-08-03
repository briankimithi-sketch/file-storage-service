package com.abcbank.file_storage_service.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import com.abcbank.file_storage_service.entities.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
