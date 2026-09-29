package com.deepblue.rescue.exception;

import com.deepblue.rescue.domain.RescueStatus;

public class InvalidStatusTransitionException extends BusinessException {

    public InvalidStatusTransitionException(RescueStatus from, RescueStatus to) {
        super("Invalid rescue status transition: %s -> %s".formatted(from, to));
    }
}
