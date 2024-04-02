package com.jsn.rl.tokenwindowratelimiter.util;

public class TokenWindowScriptArgsProvider {
    
    public String[] getArgs(String user, TokenWindowParams params, int requestCount) {
        //create array directly
        return new String[] {String.valueOf(params.getReplenishRate()), 
            String.valueOf(params.getCapacity()), 
            String.valueOf(System.currentTimeMillis()/1000), //time in seconds
            String.valueOf(requestCount)};
    }
}
