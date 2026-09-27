package br.com.cursosonline.progress.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Progresso de um aluno em um curso. Criado a partir do evento EnrollmentCreated, com uma cópia
 * local dos dados necessários (aluno, curso, total de aulas) para não depender do Enrollment Service.
 */
@Entity
@Table(name = "course_progress")
public class CourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long enrollmentId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String studentEmail;

    @Column(nullable = false)
    private String courseId;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private int totalLessons;

    private int completedLessons;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant updatedAt;

    private Instant completedAt;

    protected CourseProgress() {
    }

    public CourseProgress(Long enrollmentId, String studentId, String studentName, String studentEmail,
                          String courseId, String courseName, int totalLessons) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.courseId = courseId;
        this.courseName = courseName;
        this.totalLessons = totalLessons;
        this.status = ProgressStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.updatedAt = this.startedAt;
    }

    /** Registra mais uma aula concluída e marca o curso como concluído ao atingir o total. */
    public void registerLessonCompleted() {
        this.completedLessons++;
        this.updatedAt = Instant.now();
        if (completedLessons >= totalLessons) {
            this.status = ProgressStatus.COMPLETED;
            this.completedAt = this.updatedAt;
        }
    }

    public void cancel() {
        this.status = ProgressStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public int getProgressPercentage() {
        return totalLessons == 0 ? 0 : (completedLessons * 100) / totalLessons;
    }

    public boolean isCompleted() {
        return status == ProgressStatus.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public ProgressStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
