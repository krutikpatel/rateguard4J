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
  
    private TokenWindowRateLimiter() {}

    /**
     * Constructor for TokenWindowRateLimiter.
     *
     * @param builder the builder object containing the parameters for the TokenWindowRateLimiter
     * @throws IOException if an I/O error occurs
     */
    private TokenWindowRateLimiter(Builder builder) throws IOException {
        this.connection = builder.connection;
        //this.scriptingCommands = this.connection.sync();
        this.redisScriptLoader = new RedisScriptHelper(this.connection);
        this.tokenWindowParams = new TokenWindowParams(builder.replenishRate, builder.capacity);
    }

    /**
     * Builder class for TokenWindowRateLimiter.
     */
    public static class Builder {
        private StatefulRedisConnection<String, String> connection;
        private int capacity;
        private int replenishRate;

        /**
         * Sets the connection for the TokenWindowRateLimiter.
         *
         * @param connection the connection to set
         * @return the Builder instance
         */
        public Builder withConnection(StatefulRedisConnection<String, String> connection) {
            this.connection = connection;
            return this;
        }

        /**
         * Sets the capacity for the TokenWindowRateLimiter.
         *
         * @param capacity the capacity to set
         * @return the Builder instance
         */
        public Builder withCapacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        /**
         * Sets the replenish rate for the TokenWindowRateLimiter.
         *
         * @param replenishRate the replenish rate to set
         * @return the Builder instance
         */
        public Builder withReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
            return this;
        }

        /**
         * Builds a new instance of TokenWindowRateLimiter.
         *
         * @return a new instance of TokenWindowRateLimiter
         * @throws IOException if an I/O error occurs
         */
        public TokenWindowRateLimiter build() throws IOException {
            return new TokenWindowRateLimiter(this);
        }
    }

    /**
     * Performs rate limiting on multiple requests.
     *
     * @param clientId the clientId to perform rate limiting on
     * @param requestCount the number of requests
     * @throws RateLimitException if the rate limit is exceeded
     */
    @Override
    public void performRateLimitOnMultiRequest(String clientId, int requestCount) throws RateLimitException {
        String prefix = "request_rate_limiter." + clientId;
        //String prefix = "{request_rate_limiter." + clientId + "}"; prefix using hashtag to use in case of cluster. so that all keys are in same node
        String[] keys = {prefix + ".tokens", prefix + ".timestamp"};
        String[] args = TokenWindowScriptArgsFactory.getArgs(tokenWindowParams, requestCount);

        List<Long> results = redisScriptLoader.executeScript(SCRIPT_NAME, keys, args);
        if(results.get(0) == null) {
            results.set(0, 0L);
        }
        
        boolean allowed = results.get(0) == 1;
        if (!allowed) {
            // LOG.info("clientId:"+clientId + " and result: "+results.toString());
            LOG.error("RateLimitError: 429");
            throw new RateLimitException("RateLimitError: 429");
        }
    }

    /**
     * Performs rate limiting on a single request.
     *
     * @param clientId the clientId to perform rate limiting on
     * @throws RateLimitException if the rate limit is exceeded
     */
    @Override
    public void performRateLimit(String clientId) throws RateLimitException {
        String prefix = "request_rate_limiter." + clientId;
        //String prefix = "{request_rate_limiter." + clientId + "}"; prefix using hashtag to use in case of cluster. so that all keys are in same node
        String[] keys = {prefix + ".tokens", prefix + ".timestamp"};
        String[] args = TokenWindowScriptArgsFactory.getArgs(tokenWindowParams, 1);

        List<Long> results = redisScriptLoader.executeScript(SCRIPT_NAME, keys, args);
        if(results.get(0) == null) {
            results.set(0, 0L);
        }
        
        boolean allowed = results.get(0) == 1;
        if (!allowed) {
            // LOG.info("clientId:"+clientId + " and result: "+results.toString());
            LOG.error("RateLimitError: 429");
            throw new RateLimitException("RateLimitError: 429");
        }
    }
}
