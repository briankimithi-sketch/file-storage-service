package com.abcbank.filestorage.controllers;

import com.abcbank.filestorage.entities.StoredFile;
import com.abcbank.filestorage.services.StorageService;
import com.abcbank.filestorage.exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileControllerTest {

    private MockMvc mockMvc;
    private StorageService storageService;

    @BeforeEach
    void setUp() {

        storageService =
                mock(StorageService.class);

        FileController controller =
                new FileController(storageService);

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .setControllerAdvice(
                                new GlobalExceptionHandler()
                        )
                        .build();
    }

    @Test
    void testUploadFile() throws Exception {

        StoredFile stored =
                new StoredFile();

        stored.setOriginalName("hello.txt");
        stored.setSize(11);
        stored.setContentType("text/plain");

        stored.setDownloadUrl(
                "http://localhost:8080/files/download/hello.txt"
        );

        stored.setViewUrl(
                "http://localhost:8080/files/hello.txt"
        );

        when(
                storageService.store(any())
        ).thenReturn(stored);

        mockMvc.perform(
                        multipart("/files/upload")
                                .file(
                                        "file",
                                        "Hello World".getBytes()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.originalName")
                                .value("hello.txt")
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(11)
                )
                .andExpect(
                        jsonPath("$.downloadUrl")
                                .value(
                                        "http://localhost:8080/files/download/hello.txt"
                                )
                )
                .andExpect(
                        jsonPath("$.viewUrl")
                                .value(
                                        "http://localhost:8080/files/hello.txt"
                                )
                );

        verify(
                storageService,
                times(1)
        ).store(any());
    }

    @Test
    void testDownloadFile() throws Exception {

        StoredFile stored =
                new StoredFile();

        stored.setOriginalName("hello.txt");
        stored.setContentType("text/plain");
        stored.setSize(11);

        Resource resource =
                new ByteArrayResource(
                        "Hello World".getBytes()
                );

        when(
                storageService.findByOriginalNameOrThrow(
                        "hello.txt"
                )
        ).thenReturn(stored);

        when(
                storageService.loadAsResource(stored)
        ).thenReturn(resource);

        mockMvc.perform(
                        get("/files/download/hello.txt")
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                "attachment; filename=\"hello.txt\""
                        )
                )
                .andExpect(
                        content().contentType(
                                MediaType.TEXT_PLAIN
                        )
                );

        verify(
                storageService,
                times(1)
        ).findByOriginalNameOrThrow(
                "hello.txt"
        );

        verify(
                storageService,
                times(1)
        ).loadAsResource(stored);
    }

    @Test
    void testDownloadFileNotFound()
            throws Exception {

        when(
                storageService.findByOriginalNameOrThrow(
                        "missing.txt"
                )
        ).thenThrow(
                new com.abcbank.filestorage.exceptions.FileNotFoundException(
                        "missing.txt"
                )
        );

        mockMvc.perform(
                        get("/files/download/missing.txt")
                )
                .andExpect(
                        status().isNotFound()
                );

        verify(
                storageService,
                times(1)
        ).findByOriginalNameOrThrow(
                "missing.txt"
        );
    }

    @Test
    void testViewFile() throws Exception {

        StoredFile stored =
                new StoredFile();

        stored.setOriginalName("hello.txt");
        stored.setContentType("text/plain");
        stored.setSize(11);

        Resource resource =
                new ByteArrayResource(
                        "Hello World".getBytes()
                );

        when(
                storageService.findByOriginalNameOrThrow(
                        "hello.txt"
                )
        ).thenReturn(stored);

        when(
                storageService.loadAsResource(stored)
        ).thenReturn(resource);

        mockMvc.perform(
                        get("/files/hello.txt")
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                "inline; filename=\"hello.txt\""
                        )
                )
                .andExpect(
                        content().contentType(
                                MediaType.TEXT_PLAIN
                        )
                );

        verify(
                storageService,
                times(1)
        ).findByOriginalNameOrThrow(
                "hello.txt"
        );

        verify(
                storageService,
                times(1)
        ).loadAsResource(stored);
    }

    @Test
    void testDeleteFile() throws Exception {

        doNothing()
                .when(storageService)
                .deleteByFilename(
                        "hello.txt"
                );

        mockMvc.perform(
                        delete("/files/hello.txt")
                )
                .andExpect(
                        status().isNoContent()
                );

        verify(
                storageService,
                times(1)
        ).deleteByFilename(
                "hello.txt"
        );
    }

    @Test
    void testDeleteFileNotFound()
            throws Exception {

        doThrow(
                new com.abcbank.filestorage.exceptions.FileNotFoundException(
                        "missing.txt"
                )
        )
                .when(storageService)
                .deleteByFilename(
                        "missing.txt"
                );

        mockMvc.perform(
                        delete("/files/missing.txt")
                )
                .andExpect(
                        status().isNotFound()
                );

        verify(
                storageService,
                times(1)
        ).deleteByFilename(
                "missing.txt"
        );
    }
}
