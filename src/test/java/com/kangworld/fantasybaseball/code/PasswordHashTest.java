package com.kangworld.fantasybaseball.code;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {

    @Test
    void bcrypt_생성() {
        System.out.println(new BCryptPasswordEncoder().encode("admin"));
    }

}