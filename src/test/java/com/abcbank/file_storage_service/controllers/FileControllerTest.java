package com.abcbank.file_storage_service.controllers;

import com.abcbank.file_storage_service.entities.StoredFile;
import com.abcbank.file_storage_service.exceptions.FileNotFoundException;
import com.abcbank.file_storage_service.exceptions.InvalidFileTypeException;
import com.abcbank.file_storage_service.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class FileControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello World".getBytes()
        );

        StoredFile stored = new StoredFile();
        stored.setId(1L);
        stored.setOriginalName("hello.txt");
        stored.setContentType("text/plain");
        stored.setSize(11);
        stored.setFilePath("uploads/uuid.txt");

        Mockito.when(storageService.store(any())).thenReturn(stored);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalName").value("hello.txt"))
                .andExpect(jsonPath("$.size").value(11))
                .andExpect(jsonPath("$.downloadUrl").value("/api/files/download/1"));
    }

    @Test
    void testUploadEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );

        Mockito.when(storageService.store(any()))
                .thenThrow(new IllegalArgumentException("Cannot store empty file"));

        mockMvc.perform(multipart("/api/files/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot store empty file"));
    }

    @Test
    void testUploadInvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "dummy".getBytes()
        );

        Mockito.when(storageService.store(any()))
                .thenThrow(new InvalidFileTypeException("malware.exe", List.of("txt", "pdf", "jpg", "png")));

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid file type for malware.exe. Allowed: [txt, pdf, jpg, png]"));
    }

    @Test
    void testDownloadFile() throws Exception {
        StoredFile stored = new StoredFile();
        stored.setId(1L);
        stored.setOriginalName("hello.txt");
        stored.setContentType("text/plain");
        stored.setSize(11);
        stored.setFilePath("uploads/uuid.txt");

        ByteArrayResource resource = new ByteArrayResource("Hello World".getBytes());

        Mockito.when(storageService.findById(1L)).thenReturn(stored);
        Mockito.when(storageService.loadAsResource(1L)).thenReturn(resource);

        mockMvc.perform(get("/api/files/download/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hello.txt\""))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Hello World"));
    }

    @Test
    void testDownloadFileNotFound() throws Exception {
        Mockito.when(storageService.findById(99L))
                .thenThrow(new FileNotFoundException(99L));

        mockMvc.perform(get("/api/files/download/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("File not found with id 99"));
    }

    @Test
    void testDeleteFile() throws Exception {
        Mockito.doNothing().when(storageService).delete(1L);

        mockMvc.perform(delete("/api/files/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteFileNotFound() throws Exception {
        Mockito.doThrow(new FileNotFoundException(42L)).when(storageService).delete(42L);

        mockMvc.perform(delete("/api/files/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("File not found with id 42"));
    }
}