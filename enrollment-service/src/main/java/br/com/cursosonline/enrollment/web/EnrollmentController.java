package br.com.cursosonline.enrollment.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.cursosonline.enrollment.domain.Enrollment;
import br.com.cursosonline.enrollment.service.EnrollmentService;
import br.com.cursosonline.enrollment.web.EnrollmentDtos.CancelEnrollmentRequest;
import br.com.cursosonline.enrollment.web.EnrollmentDtos.CourseResponse;
import br.com.cursosonline.enrollment.web.EnrollmentDtos.CreateEnrollmentRequest;
import br.com.cursosonline.enrollment.web.EnrollmentDtos.EnrollmentResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    @GetMapping("/courses")
    public List<CourseResponse> courses() {
        return service.listCourses().stream().map(CourseResponse::from).toList();
    }

    /** Inicia o fluxo: cria a matrícula e publica {@code EnrollmentCreated}. */
    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResponse> create(@Valid @RequestBody CreateEnrollmentRequest request) {
        Enrollment enrollment = service.enroll(request.studentId(), request.studentName(),
                request.studentEmail(), request.courseId());
        return ResponseEntity.created(URI.create("/api/enrollments/" + enrollment.getId()))
                .body(EnrollmentResponse.from(enrollment));
    }

    @GetMapping("/enrollments")
    public List<EnrollmentResponse> list(@RequestParam(required = false) String studentId) {
        return service.list(studentId).stream().map(EnrollmentResponse::from).toList();
    }

    @GetMapping("/enrollments/{id}")
    public EnrollmentResponse get(@PathVariable Long id) {
        return EnrollmentResponse.from(service.findById(id));
    }

    @PostMapping("/enrollments/{id}/cancel")
    public EnrollmentResponse cancel(@PathVariable Long id,
                                     @Valid @RequestBody(required = false) CancelEnrollmentRequest request) {
        String reason = request != null ? request.reason() : null;
        return EnrollmentResponse.from(service.cancel(id, reason));
    }
}
