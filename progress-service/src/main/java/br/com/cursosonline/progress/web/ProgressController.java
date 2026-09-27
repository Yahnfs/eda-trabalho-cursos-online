package br.com.cursosonline.progress.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.cursosonline.progress.service.ProgressService;
import br.com.cursosonline.progress.web.ProgressDtos.LessonResponse;
import br.com.cursosonline.progress.web.ProgressDtos.ProgressResponse;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService service;

    public ProgressController(ProgressService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProgressResponse> list(@RequestParam(required = false) String studentId) {
        return service.list(studentId).stream().map(ProgressResponse::from).toList();
    }

    @GetMapping("/{enrollmentId}")
    public ProgressResponse get(@PathVariable Long enrollmentId) {
        return ProgressResponse.from(service.findByEnrollment(enrollmentId));
    }

    @GetMapping("/{enrollmentId}/lessons")
    public List<LessonResponse> lessons(@PathVariable Long enrollmentId) {
        return service.lessons(enrollmentId).stream().map(LessonResponse::from).toList();
    }

    /** Conclui uma aula e publica {@code LessonCompleted} (e {@code CourseCompleted} na última aula). */
    @PostMapping("/{enrollmentId}/lessons/{lessonNumber}/complete")
    public ProgressResponse completeLesson(@PathVariable Long enrollmentId, @PathVariable int lessonNumber) {
        return ProgressResponse.from(service.completeLesson(enrollmentId, lessonNumber));
    }
}
