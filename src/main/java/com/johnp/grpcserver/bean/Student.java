package com.johnp.grpcserver.bean;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "students")
@Data
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "student_id", unique = true, nullable = false)
    private Long studentId;

    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "email")
    private String email;
    @Column(name = "program")
    private String program;
    @Column(name = "current_semester")
    private float currentSemester;
    @Column(name = "gpa")
    private double gpa;
    @Column(name = "enrollment_date")
    private LocalDateTime enrollmentDate;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments;
}
