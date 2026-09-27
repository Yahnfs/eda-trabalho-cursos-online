package br.com.cursosonline.certificate.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Certificado de conclusão. Um por matrícula (garantido por restrição única em enrollmentId). */
@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

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

    private Instant courseCompletedAt;

    @Column(nullable = false)
    private Instant issuedAt;

    protected Certificate() {
    }

    public Certificate(Long enrollmentId, String studentId, String studentName, String studentEmail,
                       String courseId, String courseName, int totalLessons, Instant courseCompletedAt) {
        this.code = generateCode();
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.courseId = courseId;
        this.courseName = courseName;
        this.totalLessons = totalLessons;
        this.courseCompletedAt = courseCompletedAt;
        this.issuedAt = Instant.now();
    }

    private static String generateCode() {
        return "CERT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
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

    public Instant getCourseCompletedAt() {
        return courseCompletedAt;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
