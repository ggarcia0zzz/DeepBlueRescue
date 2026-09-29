package com.deepblue.rescue.domain;

public enum RescueStatus {

    ADMITTED,

    UNDER_EVALUATION,

    IN_REHABILITATION,

    READY_FOR_RELEASE,

    RELEASED,

    CLOSED;

    public boolean canTransitionTo(RescueStatus target){
        return switch(this){
            case ADMITTED -> target == UNDER_EVALUATION;
            case UNDER_EVALUATION -> target == IN_REHABILITATION;
            case IN_REHABILITATION -> target == READY_FOR_RELEASE;
            case READY_FOR_RELEASE -> target == RELEASED;
            case RELEASED -> target == CLOSED;
            case CLOSED -> false;
        };
    }

    public boolean isUnderCare(){
        return this != RELEASED && this != CLOSED;
    }
}
