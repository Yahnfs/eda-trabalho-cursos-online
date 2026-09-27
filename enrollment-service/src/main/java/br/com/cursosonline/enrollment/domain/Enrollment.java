package br.com.cursosonline.enrollment.domain;

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
 * Matrícula de um aluno em um curso. Além dos dados da matrícula, mantém uma visão local do
 * progresso e do certificado, atualizada de forma assíncrona pelos eventos dos outros serviços.
 */
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    private int completedLessons;

    private int progressPercentage;

    private String certificateCode;

    private String cancellationReason;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant completedAt;

    protected Enrollment() {
    }

    public Enrollment(String studentId, String studentName, String studentEmail, Course course) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.courseId = course.getId();
        this.courseName = course.getName();
        this.totalLessons = course.getTotalLessons();
        this.status = EnrollmentStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE;
    }

    public void cancel(String reason) {
        this.status = EnrollmentStatus.CANCELLED;
        this.cancellationReason = reason;
        this.updatedAt = Instant.now();
    }

    /**
     * Atualiza a visão de progresso. Ignora valores menores que o atual para tolerar eventos
     * entregues fora de ordem.
     */
    public boolean registerProgress(int completedLessons, int progressPercentage) {
        if (completedLessons <= this.completedLessons) {
            return false;
        }
        this.completedLessons = completedLessons;
        this.progressPercentage = progressPercentage;
        this.updatedAt = Instant.now();
        return true;
    }

    public void markCompleted(Instant completedAt) {
        this.status = EnrollmentStatus.COMPLETED;
        this.completedLessons = totalLessons;
        this.progressPercentage = 100;
        this.completedAt = completedAt;
        this.updatedAt = Instant.now();
    }

    public void markCertified(String certificateCode) {
        this.status = EnrollmentStatus.CERTIFIED;
        this.certificateCode = certificateCode;
        this.completedLessons = totalLessons;
        this.progressPercentage = 100;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
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

    public EnrollmentStatus getStatus() {
        return status;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
