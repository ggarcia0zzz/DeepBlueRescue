package com.deepblue.rescue.exception;

import com.deepblue.rescue.domain.RescueStatus;

public class CaseNotUnderCareException extends BusinessException {

    public CaseNotUnderCareException(String caseCode, RescueStatus status) {
        super("Case %s is %s and no longer accepts treatments".formatted(caseCode, status));
    }
}
