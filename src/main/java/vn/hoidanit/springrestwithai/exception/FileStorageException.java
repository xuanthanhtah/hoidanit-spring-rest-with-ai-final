package vn.hoidanit.springrestwithai.exception;

import java.io.Serial;

// Server-side I/O failure while storing a file — handled by the catch-all as a generic 500
public class FileStorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
