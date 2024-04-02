package com.jsn.rl.tokenwindowratelimiter;

public interface RateLimiter {
    public void performRateLimit(String user) throws RateLimitException;
    public void performRateLimitOnMultiRequest(String user, int requestCount) throws RateLimitException;
    //public void loadLuaScriptToRedis();
    //public void unloadLuaScript();
}
