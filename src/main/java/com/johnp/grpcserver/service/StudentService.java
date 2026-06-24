package com.johnp.grpcserver.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.johnp.grpc.*;
import com.johnp.grpcserver.bean.Course;
import com.johnp.grpcserver.bean.Enrollment;
import com.johnp.grpcserver.bean.Student;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@GrpcService
public class StudentService extends StudentsServiceGrpc.StudentsServiceImplBase {

    @Autowired
    private StudentProfileService studentProfileService;

    @Override
    public void getStudentProfile(StudentProfileRequest request, StreamObserver<StudentProfileResponse> responseObserver) {
        try {
            Student student = studentProfileService.findStudentWithEnrollments(request.getStudentId())
                    .orElse(null);

            if (student == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Student not found: " + request.getStudentId())
                        .asRuntimeException());
                return;
            }

            StudentProfileResponse studentProfileResponse = StudentProfileResponse.newBuilder()
                    .setStudentId(student.getStudentId())
                    .setFirstName(nullToEmpty(student.getFirstName()))
                    .setLastName(nullToEmpty(student.getLastName()))
                    .setEmail(nullToEmpty(student.getEmail()))
                    .setProgram(nullToEmpty(student.getProgram()))
                    .setCurrentSemester((int) student.getCurrentSemester())
                    .setGpa(student.getGpa())
                    .setEnrollmentDate(toTimestamp(student.getEnrollmentDate()))
                    .addAllEnrolledCourses(toEnrolledCourses(student))
                    .build();

            responseObserver.onNext(studentProfileResponse);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(ex.getMessage())
                    .withCause(ex)
                    .asRuntimeException());
        }
    }

    @Override
    public void streamCourseCatalog(Empty request, StreamObserver<CourseSummary> responseObserver) {
        try {
            List<Course> allCourses = studentProfileService.getAllCourses();
            allCourses.forEach(course -> {
                CourseSummary courseSummary = CourseSummary.newBuilder().setCourseId(course.getCourseId())
                        .setCourseName(nullToEmpty(course.getCourseName()))
                        .setCourseCode(nullToEmpty(course.getCourseCode()))
                        .setCredits(String.valueOf(course.getCredits()))
                        .build();
                try {
                    TimeUnit.SECONDS.sleep(4);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                responseObserver.onNext(courseSummary);
            });
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e.fillInStackTrace());
        }
    }

    @Override
    public StreamObserver<StudentEnrollmentRequest> batchConsumeEnrollStudents(
            StreamObserver<BatchEnrollStudentsResponse> responseObserver) {
        final int[] successCnt = {0};
        final int[] failedCnt = { 0 };
        ArrayList<String> failedRecordIds = new ArrayList<>();

        return new StreamObserver<StudentEnrollmentRequest>() {
            @Override
            public void onNext(StudentEnrollmentRequest studentEnrollmentRequest) {
                // Process each StudentEnrollmentRequest
                System.out.println("Received enrollment request for student ID: " + studentEnrollmentRequest.getStudentId());
                // Here you can add logic to save the enrollment request to the database or perform other operations
                try {
                    studentProfileService.insertEnrollment(studentEnrollmentRequest);
                    successCnt[0]++;
                } catch (Exception e) {
                    System.err.println("Error occurred while inserting enrollment for student "
                            + studentEnrollmentRequest.getStudentId() + ", course "
                            + studentEnrollmentRequest.getCourseId() + ": " + e.getMessage());
                    failedCnt[0]++;
                    failedRecordIds.add(studentEnrollmentRequest.getStudentId() + ":" + studentEnrollmentRequest.getCourseId());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error occurred in batch enrollment stream: " + throwable.getMessage());
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                BatchEnrollStudentsResponse response = BatchEnrollStudentsResponse.newBuilder()
                        .setSuccessCount(successCnt[0])
                        .setFailureCount(failedCnt[0])
                        .addAllFailedStudentIds(failedRecordIds)
                        .build();
                System.out.println("Batch enrollment finished. Success=" + successCnt[0]
                        + ", Failed=" + failedCnt[0]);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }



    private List<EnrolledCourse> toEnrolledCourses(Student student) {
        if (student.getEnrollments() == null) {
            return Collections.emptyList();
        }

        return student.getEnrollments().stream()
                .map(this::toEnrolledCourse)
                .toList();
    }

    private EnrolledCourse toEnrolledCourse(Enrollment enrollment) {
        return EnrolledCourse.newBuilder()
                .setCourseId(enrollment.getCourse().getCourseId())
                .setCourseName(nullToEmpty(enrollment.getCourse().getCourseName()))
                .setTerm(nullToEmpty(enrollment.getTerm()))
                .setStatus(nullToEmpty(enrollment.getStatus()))
                .setGrade((float) enrollment.getGrade())
                .build();
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Timestamp.getDefaultInstance();
        }

        return Timestamp.newBuilder()
                .setSeconds(dateTime.toEpochSecond(ZoneOffset.UTC))
                .setNanos(dateTime.getNano())
                .build();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
