package com.example.student.audit;

import java.lang.annotation.*;

/** 接口限流注解（M1-2）：作用于 Controller 方法，按 bucketKey 每分钟限次。支持重复标注（多层维度限流）。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(RateLimits.class)
public @interface RateLimit {

    /** 限流 bucket 名（业务语义），最终 key 为 rate:{bucket}:{key} */
    String bucket();

    /** 每分钟允许的最大次数 */
    int limit() default 5;

    /** 附加维度 key：空则按 IP，否则按 SpEL 表达式（如 #loginDTO.username）解析 */
    String key() default "";

    /** 被限流时的提示语 */
    String message() default "操作过于频繁，请稍后再试";
}