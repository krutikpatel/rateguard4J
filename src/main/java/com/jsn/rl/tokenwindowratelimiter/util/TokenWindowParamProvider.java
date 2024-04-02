package com.jsn.rl.tokenwindowratelimiter.util;

public class TokenWindowParamProvider {
    
    public TokenWindowParams getDefaultParams() {
        return new TokenWindowParams(50); //replenishrate, capacity
    }

}
