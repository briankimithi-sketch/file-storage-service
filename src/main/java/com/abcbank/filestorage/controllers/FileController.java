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
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    // Upload file
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        StoredFile stored = storageService.store(file);
        Map<String, Object> response = Map.of(
                "originalName", stored.getOriginalName(),
                "size", stored.getSize(),
                "downloadUrl", "/api/files/download/" + stored.getOriginalName(),
                "viewUrl", "/api/files/view/" + stored.getOriginalName()
        );
        return ResponseEntity.ok(response);
    }

    // Download by filename (forces download)
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        StoredFile stored = storageService.findByOriginalNameOrThrow(filename);
        Resource resource = storageService.loadAsResource(stored);

        String contentType = stored.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + stored.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // Inline view by filename (browser displays)
    @GetMapping("/view/{filename:.+}")
    public ResponseEntity<Resource> view(@PathVariable String filename) {
        StoredFile stored = storageService.findByOriginalNameOrThrow(filename);
        Resource resource = storageService.loadAsResource(stored);

        String contentType = stored.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + stored.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // Delete by filename
    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<Void> delete(@PathVariable String filename) throws IOException {
        storageService.deleteByFilename(filename);
        return ResponseEntity.noContent().build();
    }
}
