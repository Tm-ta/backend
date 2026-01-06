package com.example.tmta.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {

    // Pointcut: com.example.tmta.controller 패키지 하위의 모든 클래스의 모든 메소드에 적용
    @Pointcut("within(com.example.tmta.controller..*)")
    public void controller() {
    }

    // Around Advice: 메소드 실행 전후에 로직을 수행
    @Around("controller()")
    public Object logApi(ProceedingJoinPoint pjp) throws Throwable {
        String signature = pjp.getSignature().toShortString();
        Object[] args = pjp.getArgs();
        log.info("[API-IN] {}({})", signature, StringUtils.arrayToCommaDelimitedString(args));

        // 실제 타겟 메소드 실행
        Object result = pjp.proceed();

        log.info("[API-OUT] {} -> {}", signature, result);
        return result;
    }
}
