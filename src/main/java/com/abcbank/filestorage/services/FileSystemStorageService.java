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

        this.root = Paths.get(System.getProperty("user.dir"))
                .resolve(rootDir);

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
        stored.setFilePath(destination.toString());
        stored.setCreatedOn(LocalDateTime.now());

        StoredFile savedFile = repository.save(stored);

        return savedFile;
    }

    private boolean isAllowedExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) return false;
        String ext = filename.substring(dotIndex + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    @Override
    public Resource loadAsResource(StoredFile storedFile) {
        Path filePath = Paths.get(storedFile.getFilePath());
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException(storedFile.getId());
        }
        return new FileSystemResource(filePath);
    }

    @Override
    @CacheEvict(value = {"files", "filesByOriginalName"}, allEntries = true)
    public void delete(Long id) throws IOException {
        StoredFile stored = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException(id));

        Files.deleteIfExists(Paths.get(stored.getFilePath()));
        repository.delete(stored);

    }

    @Override
    public List<StoredFile> findAll() {
        return repository.findAll();
    }

    @Override
    @Cacheable(value = "files", key = "#id")
    public StoredFile findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException(id));
    }

    @Override
    @Cacheable(value = "filesByOriginalName", key = "#filename")
    public Optional<StoredFile> findByOriginalName(String filename) {
        return repository.findByOriginalName(filename);
    }
}
