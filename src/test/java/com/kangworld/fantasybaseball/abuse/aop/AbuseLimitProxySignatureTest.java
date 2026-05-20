package com.kangworld.fantasybaseball.abuse.aop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class AbuseLimitProxySignatureTest {

    private static final Logger log = LoggerFactory.getLogger(AbuseLimitProxySignatureTest.class);

    @Test
    void CGLIB_proxy에서_signature_declaringType_simpleName은_프록시_클래스가_아니라_대상_클래스다() {
        // given
        SignatureCaptureAspect aspect = new SignatureCaptureAspect();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestService());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(aspect);
        TestService proxy = proxyFactory.getProxy();

        // when
        proxy.login();

        // then
        assertThat(proxy.getClass().getSimpleName()).contains("SpringCGLIB");
        assertThat(aspect.getThisClassSimpleName()).contains("SpringCGLIB");
        assertThat(aspect.getTargetClassSimpleName()).isEqualTo("TestService");
        assertThat(aspect.getDeclaringTypeSimpleName()).isEqualTo("TestService");
    }

    @Test
    void JDK_proxy에서_signature_declaringType_simpleName은_프록시_클래스가_아니라_인터페이스다() {
        // given
        SignatureCaptureAspect aspect = new SignatureCaptureAspect();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestLoginService());
        proxyFactory.setInterfaces(LoginService.class);
        proxyFactory.addAspect(aspect);
        LoginService proxy = proxyFactory.getProxy();

        // when
        proxy.login();

        // then
        assertThat(proxy.getClass().getSimpleName()).contains("$Proxy");
        assertThat(aspect.getThisClassSimpleName()).contains("$Proxy");
        assertThat(aspect.getTargetClassSimpleName()).isEqualTo("TestLoginService");
        assertThat(aspect.getDeclaringTypeSimpleName()).isEqualTo("LoginService");
    }

    @Aspect
    static class SignatureCaptureAspect {

        private final AtomicReference<String> declaringTypeSimpleName = new AtomicReference<>();
        private final AtomicReference<String> thisClassSimpleName = new AtomicReference<>();
        private final AtomicReference<String> targetClassSimpleName = new AtomicReference<>();

        @Around("execution(* com.kangworld.fantasybaseball.abuse.aop.AbuseLimitProxySignatureTest..*.login(..))")
        Object capture(ProceedingJoinPoint joinPoint) throws Throwable {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            thisClassSimpleName.set(joinPoint.getThis().getClass().getSimpleName());
            targetClassSimpleName.set(joinPoint.getTarget().getClass().getSimpleName());
            declaringTypeSimpleName.set(signature.getDeclaringType().getSimpleName());

            return joinPoint.proceed();
        }

        String getDeclaringTypeSimpleName() {
            return declaringTypeSimpleName.get();
        }

        String getThisClassSimpleName() {
            return thisClassSimpleName.get();
        }

        String getTargetClassSimpleName() {
            return targetClassSimpleName.get();
        }

    }

    static class TestService {

        void login() {
        }

    }

    interface LoginService {

        void login();

    }

    static class TestLoginService implements LoginService {

        @Override
        public void login() {
        }

    }

}
