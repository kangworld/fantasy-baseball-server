package com.kangworld.fantasybaseball.member.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MemberProperties.class)
public class MemberConfig {
}
