package com.abcbank.filestorage.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "stored_files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stored_files_original_name",
                        columnNames = "original_name"
                )
        }
)
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "original_name",
            nullable = false,
            unique = true
    )
    private String originalName;

    @Column(
            name = "file_path",
            nullable = false
    )
    private String filePath;

    @Column(name = "content_type")
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Transient
    private String downloadUrl;

    @Transient
    private String viewUrl;

    public Long getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public void setViewUrl(String viewUrl) {
        this.viewUrl = viewUrl;
    }
}
