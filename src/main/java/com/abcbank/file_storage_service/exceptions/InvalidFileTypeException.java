package com.abcbank.file_storage_service.exceptions;

import java.util.List;

public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException(String filename, List<String> allowed) {
        super("Invalid file type for " + filename + ". Allowed: " + allowed);
    }
}
