package com.abcbank.filestorage.controllers;

import com.abcbank.filestorage.entities.StoredFile;
import com.abcbank.filestorage.services.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CrossOrigin(
        origins = "http://localhost:5173",
        exposedHeaders = HttpHeaders.CONTENT_DISPOSITION
)
@RestController
@RequestMapping("/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Get all stored files.
     */
    @GetMapping
    public ResponseEntity<List<StoredFile>> findAll() {
        return ResponseEntity.ok(
                storageService.findAll()
        );
    }

    /**
     * Upload a file.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        StoredFile stored =
                storageService.store(file);

        Map<String, Object> response = Map.of(
                "originalName",
                stored.getOriginalName(),

                "size",
                stored.getSize(),

                "downloadUrl",
                stored.getDownloadUrl(),

                "viewUrl",
                stored.getViewUrl()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Download a file.
     */
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> download(
            @PathVariable String filename
    ) {

        StoredFile stored =
                storageService.findByOriginalNameOrThrow(
                        filename
                );

        Resource resource =
                storageService.loadAsResource(stored);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                stored.getOriginalName() +
                                "\""
                )
                .contentType(
                        getContentType(stored)
                )
                .body(resource);
    }

    /**
     * View a file in the browser.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> view(
            @PathVariable String filename
    ) {

        StoredFile stored =
                storageService.findByOriginalNameOrThrow(
                        filename
                );

        Resource resource =
                storageService.loadAsResource(stored);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                stored.getOriginalName() +
                                "\""
                )
                .contentType(
                        getContentType(stored)
                )
                .body(resource);
    }

    /**
     * Delete a file.
     */
    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<Void> delete(
            @PathVariable String filename
    ) throws IOException {

        storageService.deleteByFilename(
                filename
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Resolve the stored content type.
     */
    private MediaType getContentType(
            StoredFile stored
    ) {

        String contentType =
                stored.getContentType();

        if (contentType == null ||
                contentType.isBlank()) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(
                    contentType
            );
        } catch (IllegalArgumentException e) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}

