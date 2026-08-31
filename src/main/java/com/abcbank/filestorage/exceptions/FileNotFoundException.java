package com.abcbank.filestorage.exceptions;

public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(String filename) {
        super("File not found with filename: " + filename);
    }
}
