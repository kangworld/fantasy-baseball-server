package com.kangworld.fantasybaseball;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FantasyBaseballApplication {

    public static void main(String[] args) {
        // 애플리케이션 진입점. 기본 설정 로딩과 웹 서버 기동을 시작.
        SpringApplication.run(FantasyBaseballApplication.class, args);
    }

}
