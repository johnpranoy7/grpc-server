package com.johnp.grpcserver.service;

import com.johnp.grpcserver.bean.Student;
import com.johnp.grpcserver.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StudentProfileService {

    @Autowired
    private StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public Optional<Student> findStudentWithEnrollments(float studentId) {
        return studentRepository.findById(studentId)
                .map(student -> {
                    if (student.getEnrollments() != null) {
                        student.getEnrollments().forEach(enrollment -> enrollment.getCourse().getCourseName());
                    }
                    return student;
                });
    }
}
