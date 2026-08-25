package com.example.student.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionVersionPolicyTest {

    private final PermissionVersionPolicy policy = new PermissionVersionPolicy();

    @Test
    void rejectsTokensFromAnOlderPermissionVersion() {
        assertThat(policy.isCurrent(1, 2)).isFalse();
        assertThat(policy.isCurrent(2, 2)).isTrue();
    }

    @Test
    void allowsLegacyTokensWhenNoServerVersionHasBeenStored() {
        assertThat(policy.isCurrent(null, null)).isTrue();
        assertThat(policy.isCurrent(1, null)).isTrue();
    }
}
