package com.johnp.grpcserver.bean;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "courses")
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_code", unique = true, nullable = false)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "credits")
    private Integer credits;

    @OneToMany(mappedBy = "course")
    private List<Enrollment> enrollments;
}
