package br.com.cursosonline.enrollment.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentIdOrderByIdDesc(String studentId);

    boolean existsByStudentIdAndCourseIdAndStatusNot(String studentId, String courseId, EnrollmentStatus status);
}
