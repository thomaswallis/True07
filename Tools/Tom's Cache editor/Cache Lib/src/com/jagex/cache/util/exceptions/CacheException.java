package com.jagex.cache.util.exceptions;

import java.io.IOException;

/**
 * @author Tom
 */
public class CacheException extends IOException {
    public CacheException() {
        super();
    }

    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheException(Throwable cause) {
        super(cause);
    }

    public CacheException(String message) {
        super(message);
    }
}
