package com.quark.autosave.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExecutionGuardServiceTest {

    @Test
    void shouldRejectSecondManualRunWhileFirstIsActive() {
        ExecutionGuardService service = new ExecutionGuardService();

        assertThat(service.tryAcquire()).isTrue();
        assertThat(service.tryAcquire()).isFalse();

        service.release();

        assertThat(service.tryAcquire()).isTrue();
    }
}
