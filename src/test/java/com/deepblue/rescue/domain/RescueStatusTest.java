package com.deepblue.rescue.domain;

import com.deepblue.rescue.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RescueStatusTest {

    @ParameterizedTest
    @CsvSource({
            "ADMITTED, UNDER_EVALUATION",
            "UNDER_EVALUATION, IN_REHABILITATION",
            "IN_REHABILITATION, READY_FOR_RELEASE",
            "READY_FOR_RELEASE, RELEASED",
            "RELEASED, CLOSED"
    })
    void allowsTheNextStepOfTheFlow(RescueStatus from, RescueStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "ADMITTED, RELEASED",
            "ADMITTED, CLOSED",
            "UNDER_EVALUATION, ADMITTED",
            "IN_REHABILITATION, RELEASED",
            "RELEASED, READY_FOR_RELEASE",
            "CLOSED, ADMITTED",
            "CLOSED, CLOSED"
    })
    void rejectsSkippingOrGoingBackwards(RescueStatus from, RescueStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void onlyReleasedAndClosedAreNotUnderCare() {
        assertThat(RescueStatus.ADMITTED.isUnderCare()).isTrue();
        assertThat(RescueStatus.IN_REHABILITATION.isUnderCare()).isTrue();
        assertThat(RescueStatus.READY_FOR_RELEASE.isUnderCare()).isTrue();
        assertThat(RescueStatus.RELEASED.isUnderCare()).isFalse();
        assertThat(RescueStatus.CLOSED.isUnderCare()).isFalse();
    }

    @Test
    void rescueCaseOpensAsAdmittedAndRejectsInvalidTransitions() {
        RescueCase rescueCase = RescueCase.open("RES-1", LocalDate.of(2026, 9, 1), "Playa Blanca");

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.ADMITTED);

        assertThatThrownBy(() -> rescueCase.changeStatus(RescueStatus.RELEASED))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.ADMITTED);
    }
}
