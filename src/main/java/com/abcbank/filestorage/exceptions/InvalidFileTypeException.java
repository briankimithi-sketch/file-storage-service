package com.abcbank.filestorage.exceptions;

import java.util.List;

public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException(String filename, List<String> allowed) {
        super("Invalid file type for " + filename + ". Allowed: " + allowed);
    }
}
