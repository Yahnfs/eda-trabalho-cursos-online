package br.com.cursosonline.progress.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, Long> {

    boolean existsByEnrollmentIdAndLessonNumber(Long enrollmentId, int lessonNumber);

    List<LessonCompletion> findByEnrollmentIdOrderByLessonNumber(Long enrollmentId);
}
