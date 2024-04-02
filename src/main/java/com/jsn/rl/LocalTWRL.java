package com.jsn.rl;

public class LocalTWRL {
    Integer tokensInBucket;

    public void doRateLimiting(int replenishRate, int capacity, int reqCount, int nowInSecs) {
        //System.out.println("Hello World!");
        
        if(tokensInBucket == null) {
            tokensInBucket = capacity;
        }

        int fill_time = replenishRate/capacity;
        int last_tokens = tokensInBucket;
 
        int delta = Math.max(0, nowInSecs-0);
        int filledTokens = Math.min(capacity,  + (delta * replenishRate));
        tokensInBucket = Math.min(capacity, tokensInBucket + filledTokens);
        boolean allowed = tokensInBucket >= reqCount;
        //int newTokens = tokensInBucket;
        if (allowed) {
            tokensInBucket = tokensInBucket - reqCount;
        }
        //tokensInBucket = newTokens;

        System.out.println("allowed: " + allowed + " --tokensInBucket = "+ tokensInBucket + " --filledTokens:" + filledTokens + " --last_tokens: " + last_tokens + " --delta: " + delta );
        //System.out.println("tokensInBucket: " + tokensInBucket);
    }

    public void test() {
        int replenishRate = 5;
        int capacity = 5;
        //int reqCount = 10;
        int nowInSecs = 0;

        for (int i = 0; i < 50; i++) {
            doRateLimiting(replenishRate, capacity, 1, nowInSecs);
        }
        
    }
}
