package com.mes.application.command.typesetting;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppTypesettingServiceFormeCallbackLockTest {

    @Test
    void usesDedicatedLockThatDoesNotConflictWithConfirmOperation() {
        AppTypesettingService service = new AppTypesettingService();

        List<String> callbackLockKeys = ReflectionTestUtils.invokeMethod(
                service, "buildFormeCallbackLockKeys", "plate-id");

        assertThat(callbackLockKeys)
                .containsExactly("typesetting:forme:callback:lock:plate-id")
                .doesNotContain("typesetting:operation:lock:typesetting:plate-id");
    }

    @Test
    void doesNotCreateCallbackLockForBlankRecordId() {
        AppTypesettingService service = new AppTypesettingService();

        List<String> callbackLockKeys = ReflectionTestUtils.invokeMethod(
                service, "buildFormeCallbackLockKeys", " ");

        assertThat(callbackLockKeys).isEmpty();
    }
}
