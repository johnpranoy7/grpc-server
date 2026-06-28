package com.johnp.grpcserver.repository;

import com.johnp.grpcserver.bean.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudent_StudentIdAndCourse_CourseIdAndTerm(Long studentId, Long courseId, String term);

    long countByCourse_CourseId(Long courseId);

    long deleteByEnrollmentIdGreaterThan(Long enrollmentId);
}
