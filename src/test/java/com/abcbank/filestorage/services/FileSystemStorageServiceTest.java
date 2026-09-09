package com.abcbank.filestorage.services;

import com.abcbank.filestorage.entities.StoredFile;
import com.abcbank.filestorage.exceptions.FileNotFoundException;
import com.abcbank.filestorage.exceptions.InvalidFileTypeException;
import com.abcbank.filestorage.repositories.StoredFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileSystemStorageServiceTest {

    private FileSystemStorageService storageService;

    private StoredFileRepository repository;

    private Path testRoot;

    private static final String BACKEND_BASE_URL =
            "http://localhost:8080";

    @BeforeEach
    void setUp() throws IOException {

        repository = Mockito.mock(
                StoredFileRepository.class
        );

        testRoot = Files.createTempDirectory(
                "test-uploads"
        );

        storageService = new FileSystemStorageService(
                repository,
                testRoot.toString(),
                BACKEND_BASE_URL
        );
    }

    @AfterEach
    void tearDown() throws IOException {

        deleteDirectory(testRoot);
    }

    private void deleteDirectory(Path directory)
            throws IOException {

        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            paths.sorted(
                    (path1, path2) ->
                            path2.compareTo(path1)
            ).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Ignore cleanup failures.
                }
            });
        }
    }

    @Test
    void testStoreFileBuildsUrls()
            throws IOException {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "hello.txt",
                        "text/plain",
                        "Hello World".getBytes()
                );

        when(
                repository.findByOriginalName(
                        "hello.txt"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                repository.save(
                        any(StoredFile.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        StoredFile result =
                storageService.store(file);

        assertThat(
                result.getOriginalName()
        ).isEqualTo("hello.txt");

        assertThat(
                result.getSize()
        ).isEqualTo(11);

        assertThat(
                result.getDownloadUrl()
        ).isEqualTo(
                "http://localhost:8080/files/download/hello.txt"
        );

        assertThat(
                result.getViewUrl()
        ).isEqualTo(
                "http://localhost:8080/files/hello.txt"
        );

        verify(
                repository,
                times(1)
        ).save(
                any(StoredFile.class)
        );
    }

    @Test
    void testStoreFileCreatesDateSubdirectories()
            throws IOException {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test.txt",
                        "text/plain",
                        "Hello".getBytes()
                );

        when(
                repository.findByOriginalName(
                        "test.txt"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                repository.save(
                        any(StoredFile.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        StoredFile result =
                storageService.store(file);

        Path expectedDirectory =
                testRoot.resolve(
                        LocalDate.now().format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy/MM/dd"
                                )
                        )
                );

        Path expectedPath =
                expectedDirectory.resolve(
                        "test.txt"
                );

        assertThat(
                Files.exists(expectedDirectory)
        ).isTrue();

        assertThat(
                Files.isDirectory(expectedDirectory)
        ).isTrue();

        assertThat(
                Files.exists(expectedPath)
        ).isTrue();

        assertThat(
                Files.isRegularFile(expectedPath)
        ).isTrue();

        assertThat(
                result.getOriginalName()
        ).isEqualTo("test.txt");

        assertThat(
                Path.of(
                        result.getFilePath()
                )
                .toAbsolutePath()
                .normalize()
        ).isEqualTo(
                expectedPath
                        .toAbsolutePath()
                        .normalize()
        );
    }

    @Test
    void testStoreEmptyFileThrowsException() {

        MockMultipartFile emptyFile =
                new MockMultipartFile(
                        "file",
                        "empty.txt",
                        "text/plain",
                        new byte[0]
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        storageService.store(
                                emptyFile
                        )
        );
    }

    @Test
    void testStoreInvalidFileTypeThrowsException() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "malware.exe",
                        "application/octet-stream",
                        "dummy".getBytes()
                );

        assertThrows(
                InvalidFileTypeException.class,
                () ->
                        storageService.store(file)
        );
    }

    @Test
    void testLoadAsResource()
            throws IOException {

        Path dateDirectory =
                testRoot.resolve(
                        LocalDate.now().format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy/MM/dd"
                                )
                        )
                );

        Files.createDirectories(
                dateDirectory
        );

        Path filePath =
                dateDirectory.resolve(
                        "test.txt"
                );

        Files.writeString(
                filePath,
                "Hello World"
        );

        StoredFile stored =
                new StoredFile();

        stored.setOriginalName(
                "test.txt"
        );

        stored.setFilePath(
                filePath.toString()
        );

        Resource resource =
                storageService.loadAsResource(
                        stored
                );

        assertThat(
                resource.exists()
        ).isTrue();

        assertThat(
                resource.getFile().getName()
        ).isEqualTo(
                "test.txt"
        );
    }

    @Test
    void testLoadAsResourceFileNotFound() {

        StoredFile stored =
                new StoredFile();

        stored.setOriginalName(
                "missing.txt"
        );

        stored.setFilePath(
                testRoot
                        .resolve(
                                "2026/09/08/missing.txt"
                        )
                        .toString()
        );

        assertThrows(
                FileNotFoundException.class,
                () ->
                        storageService.loadAsResource(
                                stored
                        )
        );
    }

    @Test
    void testLoadAsResourceRejectsOutsideRoot()
            throws IOException {

        Path outsideFile =
                Files.createTempFile(
                        "outside-storage",
                        ".txt"
                );

        try {

            Files.writeString(
                    outsideFile,
                    "This file is outside storage"
            );

            StoredFile stored =
                    new StoredFile();

            stored.setOriginalName(
                    "outside.txt"
            );

            stored.setFilePath(
                    outsideFile.toString()
            );

            assertThrows(
                    FileNotFoundException.class,
                    () ->
                            storageService.loadAsResource(
                                    stored
                            )
            );

        } finally {

            Files.deleteIfExists(
                    outsideFile
            );
        }
    }

    @Test
    void testDeleteFileByFilename()
            throws IOException {

        Path dateDirectory =
                testRoot.resolve(
                        LocalDate.now().format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy/MM/dd"
                                )
                        )
                );

        Files.createDirectories(
                dateDirectory
        );

        Path filePath =
                dateDirectory.resolve(
                        "delete.txt"
                );

        Files.writeString(
                filePath,
                "to be deleted"
        );

        StoredFile stored =
                new StoredFile();

        stored.setOriginalName(
                "delete.txt"
        );

        stored.setFilePath(
                filePath.toString()
        );

        when(
                repository.findByOriginalName(
                        "delete.txt"
                )
        ).thenReturn(
                Optional.of(stored)
        );

        storageService.deleteByFilename(
                "delete.txt"
        );

        assertThat(
                Files.exists(filePath)
        ).isFalse();

        verify(
                repository,
                times(1)
        ).delete(stored);
    }

    @Test
    void testDeleteFileByFilenameNotFound() {

        when(
                repository.findByOriginalName(
                        "missing.txt"
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                FileNotFoundException.class,
                () ->
                        storageService.deleteByFilename(
                                "missing.txt"
                        )
        );
    }

    @Test
    void testFindByOriginalName() {

        StoredFile stored =
                new StoredFile();

        stored.setOriginalName(
                "hello.txt"
        );

        stored.setDownloadUrl(
                "http://localhost:8080/files/download/hello.txt"
        );

        when(
                repository.findByOriginalName(
                        "hello.txt"
                )
        ).thenReturn(
                Optional.of(stored)
        );

        StoredFile result =
                storageService.findByOriginalNameOrThrow(
                        "hello.txt"
                );

        assertThat(
                result.getOriginalName()
        ).isEqualTo("hello.txt");

        assertThat(
                result.getDownloadUrl()
        ).contains(
                "/files/download/hello.txt"
        );
    }

    @Test
    void testFindByOriginalNameNotFound() {

        when(
                repository.findByOriginalName(
                        "missing.txt"
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                FileNotFoundException.class,
                () ->
                        storageService
                                .findByOriginalNameOrThrow(
                                        "missing.txt"
                                )
        );
    }

    @Test
    void testFindAll() {

        StoredFile stored =
                new StoredFile();

        stored.setOriginalName(
                "hello.txt"
        );

        stored.setDownloadUrl(
                "http://localhost:8080/files/download/hello.txt"
        );

        when(
                repository.findAll()
        ).thenReturn(
                List.of(stored)
        );

        List<StoredFile> result =
                storageService.findAll();

        assertThat(result)
                .hasSize(1);

        assertThat(
                result.get(0).getOriginalName()
        ).isEqualTo("hello.txt");

        assertThat(
                result.get(0).getDownloadUrl()
        ).contains(
                "/files/download/hello.txt"
        );
    }
}
