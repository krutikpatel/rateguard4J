package com.jsn.rl;

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

import com.jsn.rl.tokenwindowratelimiter.RateLimitException;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisScriptingCommands;
import reactor.core.publisher.Mono;

//import javax.annotation.concurrent.ThreadSafe;
//@ThreadSafe
public class TokenWindowRateLimiter {
    private static final Logger LOG = LoggerFactory.getLogger(TokenWindowRateLimiter.class);
    
    StatefulRedisConnection<String, String> connection;
    RedisScriptingCommands<String, String> scriptingCommands;

    private static final int REPLENISH_RATE = 50;// How many requests per second do you want a user to be allowed to do?
    private static final int CAPACITY = 2 * REPLENISH_RATE;// How much bursting do you want to allow?
    private static final String SCRIPT;
    private List<List<Long>> errorStates = new ArrayList<>();

    static {
        try {
            //SCRIPT = new String(Files.readAllBytes(Paths.get("TokenBucket.lua")));
            //get file from claspath
            //SCRIPT = new String(Files.readAllBytes(Paths.get(TokenWindowRateLimiter.class.getClassLoader().getResource("orig.lua").getFile())));
            InputStream stream = TokenWindowRateLimiter.class.getClassLoader().getResourceAsStream("orig.lua");
            if (stream == null) {
                throw new FileNotFoundException("Resource not found: orig.lua");
            }
            SCRIPT = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TokenWindowRateLimiter(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
        this.scriptingCommands = connection.sync();
    }

    public void checkRequestRateLimiter(String user) throws RateLimitException {
        String prefix = "request_rate_limiter." + user;
        List<String> keys = Arrays.asList(prefix + ".tokens", prefix + ".timestamp");

        //create array of above args
        String[] args = new String[]{String.valueOf(REPLENISH_RATE), 
                                        String.valueOf(CAPACITY), 
                                        String.valueOf(System.currentTimeMillis()/1000), 
                                        "1"
                                        //String.valueOf(CAPACITY)
                                    };
        
            List<Long> results = scriptingCommands.eval(SCRIPT, ScriptOutputType.MULTI, keys.toArray(new String[0]), args);
            /*
             * knote:
             * In Lua, false is a distinct value from nil. However, when Lettuce translates Lua's false to Java, it becomes null. 
             * This is because Java does not have a direct equivalent of Lua's false when dealing with non-primitive types (like Object or Long).
                So, if allowed is false in your Lua script, you will see null as the corresponding value in the List in your Java 
                code when using ScriptOutputType.MULTI.
             */
            if(results.get(0) == null) {
                results.set(0, 0L);
            }
            //LOG.info("user:"+user + " and result: "+results.toString());
            boolean allowed = results.get(0) == 1;
            if (!allowed) {
                LOG.error("RateLimitError: 429");
                errorStates.add(results);
                throw new RateLimitException("RateLimitError: 429");
            }
        /*
        } catch (Exception e) {
            LOG.error("Redis failed: " + e);
        }
        */
    }

    public void testCheckRequestRateLimiter2() throws RateLimitException {
        String id = "11";//String.valueOf((int) (Math.random() * 1000000));
        int reqCount = CAPACITY;//*10;

        //run thread for 5 seconds
        int exceptionCount = 0;
        int reqMade = 0;

        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - startTime < 3000) {
            try {
                checkRequestRateLimiter(id);
            } catch (RateLimitException e) {
                LOG.error(e.getMessage());
                exceptionCount++;
            }
            reqMade++;
        }
        LOG.info("exceptionCount: " + exceptionCount + " and reqMade: " + reqMade);
        LOG.info("errorStates: " + errorStates);
    
    }

    /*
     * knote: to see behave within a second. still depends if test started when within that second. 
     * knote: test will fail if second num changed during the loop run
     */
    public void testCheckRequestRateLimiter() throws RateLimitException {
        String id = "11";//String.valueOf((int) (Math.random() * 1000000));
        int reqCount = CAPACITY;//*10;
        //consume till the capacity

        //lets start when second starts. its very important to start at the beginning of the second so that refill does not happen
        long currentTimeSecs = System.currentTimeMillis()/1000;
        while(System.currentTimeMillis()/1000 == currentTimeSecs) {
            //do nothing
        }

        //V IMP - test has to finish loop before the refill happens(which will be change in seconds)
        //but in general it works
        for (int i = 0; i < reqCount; i++) {
            checkRequestRateLimiter(id);
        }

        //try one more time, it should fail
        try {
            checkRequestRateLimiter(id);
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
        for (int i = 0; i < REPLENISH_RATE; i++) {
            checkRequestRateLimiter(id);
        }

        //try one more time, it should fail
        try {
            checkRequestRateLimiter(id);
            throw new RuntimeException("it didn't throw RateLimitException :(");
        } catch (RateLimitException e) {
            LOG.info("it correctly threw RateLimitException");
        }

    }
}
