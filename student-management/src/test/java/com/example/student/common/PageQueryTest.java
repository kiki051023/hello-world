package com.example.student.common;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsPageSizesOverTheGlobalLimit() {
        PageQuery query = new PageQuery();
        query.setSize(101L);
        assertThat(validator.validate(query)).isNotEmpty();
    }
}
