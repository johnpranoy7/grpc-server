package com.johnp.grpcserver.service;

import com.johnp.grpcserver.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoEnrollmentResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoEnrollmentResetService.class);

    private final EnrollmentRepository enrollmentRepository;

    @Value("${demo.enrollment-reset.seed-max-id:6}")
    private long seedMaxEnrollmentId;

    public DemoEnrollmentResetService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public int resetDemoEnrollments() {
        long removed = enrollmentRepository.deleteByEnrollmentIdGreaterThan(seedMaxEnrollmentId);
        log.info("Demo enrollment reset: removed {} row(s) with enrollment_id > {}", removed, seedMaxEnrollmentId);
        return (int) removed;
    }
}
