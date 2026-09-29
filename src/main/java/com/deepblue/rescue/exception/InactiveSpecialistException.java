package com.deepblue.rescue.exception;

public class InactiveSpecialistException extends BusinessException {

    public InactiveSpecialistException(Long specialistId) {
        super("Specialist is inactive and cannot perform treatments: " + specialistId);
    }
}
