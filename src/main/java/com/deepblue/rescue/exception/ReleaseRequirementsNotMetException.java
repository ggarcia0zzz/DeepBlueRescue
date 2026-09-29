package com.deepblue.rescue.exception;

public class ReleaseRequirementsNotMetException extends BusinessException {

    public ReleaseRequirementsNotMetException(String caseCode) {
        super("Case %s cannot be READY_FOR_RELEASE without at least one treatment".formatted(caseCode));
    }
}
