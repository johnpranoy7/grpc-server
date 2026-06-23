-- Static course catalog
INSERT INTO courses (course_id, course_code, course_name, credits) VALUES
(1, 'MATH101', 'Mathematics', 3),
(2, 'PHYS201', 'Physics', 4),
(3, 'CHEM101', 'Chemistry', 3),
(4, 'BIO101', 'Biology', 4),
(5, 'CS101', 'Computer Science', 3);

-- Students (profile data only)
INSERT INTO students (student_id, first_name, last_name, email, program, current_semester, gpa, enrollment_date) VALUES
(1, 'John', 'Doe', 'johnDoe@email.com', 'Computer Science', 3, 3.5, '2023-01-10'),
(2, 'Jane', 'Smith', 'janeSmith@email.com', 'Engineering', 2, 3.8, '2023-02-15'),
(3, 'Alice', 'Johnson', 'aliceJohnson@email.com', 'Biology', 4, 3.2, '2023-03-20'),
(4, 'Bob', 'Brown', 'bobBrown@email.com', 'Mathematics', 1, 3.6, '2023-04-25');

-- Enrollments link students to courses (student_id + course_id FKs)
INSERT INTO enrollments (enrollment_id, student_id, course_id, term, status, grade) VALUES
(1, 1, 1, 'Fall 2022', 'Enrolled', 3.5),
(2, 1, 2, 'Spring 2023', 'Enrolled', 3.8),
(3, 2, 3, 'Summer 2023', 'Enrolled', 3.2),
(4, 2, 4, 'Fall 2023', 'Enrolled', 3.6),
(5, 3, 5, 'Spring 2024', 'Enrolled', 3.9),
(6, 3, 1, 'Spring 2024', 'Enrolled', 4.0);
