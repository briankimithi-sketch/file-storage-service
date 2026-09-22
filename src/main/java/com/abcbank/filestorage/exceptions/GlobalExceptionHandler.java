package com.abcbank.filestorage.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

@ExceptionHandler(FileNotFoundException.class)
public ResponseEntity<Map<String, String>> handleFileNotFound(
        FileNotFoundException ex) {

    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                    "error",
                    ex.getMessage()
            ));
}

@ExceptionHandler(InvalidFileTypeException.class)
public ResponseEntity<Map<String, String>> handleInvalidFileType(
        InvalidFileTypeException ex) {

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "error",
                    ex.getMessage()
            ));
}

@ExceptionHandler(AuthenticationFailedException.class)
public ResponseEntity<Map<String, String>> handleAuthenticationFailed(
        AuthenticationFailedException ex) {

    return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                    "error",
                    ex.getMessage()
            ));
}

@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String, String>> handleIllegalArgument(
        IllegalArgumentException ex) {

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "error",
                    ex.getMessage()
            ));
}

@ExceptionHandler(MaxUploadSizeExceededException.class)
public ResponseEntity<Map<String, String>> handleMaxSize(
        MaxUploadSizeExceededException ex) {

    return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(Map.of(
                    "error",
                    "File size exceeds limit"
            ));
}

@ExceptionHandler(IOException.class)
public ResponseEntity<Map<String, String>> handleIOException(
        IOException ex) {

    log.error("I/O error", ex);

    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                    "error",
                    "An I/O error occurred"
            ));
}

@ExceptionHandler(NoResourceFoundException.class)
public ResponseEntity<Map<String, String>> handleNoResource(
        NoResourceFoundException ex) {

    log.debug("No resource found: {}", ex.getResourcePath());

    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                    "error",
                    "Resource not found"
            ));
}

@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, String>> handleGeneralException(
        Exception ex) {

    log.error("Unhandled exception: {}", ex.getMessage(), ex);

    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                    "error",
                    "An unexpected error occurred"
            ));
}


}