package com.jsn.rl;

import com.jsn.rl.tokenwindowratelimiter.RateLimitException;
import com.jsn.rl.tokenwindowratelimiter.TokenWindowRateLimiter2;

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
            TokenWindowRateLimiter2 trl2 = new TokenWindowRateLimiter2(connection);
            trl2.testCheckRequestRateLimiter();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total time taken: " + (endTime - startTime) + "ms");

        /*
       TRL2 is slwoer than TRL. TRL2 is reusing loaded script. TRL is loading script every time.

        //trl.testCheckRequestRateLimiter2(); to see behave over seconds 
        trl.testCheckRequestRateLimiter();//to see behave within a second. still depends if test started when within that second. test will fail if second num changed during the loop run

        
        //sleep for 2 seconds
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TokenWindowRateLimiter trl = new TokenWindowRateLimiter(connection);
        startTime = System.currentTimeMillis();
        trl.testCheckRequestRateLimiter();//to see behave within a second. still depends if test started when within that second. test will fail if second num changed during the loop run
        endTime = System.currentTimeMillis();
        System.out.println("Total time taken: " + (endTime - startTime) + "ms");


        // Don't forget to close the connection and the client when you're done
        connection.close();
        redisClient.shutdown();
        */  

    }
}
