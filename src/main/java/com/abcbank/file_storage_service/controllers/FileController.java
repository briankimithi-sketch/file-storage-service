
package com.abcbank.file_storage_service.controllers;

import com.abcbank.file_storage_service.entities.StoredFile;
import com.abcbank.file_storage_service.services.StorageService;
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

    private final StorageService service;

    public FileController(StorageService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        StoredFile stored = service.store(file);
        Map<String, Object> response = Map.of(
                "id", stored.getId(),
                "originalName", stored.getOriginalName(),
                "size", stored.getSize(),
                "downloadUrl", "/api/files/download/" + stored.getId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        StoredFile stored = service.findById(id); // metadata from DB
        Resource file = service.loadAsResource(id); // actual file from disk

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + stored.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(stored.getContentType()))
                .body(file);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
