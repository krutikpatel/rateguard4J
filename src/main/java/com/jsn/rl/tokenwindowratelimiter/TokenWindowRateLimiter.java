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
    private RedisScriptHelper redisScriptLoader;
    
    private StatefulRedisConnection<String, String> connection;// StatefulRedisConnection OR StatefulRedisClusterConnection
    private RedisScriptingCommands<String, String> scriptingCommands;
  
    private TokenWindowRateLimiter() {}

    private TokenWindowRateLimiter(Builder builder) throws IOException {
        this.connection = builder.connection;
        this.scriptingCommands = this.connection.sync();
        this.redisScriptLoader = new RedisScriptHelper(this.connection);
        this.tokenWindowParams = new TokenWindowParams(builder.replenishRate, builder.capacity);
    }
    public static class Builder {
        private StatefulRedisConnection<String, String> connection;
        private int capacity;
        private int replenishRate;

        public Builder withConnection(StatefulRedisConnection<String, String> connection) {
            this.connection = connection;
            return this;
        }

        public Builder withCapacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder withReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
            return this;
        }

        public TokenWindowRateLimiter build() throws IOException {
            return new TokenWindowRateLimiter(this);
        }
    }

    @Override
    public void performRateLimitOnMultiRequest(String user, int requestCount) throws RateLimitException {
        String prefix = "request_rate_limiter." + user;
        //String prefix = "{request_rate_limiter." + user + "}"; prefix using hashtag to use in case of cluster. so that all keys are in same node
        String[] keys = {prefix + ".tokens", prefix + ".timestamp"};
        String[] args = TokenWindowScriptArgsFactory.getArgs(tokenWindowParams, requestCount);

        List<Long> results = redisScriptLoader.executeScript(SCRIPT_NAME, keys, args);
        if(results.get(0) == null) {
            results.set(0, 0L);
        }
        
        boolean allowed = results.get(0) == 1;
        if (!allowed) {
            // LOG.info("user:"+user + " and result: "+results.toString());
            LOG.error("RateLimitError: 429");
            throw new RateLimitException("RateLimitError: 429");
        }
    }

    @Override
    public void performRateLimit(String user) throws RateLimitException {
        String prefix = "request_rate_limiter." + user;
        //String prefix = "{request_rate_limiter." + user + "}"; prefix using hashtag to use in case of cluster. so that all keys are in same node
        String[] keys = {prefix + ".tokens", prefix + ".timestamp"};
        String[] args = TokenWindowScriptArgsFactory.getArgs(tokenWindowParams, 1);

        List<Long> results = redisScriptLoader.executeScript(SCRIPT_NAME, keys, args);
        if(results.get(0) == null) {
            results.set(0, 0L);
        }
        
        boolean allowed = results.get(0) == 1;
        if (!allowed) {
            // LOG.info("user:"+user + " and result: "+results.toString());
            LOG.error("RateLimitError: 429");
            throw new RateLimitException("RateLimitError: 429");
        }
    }
}
