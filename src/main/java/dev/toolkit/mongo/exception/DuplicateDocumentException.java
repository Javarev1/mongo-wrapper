package dev.toolkit.mongo.exception;

/**
 * @author revqz
 */
public class DuplicateDocumentException extends RuntimeException {
    public DuplicateDocumentException(String message) { super(message); }
    public DuplicateDocumentException(String message, Throwable cause) { super(message, cause); }
}
