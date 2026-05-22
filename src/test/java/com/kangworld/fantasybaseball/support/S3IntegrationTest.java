package com.kangworld.fantasybaseball.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("aws-integration")
@SpringBootTest
@ActiveProfiles({"test", "s3-integration"})
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface S3IntegrationTest {
}
