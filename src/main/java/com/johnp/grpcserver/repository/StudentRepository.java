package com.johnp.grpcserver.repository;

import com.johnp.grpcserver.bean.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Float> {
}
