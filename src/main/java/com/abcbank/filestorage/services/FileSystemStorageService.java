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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path root;
    private final StoredFileRepository repository;
    private final String backendBaseUrl;

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of(
                    "txt",
                    "pdf",
                    "jpg",
                    "png"
            );

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");


    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyyMMdd_HHmmssSSS"
            );

    public FileSystemStorageService(
            StoredFileRepository repository,

            @Value("${filestorage.root:uploads}")
            String rootDir,

            @Value("${filestorage.base-url:http://localhost:8080}")
            String backendBaseUrl
    ) throws IOException {

        this.repository = repository;

        this.backendBaseUrl =
                backendBaseUrl.replaceAll("/$", "");

        this.root =
                Paths
                        .get(System.getProperty("user.dir"))
                        .resolve(rootDir)
                        .toAbsolutePath()
                        .normalize();

        Files.createDirectories(root);
    }

    @Override
    @CacheEvict(
            value = {
                    "files",
                    "filesByOriginalName"
            },
            allEntries = true
    )
    public StoredFile store(
            MultipartFile file
    ) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot store empty file"
            );
        }


        String originalName =
                file.getOriginalFilename();

        if (originalName == null ||
                originalName.isBlank()) {

            throw new InvalidFileTypeException(
                    originalName,
                    ALLOWED_EXTENSIONS
            );
        }

        originalName =
                Paths
                        .get(originalName)
                        .getFileName()
                        .toString();

        if (!isAllowedExtension(originalName)) {

            throw new InvalidFileTypeException(
                    originalName,
                    ALLOWED_EXTENSIONS
            );
        }


        String storedFilename =
                generateUniqueFilename(
                        originalName
                );

        String datePath =
                LocalDate
                        .now()
                        .format(DATE_FORMAT);

        Path subDirectory =
                root
                        .resolve(datePath)
                        .normalize();

        if (!subDirectory.startsWith(root)) {

            throw new IOException(
                    "Invalid storage directory"
            );
        }

        Files.createDirectories(
                subDirectory
        );

        Path destination =
                subDirectory
                        .resolve(storedFilename)
                        .normalize();

        if (!destination.startsWith(root)) {

            throw new IOException(
                    "Invalid file destination"
            );
        }

        byte[] fileBytes =
                file.getInputStream()
                        .readAllBytes();

        if (isImage(file.getContentType())) {

            fileBytes =
                    compressImage(
                            fileBytes,
                            file.getContentType()
                    );
        }


        Files.write(
                destination,
                fileBytes
        );


        StoredFile stored =
                new StoredFile();

        stored.setOriginalName(
                storedFilename
        );

        stored.setFilePath(
                destination.toString()
        );

        stored.setContentType(
                file.getContentType()
        );

        stored.setSize(
                (long) fileBytes.length
        );

        stored.setCreatedOn(
                LocalDateTime.now()
        );


        populateUrls(stored);

        return repository.save(stored);
    }


    private String generateUniqueFilename(
            String originalName
    ) {

        int dotIndex =
                originalName.lastIndexOf('.');

        String name;

        String extension = "";

        if (dotIndex > 0) {

            name =
                    originalName.substring(
                            0,
                            dotIndex
                    );

            extension =
                    originalName.substring(
                            dotIndex
                    );

        } else {

            name = originalName;
        }

        String timestamp =
                LocalDateTime
                        .now()
                        .format(
                                TIMESTAMP_FORMAT
                        );

        String generatedName =
                name +
                        "_" +
                        timestamp +
                        extension;


        int counter = 1;

        String candidate =
                generatedName;

        while (
                repository
                        .findByOriginalName(candidate)
                        .isPresent()
        ) {

            candidate =
                    name +
                            "_" +
                            timestamp +
                            "_" +
                            counter +
                            extension;

            counter++;
        }

        return candidate;
    }

    private void populateUrls(
            StoredFile stored
    ) {

        String encodedFilename =
                encodeFilename(
                        stored.getOriginalName()
                );

        String downloadUrl =
                backendBaseUrl +
                        "/files/download/" +
                        encodedFilename;

        String viewUrl =
                backendBaseUrl +
                        "/files/" +
                        encodedFilename;

        stored.setDownloadUrl(
                downloadUrl
        );

        stored.setViewUrl(
                viewUrl
        );
    }


    private boolean isAllowedExtension(
            String filename
    ) {

        int dotIndex =
                filename.lastIndexOf('.');

        if (dotIndex == -1) {
            return false;
        }

        String extension =
                filename
                        .substring(
                                dotIndex + 1
                        )
                        .toLowerCase();

        return ALLOWED_EXTENSIONS
                .contains(extension);
    }

    private boolean isImage(
            String contentType
    ) {

        return "image/jpeg"
                .equalsIgnoreCase(contentType)

                || "image/jpg"
                .equalsIgnoreCase(contentType)

                || "image/png"
                .equalsIgnoreCase(contentType);
    }

    private byte[] compressImage(
            byte[] imageData,
            String contentType
    ) throws IOException {

        BufferedImage image =
                ImageIO.read(
                        new ByteArrayInputStream(
                                imageData
                        )
                );

        if (image == null) {
            return imageData;
        }


        if ("image/png"
                .equalsIgnoreCase(contentType)) {

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    image,
                    "png",
                    output
            );

            return output.toByteArray();
        }

        return compressJpeg(
                image,
                0.75f
        );
    }


    private byte[] compressJpeg(
            BufferedImage image,
            float quality
    ) throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName(
                        "jpeg"
                );

        if (!writers.hasNext()) {

            throw new IOException(
                    "No JPEG image writer available"
            );
        }

        ImageWriter writer =
                writers.next();

        try (
                ImageOutputStream imageOutput =
                        ImageIO.createImageOutputStream(
                                output
                        )
        ) {

            writer.setOutput(
                    imageOutput
            );

            ImageWriteParam writeParam =
                    writer.getDefaultWriteParam();

            if (writeParam.canWriteCompressed()) {

                writeParam.setCompressionMode(
                        ImageWriteParam
                                .MODE_EXPLICIT
                );

                writeParam.setCompressionQuality(
                        quality
                );
            }

            writer.write(
                    null,
                    new IIOImage(
                            image,
                            null,
                            null
                    ),
                    writeParam
            );

        } finally {

            writer.dispose();
        }

        return output.toByteArray();
    }

    @Override
    public Resource loadAsResource(
            StoredFile storedFile
    ) {

        Path filePath =
                Paths
                        .get(
                                storedFile.getFilePath()
                        )
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

        return new FileSystemResource(
                filePath
        );
    }

    @Override
    @CacheEvict(
            value = {
                    "files",
                    "filesByOriginalName"
            },
            allEntries = true
    )
    public void deleteByFilename(
            String filename
    ) throws IOException {

        StoredFile stored =
                repository
                        .findByOriginalName(filename)
                        .orElseThrow(
                                () ->
                                        new FileNotFoundException(
                                                filename
                                        )
                        );

        Path filePath =
                Paths
                        .get(
                                stored.getFilePath()
                        )
                        .toAbsolutePath()
                        .normalize();

        if (!filePath.startsWith(root)) {

            throw new IOException(
                    "Invalid file location"
            );
        }

        Files.deleteIfExists(
                filePath
        );


        repository.delete(
                stored
        );
    }

    @Override
    @Cacheable("files")
    public List<StoredFile> findAll() {

        List<StoredFile> files =
                repository.findAll();

        files.forEach(
                this::populateUrls
        );

        return files;
    }

    @Override
    public Optional<StoredFile> findByOriginalName(
            String filename
    ) {

        return repository.findByOriginalName(
                filename
        );
    }

    @Override
    @Cacheable(
            value = "filesByOriginalName",
            key = "#filename"
    )
    public StoredFile findByOriginalNameOrThrow(
            String filename
    ) {

        StoredFile stored =
                repository
                        .findByOriginalName(filename)
                        .orElseThrow(
                                () ->
                                        new FileNotFoundException(
                                                filename
                                        )
                        );

        populateUrls(stored);

        return stored;
    }


    private String encodeFilename(
            String filename
    ) {

        return filename
                .replace(" ", "%20")
                .replace("#", "%23")
                .replace("?", "%3F");
    }
}