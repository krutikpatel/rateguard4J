package com.jsn.rl.tokenwindowratelimiter.util;

public class TokenWindowParams {
    private int replenishRate;  // How many tokens are replenished in bucket per second
    private int capacity;       // Bucket capacity
/*
    public TokenWindowParams(int replenishRate) {
        this.replenishRate = replenishRate;
        this.capacity = replenishRate * 2;
    }
*/
    public TokenWindowParams(int replenishRate, int capacity) {
        this.replenishRate = replenishRate;
        this.capacity = capacity;
    }

    public int getReplenishRate() {
        return replenishRate;
    }

    public int getCapacity() {
        return capacity;
    }
}
