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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

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
    void testStoreFileBuildsUrls() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello World".getBytes()
        );

        // ✅ Bind a mock request so ServletUriComponentsBuilder works
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        StoredFile saved = new StoredFile();
        saved.setOriginalName("hello.txt");
        saved.setContentType("text/plain");
        saved.setSize(11);
        saved.setDownloadUrl("http://localhost/files/hello.txt");
        saved.setViewUrl("http://localhost/files/hello.txt");

        when(repository.save(any(StoredFile.class))).thenReturn(saved);

        StoredFile result = storageService.store(file);

        assertThat(result.getOriginalName()).isEqualTo("hello.txt");
        assertThat(result.getSize()).isEqualTo(11);
        assertThat(result.getDownloadUrl()).isEqualTo("http://localhost/files/hello.txt");
        assertThat(result.getViewUrl()).isEqualTo("http://localhost/files/hello.txt");
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
        // ✅ Create file with exact name
        Path filePath = testRoot.resolve("test.txt");
        Files.writeString(filePath, "Hello World");

        StoredFile stored = new StoredFile();
        stored.setOriginalName("test.txt");
        stored.setFilePath(filePath.toString());

        Resource resource = storageService.loadAsResource(stored);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFile().getName()).isEqualTo("test.txt");
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
        // ✅ Create file with exact name
        Path filePath = testRoot.resolve("delete.txt");
        Files.writeString(filePath, "to be deleted");

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
        stored.setDownloadUrl("http://localhost/files/hello.txt");

        when(repository.findByOriginalName("hello.txt")).thenReturn(Optional.of(stored));

        StoredFile result = storageService.findByOriginalNameOrThrow("hello.txt");

        assertThat(result.getDownloadUrl()).isEqualTo("http://localhost/files/hello.txt");
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
        stored.setDownloadUrl("http://localhost/files/hello.txt");

        when(repository.findAll()).thenReturn(List.of(stored));

        List<StoredFile> result = storageService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDownloadUrl()).isEqualTo("http://localhost/files/hello.txt");
    }
}
