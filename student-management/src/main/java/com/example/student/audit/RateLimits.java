package com.example.student.audit;

import java.lang.annotation.*;

/** @RateLimit 重复标注容器（M1-2）。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimits {
    RateLimit[] value();
}