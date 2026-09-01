package com.abcbank.filestorage.services;

import com.abcbank.filestorage.entities.StoredFile;
import com.abcbank.filestorage.exceptions.FileNotFoundException;
import com.abcbank.filestorage.exceptions.InvalidFileTypeException;
import com.abcbank.filestorage.repositories.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path root;
    private final StoredFileRepository repository;

    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("txt", "pdf", "jpg", "png");

    public FileSystemStorageService(
            StoredFileRepository repository,
            @Value("${filestorage.root:uploads}") String rootDir
    ) throws IOException {
        this.repository = repository;
        this.root = Paths.get(System.getProperty("user.dir")).resolve(rootDir);
        Files.createDirectories(root);
    }

    @Override
    @CacheEvict(value = {"files", "filesByOriginalName"}, allEntries = true)
    public StoredFile store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !isAllowedExtension(originalName)) {
            throw new InvalidFileTypeException(originalName, ALLOWED_EXTENSIONS);
        }

        String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        String filename = UUID.randomUUID() + "." + ext;

        Path destination = root.resolve(filename);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        StoredFile stored = new StoredFile();
        stored.setOriginalName(originalName);
        stored.setContentType(file.getContentType());
        stored.setSize(file.getSize());
        stored.setCreatedOn(LocalDateTime.now());

       
        String downloadUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/files/")
                .path(originalName)
                .toUriString();

        String viewUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/files/")
                .path(originalName)
                .toUriString();

        stored.setDownloadUrl(downloadUrl);
        stored.setViewUrl(viewUrl);

        return repository.save(stored);
    }

    private boolean isAllowedExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) return false;
        String ext = filename.substring(dotIndex + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    @Override
    public Resource loadAsResource(StoredFile storedFile) {
        Path filePath = root.resolve(storedFile.getOriginalName());
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException(storedFile.getOriginalName());
        }
        return new FileSystemResource(filePath);
    }

    @Override
    @CacheEvict(value = {"files", "filesByOriginalName"}, allEntries = true)
    public void deleteByFilename(String filename) throws IOException {
        StoredFile stored = repository.findByOriginalName(filename)
                .orElseThrow(() -> new FileNotFoundException(filename));

        Files.deleteIfExists(root.resolve(stored.getOriginalName()));
        repository.delete(stored);
    }

    @Override
    public List<StoredFile> findAll() {
        return repository.findAll();
    }

    @Override
    @Cacheable(value = "filesByOriginalName", key = "#filename")
    public StoredFile findByOriginalNameOrThrow(String filename) {
        return repository.findByOriginalName(filename)
                .orElseThrow(() -> new FileNotFoundException(filename));
    }

    @Override
    public Optional<StoredFile> findByOriginalName(String filename) {
        return repository.findByOriginalName(filename);
    }
}
eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ6NjVJT3ZXNEFwN0xMbVN6LU53dFZzeXV5emtkMUhHVTJuX2Jwd0FSeUpNIn0.eyJleHAiOjE3ODgyNjQ0OTYsImlhdCI6MTc4ODI0NjQ5NiwianRpIjoiOWU0N2I5MDEtYjU0OC00ZjYxLTg1ZGUtOTRmZDljZTkyZDBmIiwiaXNzIjoiaHR0cHM6Ly9rZXljbG9ha3VhdC5hYmN0aGViYW5rLmNvbTo4MDk5L3JlYWxtcy9GaWxlU3RvcmFnZSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJhZDU4YjE1NC04YTVkLTQwZjUtOTAzOS00ZTVkYTFhMjkyMzciLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJmaWxlLXN0b3JhZ2UtY2xpZW50Iiwic2Vzc2lvbl9zdGF0ZSI6IjM3MDMwZTE2LTE2ZWItNDhhMS1hYzJmLTkyMTUzMTgwMWM1YiIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1maWxlc3RvcmFnZSIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJzaWQiOiIzNzAzMGUxNi0xNmViLTQ4YTEtYWMyZi05MjE1MzE4MDFjNWIiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6ImFwaWNhbGxlciIsImdpdmVuX25hbWUiOiIiLCJmYW1pbHlfbmFtZSI6IiJ9.LsNksjWg-1WlB8dNOaOuvjGwlK4o4V6JO0jsO3ZtuoD0IWfT32pJhxE8qbj96ScpH6fgtIBRBXHy1O1PHFvcOvcQ2V79ZV4vHAastF92y67m9xgAborRsPSAcrl9IR1y1jv-jsX5--cS7eCrCWPZrhetmQxrs7-JdgWH0AZYyGR96-SccIy8CRFjhBp5Aht5PH8nr1O_EtmeJmZjINAxPYH4NgYRClglCNOYKSFN7TRz9r6vnPlWFabcwH9XatQjb90cP77zli_Q4G9j9_1S8LIVk-3Zai01Nc1awtEROMJv9f51QKWpXRskn1y4uu0gKMfiuzRoqn3tvaoXNHTfiQ