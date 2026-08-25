package com.example.student.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {
    @Min(value = 1, message = "页码必须大于0")
    private Long current = 1L;

    @Min(value = 1, message = "页大小必须大于0")
    @Max(value = 100, message = "页大小不能超过100")
    private Long size = 10L;
}
