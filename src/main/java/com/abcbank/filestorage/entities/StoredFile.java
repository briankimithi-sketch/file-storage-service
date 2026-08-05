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
    private Long id; // JPA manages this, so no @NonNull

    @Column(nullable = false)
    private String originalName; // required

    @Column(nullable = false)
    private String contentType;  // required

    private long size; // primitive avoids null issues

    @Column(nullable = false)
    private String filePath; // required

    private LocalDateTime createdOn = LocalDateTime.now();
}
