package com.jsn.rl.tokenwindowratelimiter;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

public class TokenWindowRateLimiterTest {
    private static final Logger LOG = LoggerFactory.getLogger(TokenWindowRateLimiterTest.class);
    
    /*
     * Use this test with a single node redis
     */
    @Test
    public void testCheckRequestRateLimiter() {
        // Create a RedisClient instance
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        // Create a connection
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        //record time
        long startTime = System.currentTimeMillis();
        // Use the connection...
        try {
            RateLimiter trl = new TokenWindowRateLimiter(connection);
            testCheckRequestRateLimiter(trl);
        } catch (Exception e) {
            LOG.error("Exception "+e);
        }

        long endTime = System.currentTimeMillis();
        LOG.info("Total time taken: " + (endTime - startTime) + "ms");
    }

    /*
     * knote: to see behave within a second. still depends if test started when within that second. 
     * knote: test will fail if second num changed during the loop run
     */
    private  void testCheckRequestRateLimiter(RateLimiter trl) throws RateLimitException {
        String id = "11";//String.valueOf((int) (Math.random() * 1000000));
        int replenishRate = 50;
        int reqCount = replenishRate*2; //consume till the capacity
        
        //consume till the capacity
        //lets start when second starts. its very important to start at the beginning of the second so that refill does not happen
        long currentTimeSecs = System.currentTimeMillis()/1000;
        while(System.currentTimeMillis()/1000 == currentTimeSecs) {
            //do nothing
        }

        //V IMP - test has to finish loop before the refill happens(which will be change in seconds)
        //but in general it works
        for (int i = 0; i < reqCount; i++) {
            trl.performRateLimitOnMultiRequest(id,1);
        }

        //try one more time, it should fail
        try {
            trl.performRateLimitOnMultiRequest(id,1);
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
            trl.performRateLimitOnMultiRequest(id,1);
        }

        //try one more time, it should fail
        try {
            trl.performRateLimitOnMultiRequest(id,1);
            throw new RuntimeException("it didn't throw RateLimitException :(");
        } catch (RateLimitException e) {
            LOG.info("it correctly threw RateLimitException");
        }

    }
}
