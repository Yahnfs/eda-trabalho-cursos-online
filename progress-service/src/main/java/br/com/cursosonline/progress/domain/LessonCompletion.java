package br.com.cursosonline.progress.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Registro de uma aula concluída (uma aula só pode ser concluída uma vez por matrícula). */
@Entity
@Table(name = "lesson_completions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"enrollmentId", "lessonNumber"}))
public class LessonCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long enrollmentId;

    @Column(nullable = false)
    private int lessonNumber;

    @Column(nullable = false)
    private Instant completedAt;

    protected LessonCompletion() {
    }

    public LessonCompletion(Long enrollmentId, int lessonNumber) {
        this.enrollmentId = enrollmentId;
        this.lessonNumber = lessonNumber;
        this.completedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public int getLessonNumber() {
        return lessonNumber;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
