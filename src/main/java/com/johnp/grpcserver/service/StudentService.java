package com.johnp.grpcserver.service;

import com.google.protobuf.Timestamp;
import com.johnp.grpc.EnrolledCourse;
import com.johnp.grpc.StudentProfileRequest;
import com.johnp.grpc.StudentProfileResponse;
import com.johnp.grpc.StudentsServiceGrpc;
import com.johnp.grpcserver.bean.Enrollment;
import com.johnp.grpcserver.bean.Student;
import com.johnp.grpcserver.repository.StudentRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@GrpcService
public class StudentService extends StudentsServiceGrpc.StudentsServiceImplBase {

    @Autowired
    private StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public void getStudentProfile(StudentProfileRequest request, StreamObserver<StudentProfileResponse> responseObserver) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElse(null);

        if (student == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Student not found: " + request.getStudentId())
                    .asRuntimeException());
            return;
        }

        StudentProfileResponse studentProfileResponse = StudentProfileResponse.newBuilder()
                .setStudentId(student.getStudentId())
                .setFirstName(student.getFirstName())
                .setLastName(student.getLastName())
                .setEmail(student.getEmail())
                .setProgram(student.getProgram())
                .setCurrentSemester((int) student.getCurrentSemester())
                .setGpa(student.getGpa())
                .setEnrollmentDate(toTimestamp(student.getEnrollmentDate()))
                .addAllEnrolledCourses(student.getEnrollments().stream()
                        .map(this::toEnrolledCourse)
                        .toList())
                .build();

        responseObserver.onNext(studentProfileResponse);
        responseObserver.onCompleted();
    }

    private EnrolledCourse toEnrolledCourse(Enrollment enrollment) {
        return EnrolledCourse.newBuilder()
                .setCourseId(String.valueOf(enrollment.getCourse().getCourseId()))
                .setCourseName(enrollment.getCourse().getCourseName())
                .setTerm(enrollment.getTerm())
                .setStatus(enrollment.getStatus())
                .setGrade(enrollment.getGrade())
                .build();
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Timestamp.getDefaultInstance();
        }

        long epochSecond = dateTime.toEpochSecond(ZoneOffset.UTC);
        return Timestamp.newBuilder()
                .setSeconds(epochSecond)
                .setNanos(dateTime.getNano())
                .build();
    }
}
