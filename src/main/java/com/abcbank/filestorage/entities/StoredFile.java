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

    @Column(nullable = false)
    private String originalName; 

    @Column(nullable = false)
    private String contentType;  

    private long size; 

    @Column(nullable = false)
    private String filePath; 

    private LocalDateTime createdOn = LocalDateTime.now();
}
