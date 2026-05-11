package dev.toolkit.mongo.exception;

/**
 * @author revqz
 */
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String message) { super(message); }
}
