package com.deepblue.rescue.exception;

import java.time.LocalDate;

public class InvalidRescueDateException extends BusinessException {

    public InvalidRescueDateException(LocalDate date) {
        super("Rescue date cannot be in the future: " + date);
    }
}
