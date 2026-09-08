package com.abcbank.filestorage.services;

import com.abcbank.filestorage.entities.StoredFile;
import com.abcbank.filestorage.exceptions.FileNotFoundException;
import com.abcbank.filestorage.exceptions.InvalidFileTypeException;
import com.abcbank.filestorage.repositories.StoredFileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    @BeforeEach
    void setUp() throws IOException {

        repository =
                Mockito.mock(
                        StoredFileRepository.class
                );

        testRoot =
                Files.createTempDirectory(
                        "test-uploads"
                );

        storageService =
                new FileSystemStorageService(
                        repository,
                        testRoot.toString()
                );
    }

    @AfterEach
    void tearDown() {

        RequestContextHolder.resetRequestAttributes();
    }

    private void setUpMockRequest() {

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        when(request.getScheme())
                .thenReturn("http");

        when(request.getServerName())
                .thenReturn("localhost");

        when(request.getServerPort())
                .thenReturn(8080);

        when(request.getContextPath())
                .thenReturn("");

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request)
        );
    }

    @Test
    void testStoreFileBuildsUrls()
            throws IOException {

        setUpMockRequest();

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "hello.txt",
                        "text/plain",
                        "Hello World".getBytes()
                );

        StoredFile saved =
                new StoredFile();

        saved.setOriginalName("hello.txt");
        saved.setContentType("text/plain");
        saved.setSize(11);

        saved.setDownloadUrl(
                "http://localhost:8080/files/download/hello.txt"
        );

        saved.setViewUrl(
                "http://localhost:8080/files/hello.txt"
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
        ).thenReturn(saved);

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

        setUpMockRequest();

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
                ).toAbsolutePath().normalize()
        ).isEqualTo(
                expectedPath.toAbsolutePath()
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
        ).isEqualTo(
                "hello.txt"
        );

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
        ).isEqualTo(
                "hello.txt"
        );

        assertThat(
                result.get(0).getDownloadUrl()
        ).contains(
                "/files/download/hello.txt"
        );
    }
}
