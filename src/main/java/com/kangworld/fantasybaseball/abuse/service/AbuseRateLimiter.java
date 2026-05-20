package com.kangworld.fantasybaseball.abuse.service;

import com.kangworld.fantasybaseball.abuse.config.AbuseLimitPolicy;

public interface AbuseRateLimiter {

    void check(String policyName, String key, AbuseLimitPolicy policy);

}


