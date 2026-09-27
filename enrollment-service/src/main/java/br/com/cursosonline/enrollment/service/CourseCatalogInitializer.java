package br.com.cursosonline.enrollment.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.com.cursosonline.enrollment.domain.Course;
import br.com.cursosonline.enrollment.domain.CourseRepository;

/** Popula o catálogo de cursos na primeira execução (banco vazio). */
@Component
public class CourseCatalogInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CourseCatalogInitializer.class);

    private final CourseRepository courses;

    public CourseCatalogInitializer(CourseRepository courses) {
        this.courses = courses;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (courses.count() > 0) {
            return;
        }
        courses.saveAll(List.of(
                new Course("EDA-101", "Arquitetura Orientada a Eventos com RabbitMQ", 3, 12),
                new Course("JAVA-201", "Java 21 Moderno", 5, 20),
                new Course("SPRING-301", "Microsserviços com Spring Boot 4", 4, 16)));
        log.info("[CATALOG] Catálogo inicial de cursos criado ({} cursos)", courses.count());
    }
}
