package com.abcbank.file_storage_service.services;

import com.abcbank.file_storage_service.entities.StoredFile;
import com.abcbank.file_storage_service.exceptions.FileNotFoundException;
import com.abcbank.file_storage_service.exceptions.InvalidFileTypeException;
import com.abcbank.file_storage_service.repositories.StoredFileRepository;
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
        stored.setId(1L);
        stored.setFilePath(filePath.toString());

        Resource resource = storageService.loadAsResource(stored);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFile().getName()).contains("test");
    }

    
    @Test
    void testLoadAsResourceFileNotFound() {
        StoredFile stored = new StoredFile();
        stored.setId(99L);
        stored.setFilePath(testRoot.resolve("missing.txt").toString());

        assertThrows(FileNotFoundException.class, () -> storageService.loadAsResource(stored));
    }

    
    @Test
    void testDeleteFile() throws IOException {
        Path filePath = Files.createTempFile(testRoot, "delete", ".txt");
        StoredFile stored = new StoredFile();
        stored.setId(1L);
        stored.setFilePath(filePath.toString());

        when(repository.findById(1L)).thenReturn(Optional.of(stored));

        storageService.delete(1L);

        assertThat(Files.exists(filePath)).isFalse();
        verify(repository, times(1)).delete(stored);
    }

    
    @Test
    void testDeleteFileNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> storageService.delete(42L));
    }

    
    @Test
    void testFindById() {
        StoredFile stored = new StoredFile();
        stored.setId(1L);
        stored.setOriginalName("hello.txt");

        when(repository.findById(1L)).thenReturn(Optional.of(stored));

        StoredFile result = storageService.findById(1L);

        assertThat(result.getOriginalName()).isEqualTo("hello.txt");
    }

    
    @Test
    void testFindByIdNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> storageService.findById(99L));
    }

    @Test
    void testFindAll() {
        StoredFile stored = new StoredFile();
        stored.setId(1L);
        stored.setOriginalName("hello.txt");

        when(repository.findAll()).thenReturn(List.of(stored));

        List<StoredFile> result = storageService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalName()).isEqualTo("hello.txt");
    }
}

