package com.johnp.grpcserver.scheduler;

import com.johnp.grpcserver.service.DemoEnrollmentResetService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic cleanup so demo enrollments do not exhaust course capacity.
 * Manual reset is also available via gRPC / BFF for cold-start deployments.
 */
@Component
@ConditionalOnProperty(name = "demo.enrollment-reset.enabled", havingValue = "true", matchIfMissing = true)
public class DemoEnrollmentResetScheduler {

    private final DemoEnrollmentResetService demoEnrollmentResetService;

    public DemoEnrollmentResetScheduler(DemoEnrollmentResetService demoEnrollmentResetService) {
        this.demoEnrollmentResetService = demoEnrollmentResetService;
    }

    @Scheduled(fixedDelayString = "${demo.enrollment-reset.interval-ms:1800000}")
    public void clearDemoEnrollments() {
        demoEnrollmentResetService.resetDemoEnrollments();
    }
}
