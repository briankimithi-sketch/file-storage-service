package com.abcbank.filestorage.controllers;

import com.abcbank.filestorage.entities.StoredFile;
import com.abcbank.filestorage.services.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        StoredFile stored =
                storageService.store(file);

        Map<String, Object> response = Map.of(
                "originalName", stored.getOriginalName(),
                "size", stored.getSize(),
                "downloadUrl", stored.getDownloadUrl(),
                "viewUrl", stored.getViewUrl()
        );

        return ResponseEntity.ok(response);
    }

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

        String contentType =
                stored.getContentType();

        if (contentType == null ||
                contentType.isBlank()) {

            contentType =
                    MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                stored.getOriginalName() +
                                "\""
                )
                .contentType(
                        MediaType.parseMediaType(contentType)
                )
                .body(resource);
    }

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

        String contentType =
                stored.getContentType();

        if (contentType == null ||
                contentType.isBlank()) {

            contentType =
                    MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                stored.getOriginalName() +
                                "\""
                )
                .contentType(
                        MediaType.parseMediaType(contentType)
                )
                .body(resource);
    }

    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<Void> delete(
            @PathVariable String filename
    ) throws IOException {

        storageService.deleteByFilename(filename);

        return ResponseEntity.noContent().build();
    }
}
