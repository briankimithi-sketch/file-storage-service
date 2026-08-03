package com.abcbank.file_storage_service.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stored_files")
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;
    private String contentType;
    private long size;
    private String filePath;
    private LocalDateTime createdOn = LocalDateTime.now();

    // --- Getters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getFilePath() {
        return filePath;
    }

        public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    // --- Setters ---
      public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }
}
