package com.abcbank.filestorage.services;

import com.abcbank.filestorage.entities.StoredFile;
import com.abcbank.filestorage.exceptions.FileNotFoundException;
import com.abcbank.filestorage.exceptions.InvalidFileTypeException;
import com.abcbank.filestorage.repositories.StoredFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FileSystemStorageServiceTest {

    private FileSystemStorageService storageService;
    private StoredFileRepository repository;
    private Path testRoot;

    @BeforeEach
    void setUp() throws IOException {
        repository = Mockito.mock(StoredFileRepository.class);
        testRoot = Files.createTempDirectory("test-uploads");
        storageService = new FileSystemStorageService(repository, testRoot.toString());
    }

    @Test
    void testStoreFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello World".getBytes()
        );

        StoredFile saved = new StoredFile();
        saved.setOriginalName("hello.txt");
        saved.setContentType("text/plain");
        saved.setSize(11);
        saved.setFilePath(testRoot.resolve("uuid.txt").toString());

        when(repository.save(any(StoredFile.class))).thenReturn(saved);

        StoredFile result = storageService.store(file);

        assertThat(result.getOriginalName()).isEqualTo("hello.txt");
        assertThat(result.getSize()).isEqualTo(11);
        verify(repository, times(1)).save(any(StoredFile.class));
    }

    @Test
    void testStoreEmptyFileThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.store(emptyFile));
    }

    @Test
    void testStoreInvalidFileTypeThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "dummy".getBytes()
        );

        assertThrows(InvalidFileTypeException.class, () -> storageService.store(file));
    }

    @Test
    void testLoadAsResource() throws IOException {
        Path filePath = Files.createTempFile(testRoot, "test", ".txt");
        StoredFile stored = new StoredFile();
        stored.setOriginalName("test.txt");
        stored.setFilePath(filePath.toString());

        Resource resource = storageService.loadAsResource(stored);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFile().getName()).contains("test");
    }

    @Test
    void testLoadAsResourceFileNotFound() {
        StoredFile stored = new StoredFile();
        stored.setOriginalName("missing.txt");
        stored.setFilePath(testRoot.resolve("missing.txt").toString());

        assertThrows(FileNotFoundException.class, () -> storageService.loadAsResource(stored));
    }

    @Test
    void testDeleteFileByFilename() throws IOException {
        Path filePath = Files.createTempFile(testRoot, "delete", ".txt");
        StoredFile stored = new StoredFile();
        stored.setOriginalName("delete.txt");
        stored.setFilePath(filePath.toString());

        when(repository.findByOriginalName("delete.txt")).thenReturn(Optional.of(stored));

        storageService.deleteByFilename("delete.txt");

        assertThat(Files.exists(filePath)).isFalse();
        verify(repository, times(1)).delete(stored);
    }

    @Test
    void testDeleteFileByFilenameNotFound() {
        when(repository.findByOriginalName("missing.txt")).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> storageService.deleteByFilename("missing.txt"));
    }

    @Test
    void testFindByOriginalName() {
        StoredFile stored = new StoredFile();
        stored.setOriginalName("hello.txt");

        when(repository.findByOriginalName("hello.txt")).thenReturn(Optional.of(stored));

        StoredFile result = storageService.findByOriginalNameOrThrow("hello.txt");

        assertThat(result.getOriginalName()).isEqualTo("hello.txt");
    }

    @Test
    void testFindByOriginalNameNotFound() {
        when(repository.findByOriginalName("missing.txt")).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> storageService.findByOriginalNameOrThrow("missing.txt"));
    }

    @Test
    void testFindAll() {
        StoredFile stored = new StoredFile();
        stored.setOriginalName("hello.txt");

        when(repository.findAll()).thenReturn(List.of(stored));

        List<StoredFile> result = storageService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalName()).isEqualTo("hello.txt");
    }
}
