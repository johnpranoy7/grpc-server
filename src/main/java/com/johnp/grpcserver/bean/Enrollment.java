package com.johnp.grpcserver.bean;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollment_student_course_term",
                columnNames = {"student_id", "course_id", "term"}))
@Data
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "term")
    private String term;

    @Column(name = "status")
    private String status;

    @Column(name = "grade")
    private double grade;
}
