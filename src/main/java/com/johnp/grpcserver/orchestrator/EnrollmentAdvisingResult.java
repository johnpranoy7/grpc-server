package com.johnp.grpcserver.orchestrator;

public record EnrollmentAdvisingResult(
        String status,
        String message,
        double projectedGpa,
        boolean persisted
) {
}
