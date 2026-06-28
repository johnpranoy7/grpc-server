package com.johnp.grpcserver.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.johnp.grpc.*;
import com.johnp.grpcserver.bean.Course;
import com.johnp.grpcserver.bean.Enrollment;
import com.johnp.grpcserver.bean.Student;
import com.johnp.grpcserver.orchestrator.EnrollmentAdvisingResult;
import com.johnp.grpcserver.orchestrator.EnrollmentRejectedException;
import com.johnp.grpcserver.orchestrator.StudentDatabaseOrchestrator;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j
@GrpcService
public class StudentService extends StudentsServiceGrpc.StudentsServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    @Autowired
    private StudentDatabaseOrchestrator studentDatabaseOrchestrator;

    @Override
    public void getStudentProfile(StudentProfileRequest request, StreamObserver<StudentProfileResponse> responseObserver) {
        try {
            Student student = studentDatabaseOrchestrator.findStudentWithEnrollments(request.getStudentId())
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
    public void listStudentCatalog(Empty request, StreamObserver<StudentCatalogResponse> responseObserver) {
        try {
            StudentCatalogResponse.Builder builder = StudentCatalogResponse.newBuilder();
            studentDatabaseOrchestrator.getAllStudents().forEach(student ->
                    builder.addStudents(StudentSummary.newBuilder()
                            .setStudentId(student.getStudentId())
                            .setFirstName(nullToEmpty(student.getFirstName()))
                            .setLastName(nullToEmpty(student.getLastName()))
                            .setProgram(nullToEmpty(student.getProgram()))
                            .setGpa(student.getGpa())
                            .build()));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(ex.getMessage())
                    .withCause(ex)
                    .asRuntimeException());
        }
    }

    @Override
    public void listCourseCatalog(Empty request, StreamObserver<CourseCatalogResponse> responseObserver) {
        try {
            CourseCatalogResponse.Builder builder = CourseCatalogResponse.newBuilder();
            studentDatabaseOrchestrator.getAllCourses().forEach(course ->
                    builder.addCourses(CourseSummary.newBuilder()
                            .setCourseId(course.getCourseId())
                            .setCourseName(nullToEmpty(course.getCourseName()))
                            .setCourseCode(nullToEmpty(course.getCourseCode()))
                            .setCredits(String.valueOf(course.getCredits()))
                            .build()));
            responseObserver.onNext(builder.build());
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
            List<Course> allCourses = studentDatabaseOrchestrator.getAllCourses();
            allCourses.forEach(course -> {
                CourseSummary courseSummary = CourseSummary.newBuilder().setCourseId(course.getCourseId())
                        .setCourseName(nullToEmpty(course.getCourseName()))
                        .setCourseCode(nullToEmpty(course.getCourseCode()))
                        .setCredits(String.valueOf(course.getCredits()))
                        .build();
                try {
                    TimeUnit.SECONDS.sleep(2);
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
        ArrayList<FailedEnrollment> failures = new ArrayList<>();

        return new StreamObserver<StudentEnrollmentRequest>() {
            @Override
            public void onNext(StudentEnrollmentRequest studentEnrollmentRequest) {
                log.info("Received enrollment request for studentId={}, courseId={}",
                        studentEnrollmentRequest.getStudentId(), studentEnrollmentRequest.getCourseId());
                try {
                    studentDatabaseOrchestrator.insertEnrollment(studentEnrollmentRequest);
                    successCnt[0]++;
                } catch (EnrollmentRejectedException e) {
                    log.warn("Rejected enrollment for studentId={}, courseId={}: {}",
                            studentEnrollmentRequest.getStudentId(),
                            studentEnrollmentRequest.getCourseId(),
                            e.getMessage());
                    failedCnt[0]++;
                    failedRecordIds.add(studentEnrollmentRequest.getStudentId() + ":" + studentEnrollmentRequest.getCourseId());
                    failures.add(FailedEnrollment.newBuilder()
                            .setStudentId(studentEnrollmentRequest.getStudentId())
                            .setCourseId(studentEnrollmentRequest.getCourseId())
                            .setReasonCode(e.getReasonCode())
                            .setMessage(e.getMessage())
                            .build());
                } catch (Exception e) {
                    log.warn("Failed to insert enrollment for studentId={}, courseId={}: {}",
                            studentEnrollmentRequest.getStudentId(),
                            studentEnrollmentRequest.getCourseId(),
                            e.getMessage());
                    failedCnt[0]++;
                    failedRecordIds.add(studentEnrollmentRequest.getStudentId() + ":" + studentEnrollmentRequest.getCourseId());
                    failures.add(FailedEnrollment.newBuilder()
                            .setStudentId(studentEnrollmentRequest.getStudentId())
                            .setCourseId(studentEnrollmentRequest.getCourseId())
                            .setReasonCode("ERROR")
                            .setMessage(e.getMessage() != null ? e.getMessage() : "Unknown error")
                            .build());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error in batch enrollment stream", throwable);
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                BatchEnrollStudentsResponse response = BatchEnrollStudentsResponse.newBuilder()
                        .setSuccessCount(successCnt[0])
                        .setFailureCount(failedCnt[0])
                        .addAllFailedStudentIds(failedRecordIds)
                        .addAllFailures(failures)
                        .build();
                log.info("Batch enrollment finished: success={}, failed={}", successCnt[0], failedCnt[0]);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<EnrollmentIntent> liveEnrollmentAdvising(
            StreamObserver<EnrollmentFeedback> responseObserver) {
        return new StreamObserver<EnrollmentIntent>() {
            @Override
            public void onNext(EnrollmentIntent intent) {
                log.info("Advising intent received: studentId={}, courseId={}, term={}",
                        intent.getStudentId(), intent.getCourseId(), intent.getTerm());
                try {
                    EnrollmentAdvisingResult result = studentDatabaseOrchestrator.adviseEnrollment(intent);
                    EnrollmentFeedback feedback = EnrollmentFeedback.newBuilder()
                            .setStudentId(intent.getStudentId())
                            .setCourseId(intent.getCourseId())
                            .setStatus(result.status())
                            .setMessage(result.message())
                            .setProjectedGpa(result.projectedGpa())
                            .setPersisted(result.persisted())
                            .build();
                    log.info("Advising feedback: status={}, message={}", result.status(), result.message());
                    responseObserver.onNext(feedback);
                } catch (Exception ex) {
                    log.error("Advising error for studentId={}, courseId={}",
                            intent.getStudentId(), intent.getCourseId(), ex);
                    responseObserver.onNext(EnrollmentFeedback.newBuilder()
                            .setStudentId(intent.getStudentId())
                            .setCourseId(intent.getCourseId())
                            .setStatus("ERROR")
                            .setMessage(ex.getMessage())
                            .setPersisted(false)
                            .build());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Client closed advising stream with error", throwable);
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Advising session completed");
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
