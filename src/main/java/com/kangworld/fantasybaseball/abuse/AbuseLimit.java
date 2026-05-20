package com.kangworld.fantasybaseball.abuse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AbuseLimit {

    String policy() default "";

    String key();

    int maxAttempts() default 60;

    String window() default "1m";

    boolean failOpen() default false;

}

