package br.com.cursosonline.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Curso disponível no catálogo. O número de aulas define quando o curso é considerado concluído. */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int totalLessons;

    @Column(nullable = false)
    private int workloadHours;

    protected Course() {
    }

    public Course(String id, String name, int totalLessons, int workloadHours) {
        this.id = id;
        this.name = name;
        this.totalLessons = totalLessons;
        this.workloadHours = workloadHours;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public int getWorkloadHours() {
        return workloadHours;
    }
}
