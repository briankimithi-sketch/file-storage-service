package com.abcbank.filestorage.controllers;

import com.abcbank.filestorage.entities.StoredFile;
import com.abcbank.filestorage.exceptions.FileNotFoundException;
import com.abcbank.filestorage.exceptions.InvalidFileTypeException;
import com.abcbank.filestorage.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @MockBean
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
        stored.setOriginalName("hello.txt");
        stored.setContentType("text/plain");
        stored.setSize(11);
        stored.setFilePath("uploads/uuid.txt");

        Mockito.when(storageService.store(any())).thenReturn(stored);

        mockMvc.perform(multipart("/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("hello.txt"))
                .andExpect(jsonPath("$.size").value(11))
                // ✅ Expect absolute URLs now
                .andExpect(jsonPath("$.downloadUrl").value("http://localhost/files/hello.txt"))
                .andExpect(jsonPath("$.viewUrl").value("http://localhost/files/hello.txt"));
    }

    @Test
    void testUploadEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );

        Mockito.when(storageService.store(any()))
                .thenThrow(new IllegalArgumentException("Cannot store empty file"));

        mockMvc.perform(multipart("/files/upload").file(emptyFile))
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

        mockMvc.perform(multipart("/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid file type for malware.exe. Allowed: [txt, pdf, jpg, png]"));
    }

    @Test
    void testDownloadFile() throws Exception {
        StoredFile stored = new StoredFile();
        stored.setOriginalName("hello.txt");
        stored.setContentType("text/plain");
        stored.setSize(11);
        stored.setFilePath("uploads/uuid.txt");

        ByteArrayResource resource = new ByteArrayResource("Hello World".getBytes());

        Mockito.when(storageService.findByOriginalNameOrThrow("hello.txt")).thenReturn(stored);
        Mockito.when(storageService.loadAsResource(stored)).thenReturn(resource);

        mockMvc.perform(get("/files/download/hello.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hello.txt\""))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Hello World"));
    }

    @Test
    void testDownloadFileNotFound() throws Exception {
        Mockito.when(storageService.findByOriginalNameOrThrow("missing.txt"))
                .thenThrow(new FileNotFoundException("missing.txt"));

        mockMvc.perform(get("/files/download/missing.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("File not found with filename: missing.txt"));
    }

    @Test
    void testDeleteFile() throws Exception {
        Mockito.doNothing().when(storageService).deleteByFilename("hello.txt");

        mockMvc.perform(delete("/files/hello.txt"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteFileNotFound() throws Exception {
        Mockito.doThrow(new FileNotFoundException("missing.txt"))
                .when(storageService).deleteByFilename("missing.txt");

        mockMvc.perform(delete("/files/missing.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("File not found with filename: missing.txt"));
    }
}
