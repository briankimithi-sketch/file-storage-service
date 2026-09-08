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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path root;
    private final StoredFileRepository repository;

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("txt", "pdf", "jpg", "png");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public FileSystemStorageService(
            StoredFileRepository repository,
            @Value("${filestorage.root:uploads}") String rootDir
    ) throws IOException {

        this.repository = repository;

        this.root = Paths
                .get(System.getProperty("user.dir"))
                .resolve(rootDir)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(root);
    }

    @Override
    @CacheEvict(
            value = {"files", "filesByOriginalName"},
            allEntries = true
    )
    public StoredFile store(MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot store empty file"
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new InvalidFileTypeException(
                    originalName,
                    ALLOWED_EXTENSIONS
            );
        }


        originalName = Paths
                .get(originalName)
                .getFileName()
                .toString();

        if (!isAllowedExtension(originalName)) {
            throw new InvalidFileTypeException(
                    originalName,
                    ALLOWED_EXTENSIONS
            );
        }

        if (repository.findByOriginalName(originalName).isPresent()) {
            throw new IOException(
                    "A file with the name '" +
                            originalName +
                            "' already exists"
            );
        }


        String datePath =
                LocalDate.now().format(DATE_FORMAT);

        Path subDirectory =
                root.resolve(datePath)
                        .normalize();

        if (!subDirectory.startsWith(root)) {
            throw new IOException(
                    "Invalid storage directory"
            );
        }

        Files.createDirectories(subDirectory);

        Path destination =
                subDirectory
                        .resolve(originalName)
                        .normalize();

        if (!destination.startsWith(root)) {
            throw new IOException(
                    "Invalid file destination"
            );
        }

        /*
         * Do not overwrite an existing file.
         */
        Files.copy(
                file.getInputStream(),
                destination
        );

        StoredFile stored = new StoredFile();

        stored.setOriginalName(originalName);
        stored.setFilePath(destination.toString());
        stored.setContentType(file.getContentType());
        stored.setSize(file.getSize());
        stored.setCreatedOn(LocalDateTime.now());

        /*
         * URLs use the original filename.
         */
        String downloadUrl =
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .path("/files/download/")
                        .path(originalName)
                        .toUriString();

        String viewUrl =
                ServletUriComponentsBuilder
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

        if (dotIndex == -1) {
            return false;
        }

        String extension =
                filename
                        .substring(dotIndex + 1)
                        .toLowerCase();

        return ALLOWED_EXTENSIONS.contains(extension);
    }

    @Override
    public Resource loadAsResource(StoredFile storedFile) {

        Path filePath =
                Paths
                        .get(storedFile.getFilePath())
                        .toAbsolutePath()
                        .normalize();

        if (!filePath.startsWith(root)) {
            throw new FileNotFoundException(
                    storedFile.getOriginalName()
            );
        }

        if (!Files.exists(filePath) ||
                !Files.isRegularFile(filePath)) {

            throw new FileNotFoundException(
                    storedFile.getOriginalName()
            );
        }

        return new FileSystemResource(filePath);
    }

    @Override
    @CacheEvict(
            value = {"files", "filesByOriginalName"},
            allEntries = true
    )
    public void deleteByFilename(String filename)
            throws IOException {

        StoredFile stored =
                repository.findByOriginalName(filename)
                        .orElseThrow(
                                () -> new FileNotFoundException(
                                        filename
                                )
                        );

        Path filePath =
                Paths
                        .get(stored.getFilePath())
                        .toAbsolutePath()
                        .normalize();

        if (!filePath.startsWith(root)) {
            throw new IOException(
                    "Invalid file location"
            );
        }

        Files.deleteIfExists(filePath);

        repository.delete(stored);
    }

    @Override
    @Cacheable("files")
    public List<StoredFile> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<StoredFile> findByOriginalName(
        String filename
    ) {
        return repository.findByOriginalName(filename);
    }

    @Override
    @Cacheable(
            value = "filesByOriginalName",
            key = "#filename"
    )
    public StoredFile findByOriginalNameOrThrow(
            String filename
    ) {

        return repository.findByOriginalName(filename)
                .orElseThrow(
                        () -> new FileNotFoundException(filename)
                );
    }
}
