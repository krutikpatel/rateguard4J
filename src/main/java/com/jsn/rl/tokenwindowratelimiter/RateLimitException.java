package com.jsn.rl.tokenwindowratelimiter;

public class RateLimitException extends Exception {
    public RateLimitException(String message) {
        super(message);
    }
}
