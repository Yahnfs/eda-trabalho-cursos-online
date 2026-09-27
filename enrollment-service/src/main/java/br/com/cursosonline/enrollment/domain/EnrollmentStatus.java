package br.com.cursosonline.enrollment.domain;

public enum EnrollmentStatus {
    /** Matrícula criada; aluno cursando. */
    ACTIVE,
    /** Todas as aulas concluídas (evento CourseCompleted recebido). */
    COMPLETED,
    /** Certificado emitido (evento CertificateIssued recebido). */
    CERTIFIED,
    /** Matrícula cancelada pelo aluno/administrador. */
    CANCELLED
}
