package io.kope.testr.stores;

public class StoreException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public StoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public StoreException(String message) {
        super(message);
    }

}
