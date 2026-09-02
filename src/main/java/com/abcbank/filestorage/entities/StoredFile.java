package com.abcbank.filestorage.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stored_files")
@Getter
@Setter
@NoArgsConstructor
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The original filename as uploaded by the user.
     */
    @Column(nullable = false)
    private String originalName;

    /**
     * A unique UUID-based filename used for storage and URL building.
     * This ensures no duplicate conflicts in the database.
     */
    @Column(nullable = false, unique = true)
    private String uuidFilename;

    /**
     * MIME type of the file (e.g., text/plain, image/png).
     */
    @Column(nullable = false)
    private String contentType;

    /**
     * File size in bytes.
     */
    private long size;

    /**
     * Absolute path on disk where the file is stored.
     */
    @Column(nullable = false)
    private String filePath;

    /**
     * Timestamp when the file was created.
     */
    private LocalDateTime createdOn = LocalDateTime.now();

    /**
     * Transient fields for URLs returned in API responses.
     * These are not persisted in the database.
     */
    @Transient
    private String downloadUrl;

    @Transient
    private String viewUrl;
}
