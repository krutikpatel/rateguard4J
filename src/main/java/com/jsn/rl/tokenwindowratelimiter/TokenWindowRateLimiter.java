package com.jsn.rl.tokenwindowratelimiter;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsn.rl.tokenwindowratelimiter.util.RedisScriptHelper;
import com.jsn.rl.tokenwindowratelimiter.util.TokenWindowParams;
import com.jsn.rl.tokenwindowratelimiter.util.TokenWindowScriptArgsFactory;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisScriptingCommands;

//import javax.annotation.concurrent.ThreadSafe;
//@ThreadSafe
public class TokenWindowRateLimiter implements RateLimiter {
    private static final Logger LOG = LoggerFactory.getLogger(TokenWindowRateLimiter.class);
    private static final String SCRIPT_NAME = "TokenBucket.lua";
    private TokenWindowParams tokenWindowParams;

    public TokenWindowScriptArgsFactory tokenWindowScriptArgsFactory = new TokenWindowScriptArgsFactory();
    private RedisScriptHelper redisScriptLoader;
    
    private StatefulRedisConnection<String, String> connection;// StatefulRedisConnection OR StatefulRedisClusterConnection
    private RedisScriptingCommands<String, String> scriptingCommands;
    

    public TokenWindowRateLimiter(StatefulRedisConnection<String, String> connection) throws IOException {
        this.connection = connection;
        this.scriptingCommands = connection.sync();
        redisScriptLoader = new RedisScriptHelper(connection);
        redisScriptLoader.loadScript(SCRIPT_NAME);
        tokenWindowParams = getDefaultTokenWindowParams();
    }

    public TokenWindowRateLimiter(StatefulRedisConnection<String, String> connection, TokenWindowParams tokenWindowParams) throws IOException {
        this.connection = connection;
        this.scriptingCommands = connection.sync();
        redisScriptLoader = new RedisScriptHelper(connection);
        redisScriptLoader.loadScript(SCRIPT_NAME);
        this.tokenWindowParams = tokenWindowParams;
    }

    @Override
    public void performRateLimitOnMultiRequest(String user, int requestCount) throws RateLimitException {
        String prefix = "request_rate_limiter." + user;
        //String prefix = "{request_rate_limiter." + user + "}"; prefix using hashtag to use in case of cluster. so that all keys are in same node
        String[] keys = {prefix + ".tokens", prefix + ".timestamp"};
        String[] args = tokenWindowScriptArgsFactory.getArgs(tokenWindowParams, requestCount);

        List<Long> results = redisScriptLoader.executeScript(SCRIPT_NAME, keys, args);
        if(results.get(0) == null) {
            results.set(0, 0L);
        }
        
        boolean allowed = results.get(0) == 1;
        if (!allowed) {
            LOG.info("user:"+user + " and result: "+results.toString());
            LOG.error("RateLimitError: 429");
            throw new RateLimitException("RateLimitError: 429");
        }
    }

    @Override
    public void performRateLimit(String user) throws RateLimitException {
        String prefix = "request_rate_limiter." + user;
        //String prefix = "{request_rate_limiter." + user + "}"; prefix using hashtag to use in case of cluster. so that all keys are in same node
        String[] keys = {prefix + ".tokens", prefix + ".timestamp"};
        String[] args = tokenWindowScriptArgsFactory.getArgs(tokenWindowParams, 1);

        List<Long> results = redisScriptLoader.executeScript(SCRIPT_NAME, keys, args);
        if(results.get(0) == null) {
            results.set(0, 0L);
        }
        
        boolean allowed = results.get(0) == 1;
        if (!allowed) {
            LOG.info("user:"+user + " and result: "+results.toString());
            LOG.error("RateLimitError: 429");
            throw new RateLimitException("RateLimitError: 429");
        }
    }

    private TokenWindowParams getDefaultTokenWindowParams() {
        return new TokenWindowParams(50); //replenishrate, capacity
    }
}
