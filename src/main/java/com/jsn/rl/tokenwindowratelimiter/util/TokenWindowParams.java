package com.jsn.rl.tokenwindowratelimiter.util;

public class TokenWindowParams {
    private int replenishRate;// How many requests per second do you want a user to be allowed to do?
    private int capacity;    // How much bursting do you want to allow?

    public TokenWindowParams(int replenishRate) {
        this.replenishRate = replenishRate;
        this.capacity = replenishRate * 2;
    }

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
