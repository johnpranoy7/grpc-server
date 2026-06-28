package com.johnp.grpcserver.orchestrator;

import com.johnp.grpc.EnrollmentIntent;
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
public class StudentDatabaseOrchestrator {

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

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional
    public void insertEnrollment(StudentEnrollmentRequest enrollmentRequest) throws EnrollmentRejectedException {
        EnrollmentIntent intent = EnrollmentIntent.newBuilder()
                .setStudentId(enrollmentRequest.getStudentId())
                .setCourseId(enrollmentRequest.getCourseId())
                .setTerm(enrollmentRequest.getTerm())
                .setStatus(enrollmentRequest.getStatus())
                .setGrade(enrollmentRequest.getGrade())
                .build();

        EnrollmentAdvisingResult result = adviseEnrollment(intent);
        if (!result.persisted()) {
            throw new EnrollmentRejectedException(result.status(), result.message());
        }
    }

    private static final int MAX_SEATS_PER_COURSE = 3;
    private static final double GPA_WARNING_THRESHOLD = 2.5;

    @Transactional
    public EnrollmentAdvisingResult adviseEnrollment(EnrollmentIntent intent) {
        if (intent.getTerm() == null || intent.getTerm().isBlank()) {
            return new EnrollmentAdvisingResult(
                    "INVALID_TERM",
                    "Term is required (e.g. Fall 2024).",
                    0,
                    false);
        }

        Optional<Student> studentOptional = studentRepository.findById(intent.getStudentId());
        if (studentOptional.isEmpty()) {
            return new EnrollmentAdvisingResult(
                    "STUDENT_NOT_FOUND",
                    "No student found with id " + intent.getStudentId() + ".",
                    0,
                    false);
        }

        Optional<Course> courseOptional = courseRepository.findById(intent.getCourseId());
        if (courseOptional.isEmpty()) {
            return new EnrollmentAdvisingResult(
                    "COURSE_NOT_FOUND",
                    "No course found with id " + intent.getCourseId() + ".",
                    0,
                    false);
        }

        Student student = studentOptional.get();
        Course course = courseOptional.get();

        if (enrollmentRepository.existsByStudent_StudentIdAndCourse_CourseIdAndTerm(
                intent.getStudentId(), intent.getCourseId(), intent.getTerm())) {
            return new EnrollmentAdvisingResult(
                    "ALREADY_ENROLLED",
                    student.getFirstName() + " is already enrolled in " + course.getCourseName()
                            + " for " + intent.getTerm() + ".",
                    student.getGpa(),
                    false);
        }

        if (enrollmentRepository.countByCourse_CourseId(intent.getCourseId()) >= MAX_SEATS_PER_COURSE) {
            return new EnrollmentAdvisingResult(
                    "COURSE_FULL",
                    course.getCourseName() + " is full (" + MAX_SEATS_PER_COURSE + " seats).",
                    student.getGpa(),
                    false);
        }

        double projectedGpa = projectGpa(student.getGpa(), intent.getGrade());
        String status = intent.getGrade() < GPA_WARNING_THRESHOLD ? "GPA_WARNING" : "APPROVED";
        String message = status.equals("GPA_WARNING")
                ? "Enrolled with warning: grade " + intent.getGrade() + " may affect GPA."
                : "Enrolled in " + course.getCourseName() + " for " + intent.getTerm() + ".";

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setTerm(intent.getTerm());
        enrollment.setStatus(intent.getStatus().isBlank() ? "Enrolled" : intent.getStatus());
        enrollment.setGrade(intent.getGrade());
        enrollmentRepository.save(enrollment);

        return new EnrollmentAdvisingResult(status, message, projectedGpa, true);
    }

    private double projectGpa(double currentGpa, double newGrade) {
        return Math.round(((currentGpa + newGrade) / 2.0) * 100.0) / 100.0;
    }
}
