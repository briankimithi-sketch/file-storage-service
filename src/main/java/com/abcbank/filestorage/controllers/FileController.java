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
@RequestMapping("/files")   // ✅ base path is /files only
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
                "uuidFilename", stored.getUuidFilename(),
                "size", stored.getSize()
        );
        return ResponseEntity.ok(response);
    }

    // Download by UUID filename (forces download)
    @GetMapping("/download/{uuidFilename}")
    public ResponseEntity<Resource> download(@PathVariable String uuidFilename) {
        StoredFile stored = storageService.findByUuidFilenameOrThrow(uuidFilename);
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

    // Inline view by UUID filename (browser displays)
    @GetMapping("/{uuidFilename}")
    public ResponseEntity<Resource> view(@PathVariable String uuidFilename) {
        StoredFile stored = storageService.findByUuidFilenameOrThrow(uuidFilename);
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

    // Delete by UUID filename
    @DeleteMapping("/{uuidFilename}")
    public ResponseEntity<Void> delete(@PathVariable String uuidFilename) throws IOException {
        storageService.deleteByFilename(uuidFilename);
        return ResponseEntity.noContent().build();
    }
}
