package br.com.cursosonline.certificate.web;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.cursosonline.certificate.domain.Certificate;
import br.com.cursosonline.certificate.service.CertificateService;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService service;

    public CertificateController(CertificateService service) {
        this.service = service;
    }

    @GetMapping
    public List<CertificateResponse> list(@RequestParam(required = false) String studentId) {
        return service.list(studentId).stream().map(CertificateResponse::from).toList();
    }

    /** Verificação pública de autenticidade pelo código do certificado. */
    @GetMapping("/{code}")
    public CertificateResponse get(@PathVariable String code) {
        return CertificateResponse.from(service.findByCode(code));
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public CertificateResponse byEnrollment(@PathVariable Long enrollmentId) {
        return CertificateResponse.from(service.findByEnrollment(enrollmentId));
    }

    public record CertificateResponse(
            Long id,
            String code,
            Long enrollmentId,
            String studentId,
            String studentName,
            String courseId,
            String courseName,
            int totalLessons,
            Instant courseCompletedAt,
            Instant issuedAt) {

        static CertificateResponse from(Certificate c) {
            return new CertificateResponse(c.getId(), c.getCode(), c.getEnrollmentId(), c.getStudentId(),
                    c.getStudentName(), c.getCourseId(), c.getCourseName(), c.getTotalLessons(),
                    c.getCourseCompletedAt(), c.getIssuedAt());
        }
    }
}
