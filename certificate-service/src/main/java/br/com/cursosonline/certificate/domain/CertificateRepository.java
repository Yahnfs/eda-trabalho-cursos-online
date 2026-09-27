package br.com.cursosonline.certificate.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCode(String code);

    Optional<Certificate> findByEnrollmentId(Long enrollmentId);

    List<Certificate> findByStudentIdOrderByIdDesc(String studentId);
}
