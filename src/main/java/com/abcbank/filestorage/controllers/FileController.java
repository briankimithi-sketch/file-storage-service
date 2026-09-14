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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    
    @GetMapping
    public ResponseEntity<List<StoredFile>> findAll() {
        return ResponseEntity.ok(storageService.findAll());
    }

   
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        StoredFile storedFile = storageService.store(file);

    
        Map<String, Object> response = Map.of(
                "originalName", storedFile.getOriginalName(),
                "size", storedFile.getSize(),
                "downloadUrl", "/files/download/" + storedFile.getOriginalName(),
                "viewUrl", "/files/" + storedFile.getOriginalName()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        StoredFile storedFile = storageService.findByOriginalNameOrThrow(filename);
        Resource resource = storageService.loadAsResource(storedFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + storedFile.getOriginalName() + "\"")
                .contentType(getContentType(storedFile))
                .body(resource);
    }

    
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> view(@PathVariable String filename) {
        StoredFile storedFile = storageService.findByOriginalNameOrThrow(filename);
        Resource resource = storageService.loadAsResource(storedFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + storedFile.getOriginalName() + "\"")
                .contentType(getContentType(storedFile))
                .body(resource);
    }

    
    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<Void> delete(@PathVariable String filename) throws IOException {
        storageService.deleteByFilename(filename);
        return ResponseEntity.noContent().build();
    }

    private MediaType getContentType(StoredFile storedFile) {
        String contentType = storedFile.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}

