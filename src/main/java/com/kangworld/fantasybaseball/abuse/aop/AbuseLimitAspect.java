package com.kangworld.fantasybaseball.abuse.aop;

import java.util.stream.IntStream;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.kangworld.fantasybaseball.abuse.AbuseLimit;
import com.kangworld.fantasybaseball.abuse.config.AbuseLimitPolicy;
import com.kangworld.fantasybaseball.abuse.exception.RateLimitExceededException;
import com.kangworld.fantasybaseball.abuse.exception.RateLimitUnavailableException;
import com.kangworld.fantasybaseball.abuse.service.AbuseRateLimiter;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "abuse", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AbuseLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(AbuseLimitAspect.class);

    private final AbuseRateLimiter abuseRateLimiter;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(abuseLimit)")
    public Object check(ProceedingJoinPoint joinPoint, AbuseLimit abuseLimit) throws Throwable {
        // 로그인, 인증번호 전송, 로스터 제출처럼 고위험 명령성 API는 @AbuseLimit 선언 지점의 세부 정책으로 제한한다.
        String key = resolveKey(joinPoint, abuseLimit.key());
        String policyName = resolvePolicyName(joinPoint, abuseLimit);
        AbuseLimitPolicy policy = AbuseLimitPolicy.of(
                abuseLimit.maxAttempts(),
                DurationStyle.detectAndParse(abuseLimit.window()),
                abuseLimit.failOpen()
        );

        try {
            abuseRateLimiter.check(policyName, key, policy);
        } catch (RateLimitUnavailableException exception) {
            // Redis 장애 시에도 조회성/저위험 정책은 fail-open으로 통과시키고, 고위험 정책은 차단한다.
            if (!policy.isFailOpen())
                throw exception;

            log.warn("rate limit check failed. policy={}, key={}, error={}", policyName, key, exception.getMessage());
        } catch (RateLimitExceededException exception) {
            log.warn("rate limit exceeded. policy={}, key={}", policyName, key);
            throw exception;
        }

        return joinPoint.proceed();
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null)
            IntStream.range(0, parameterNames.length)
                    .forEach(index -> context.setVariable(parameterNames[index], args[index]));

        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value == null)
            throw new IllegalArgumentException("rate limit key must not be null");

        return String.valueOf(value);
    }

    private String resolvePolicyName(ProceedingJoinPoint joinPoint, AbuseLimit abuseLimit) {
        if (StringUtils.hasText(abuseLimit.policy()))
            return abuseLimit.policy();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

}
