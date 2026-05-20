package com.kangworld.fantasybaseball.abuse.config;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbuseLimitPolicy {

    private int maxAttempts;
    private Duration window;
    private boolean failOpen;

    public static AbuseLimitPolicy of(int maxAttempts, Duration window, boolean failOpen) {
        AbuseLimitPolicy policy = new AbuseLimitPolicy();
        policy.setMaxAttempts(maxAttempts);
        policy.setWindow(window);
        policy.setFailOpen(failOpen);

        return policy;
    }

}


