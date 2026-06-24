package com.johnp.grpcserver.service;

import com.johnp.grpc.StudentEnrollmentRequest;
import com.johnp.grpcserver.bean.Course;
import com.johnp.grpcserver.bean.Enrollment;
import com.johnp.grpcserver.bean.Student;
import com.johnp.grpcserver.repository.CourseRepository;
import com.johnp.grpcserver.repository.EnrollmentRepository;
import com.johnp.grpcserver.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentProfileService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public Optional<Student> findStudentWithEnrollments(long studentId) {
        return studentRepository.findById(studentId)
                .map(student -> {
                    if (student.getEnrollments() != null) {
                        student.getEnrollments().forEach(enrollment -> enrollment.getCourse().getCourseName());
                    }
                    return student;
                });
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional
    public void insertEnrollment(StudentEnrollmentRequest enrollmentRequest) throws Exception {

        boolean isStudentPresent = studentRepository.findById(enrollmentRequest.getStudentId()).isPresent();
        boolean isCoursePresent = courseRepository.findById(enrollmentRequest.getCourseId()).isPresent();

        if (isStudentPresent && isCoursePresent) {
            Student student = studentRepository.findById(enrollmentRequest.getStudentId()).get();
            Course course = courseRepository.findById(enrollmentRequest.getCourseId()).get();

            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);
            enrollment.setTerm(enrollmentRequest.getTerm());
            enrollment.setStatus(enrollmentRequest.getStatus());
            enrollment.setGrade(enrollmentRequest.getGrade());
            enrollmentRepository.save(enrollment);
        } else {
            throw new Exception("Unable to enroll student in course");
        }


    }


}
