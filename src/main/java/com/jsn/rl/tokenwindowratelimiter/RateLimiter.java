package com.jsn.rl.tokenwindowratelimiter;

public interface RateLimiter {
    public void performRateLimitOnRequest(String user, int requestCount) throws RateLimitException;
    //public void loadLuaScriptToRedis();
    //public void unloadLuaScript();
}
