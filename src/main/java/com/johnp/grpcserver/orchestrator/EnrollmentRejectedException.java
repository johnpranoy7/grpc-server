package com.johnp.grpcserver.orchestrator;

public class EnrollmentRejectedException extends Exception {

    private final String reasonCode;

    public EnrollmentRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
