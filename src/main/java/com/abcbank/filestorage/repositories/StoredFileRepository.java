package com.abcbank.filestorage.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import com.abcbank.filestorage.entities.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
