package com.jsn.rl.tokenwindowratelimiter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsn.rl.TokenWindowRateLimiter;
import com.jsn.rl.tokenwindowratelimiter.util.RedisScriptHelper;
import com.jsn.rl.tokenwindowratelimiter.util.TokenWindowParamProvider;
import com.jsn.rl.tokenwindowratelimiter.util.TokenWindowScriptArgsProvider;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisScriptingCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import reactor.core.publisher.Mono;

//import javax.annotation.concurrent.ThreadSafe;
//@ThreadSafe
public class TokenWindowRateLimiter2 implements RateLimiter {
    private static final Logger LOG = LoggerFactory.getLogger(TokenWindowRateLimiter2.class);
    private static final String SCRIPT_NAME = "orig.lua";
    //private static final String SCRIPT_PATH = TokenWindowRateLimiter2.class.getClassLoader().getResource(SCRIPT_NAME).getPath();

    private TokenWindowParamProvider tokenWindowParamProvider = new TokenWindowParamProvider();
    private TokenWindowScriptArgsProvider tokenWindowScriptArgsProvider = new TokenWindowScriptArgsProvider();
    private RedisScriptHelper redisScriptLoader;
    
    private StatefulRedisConnection<String, String> connection;// StatefulRedisConnection OR StatefulRedisClusterConnection
    private RedisScriptingCommands<String, String> scriptingCommands;
    

    public TokenWindowRateLimiter2(StatefulRedisConnection<String, String> connection) throws IOException {
        this.connection = connection;
        this.scriptingCommands = connection.sync();
        redisScriptLoader = new RedisScriptHelper(connection);
        redisScriptLoader.loadScript(SCRIPT_NAME);
    }

    @Override
    public void performRateLimitOnRequest(String user, int requestCount) throws RateLimitException {
        String prefix = "request_rate_limiter." + user;
        //String prefix = "{request_rate_limiter." + user + "}"; prefix using hashtag to use in case of cluster. so that all keys are in same node
        String[] keys = {prefix + ".tokens", prefix + ".timestamp"};
        String[] args = tokenWindowScriptArgsProvider.getArgs(user, tokenWindowParamProvider.getDefaultParams(), requestCount);

        List<Long> results = redisScriptLoader.executeScript(SCRIPT_NAME, keys, args);
        if(results.get(0) == null) {
            results.set(0, 0L);
        }
        
        boolean allowed = results.get(0) == 1;
        if (!allowed) {
            LOG.info("user:"+user + " and result: "+results.toString());
            LOG.error("RateLimitError: 429");
            //errorStates.add(results);
            throw new RateLimitException("RateLimitError: 429");
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
     * knote: to see behave within a second. still depends if test started when within that second. 
     * knote: test will fail if second num changed during the loop run
     */
    public void testCheckRequestRateLimiter() throws RateLimitException {
        String id = "11";//String.valueOf((int) (Math.random() * 1000000));
        int reqCount = tokenWindowParamProvider.getDefaultParams().getCapacity(); //consume till the capacity
        int replenishRate = tokenWindowParamProvider.getDefaultParams().getReplenishRate();
        //consume till the capacity

        //lets start when second starts. its very important to start at the beginning of the second so that refill does not happen
        long currentTimeSecs = System.currentTimeMillis()/1000;
        while(System.currentTimeMillis()/1000 == currentTimeSecs) {
            //do nothing
        }

        //V IMP - test has to finish loop before the refill happens(which will be change in seconds)
        //but in general it works
        for (int i = 0; i < reqCount; i++) {
            performRateLimitOnRequest(id,1);
        }

        //try one more time, it should fail
        try {
            performRateLimitOnRequest(id,1);
            throw new RuntimeException("it didn't throw :(");
        } catch (RateLimitException e) {
            LOG.info("it correctly threw RateLimitException");
        }

        //sleep for a second, so that the window is replenished
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //try again, it should pass
        for (int i = 0; i < replenishRate; i++) {
            performRateLimitOnRequest(id,1);
        }

        //try one more time, it should fail
        try {
            performRateLimitOnRequest(id,1);
            throw new RuntimeException("it didn't throw RateLimitException :(");
        } catch (RateLimitException e) {
            LOG.info("it correctly threw RateLimitException");
        }

    }
}
