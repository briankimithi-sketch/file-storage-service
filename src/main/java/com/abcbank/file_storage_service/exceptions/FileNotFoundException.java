package com.abcbank.file_storage_service.exceptions;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(Long id) {
        super("File not found with id " + id);
    }
}
