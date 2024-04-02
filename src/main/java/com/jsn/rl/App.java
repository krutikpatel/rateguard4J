package com.jsn.rl;

import com.jsn.rl.tokenwindowratelimiter.RateLimitException;
import com.jsn.rl.tokenwindowratelimiter.TokenWindowRateLimiter;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

/**
 * Hello world!
 *
 */
public class App {
/*
    public static void mainForCluster( String[] args ) throws RateLimitException {
        
        System.out.println( "Hello World!" );
        // Create a RedisClient instance
        RedisClusterClient redisClient = RedisClusterClient.create("redis://localhost:6379");

        // Create a connection
        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        //record time
        long startTime = System.currentTimeMillis();
        // Use the connection...
        try {
            TokenWindowRateLimiter2 trl2 = new TokenWindowRateLimiter2(connection);
            trl2.testCheckRequestRateLimiter();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total time taken: " + (endTime - startTime) + "ms");

        // Don't forget to close the connection and the client when you're done
        connection.close();
        redisClient.shutdown();

    }
*/
    //below method uses StatefulRedisConnection instead of StatefulRedisClusterConnection. for single node redis
    public static void main( String[] args ) throws RateLimitException {
        
        System.out.println( "Hello World!" );
        // Create a RedisClient instance
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        // Create a connection
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        //record time
        long startTime = System.currentTimeMillis();
        // Use the connection...
        try {
            TokenWindowRateLimiter trl2 = new TokenWindowRateLimiter(connection);
            trl2.performRateLimitOnMultiRequest("11",50);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total time taken: " + (endTime - startTime) + "ms");

    }
}
