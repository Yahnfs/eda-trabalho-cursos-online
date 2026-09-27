#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Demonstração ponta a ponta do fluxo orientado a eventos.
# Pré-requisito: RabbitMQ + 4 serviços em execução (docker compose up -d --build).
#
#   ./scripts/demo.sh           -> fluxo completo: matrícula -> aulas -> certificado
#   ./scripts/demo.sh --falha   -> também demonstra retry com backoff + DLQ
# -----------------------------------------------------------------------------
set -euo pipefail

ENROLLMENT=${ENROLLMENT_URL:-http://localhost:8081}
PROGRESS=${PROGRESS_URL:-http://localhost:8082}
CERTIFICATE=${CERTIFICATE_URL:-http://localhost:8083}
NOTIFICATION=${NOTIFICATION_URL:-http://localhost:8084}
RABBIT=${RABBIT_URL:-http://localhost:15672}

JSON='Content-Type: application/json'
STUDENT="aluno-$RANDOM"
EMAIL="$STUDENT@email.com"

step() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }

step "1. Catálogo de cursos (Enrollment Service)"
curl -s "$ENROLLMENT/api/courses"; echo

step "2. POST /api/enrollments -> publica EnrollmentCreated (topic: enrollment.created)"
RESPONSE=$(curl -s -X POST "$ENROLLMENT/api/enrollments" -H "$JSON" \
  -d "{\"studentId\":\"$STUDENT\",\"studentName\":\"Aluno Demo\",\"studentEmail\":\"$EMAIL\",\"courseId\":\"EDA-101\"}")
echo "$RESPONSE"
ENROLLMENT_ID=$(echo "$RESPONSE" | sed -E 's/^\{"id":([0-9]+).*/\1/')
sleep 2

step "3. Progress Service criou o acompanhamento ao consumir EnrollmentCreated"
curl -s "$PROGRESS/api/progress/$ENROLLMENT_ID"; echo

step "4. Concluindo as 3 aulas -> LessonCompleted (x3) e CourseCompleted na última"
for LESSON in 1 2 3; do
  curl -s -X POST "$PROGRESS/api/progress/$ENROLLMENT_ID/lessons/$LESSON/complete"; echo
done
sleep 3

step "5. Certificate Service emitiu o certificado (CertificateIssued via fanout)"
curl -s "$CERTIFICATE/api/certificates/enrollment/$ENROLLMENT_ID"; echo

step "6. Enrollment Service atualizou a matrícula para CERTIFIED"
curl -s "$ENROLLMENT/api/enrollments/$ENROLLMENT_ID"; echo

step "7. Notification Service registrou as notificações do aluno"
curl -s "$NOTIFICATION/api/notifications?recipient=$EMAIL"; echo

step "8. Tratamento de erros: aula repetida (409) e matrícula duplicada (409)"
curl -s -X POST "$PROGRESS/api/progress/$ENROLLMENT_ID/lessons/1/complete"; echo
curl -s -X POST "$ENROLLMENT/api/enrollments" -H "$JSON" \
  -d "{\"studentId\":\"$STUDENT\",\"studentName\":\"Aluno Demo\",\"studentEmail\":\"$EMAIL\",\"courseId\":\"EDA-101\"}"; echo

if [[ "${1:-}" == "--falha" ]]; then
  step "9. Retry + DLQ: e-mail @falha.com falha no Notification Service (1s, 2s, 4s -> DLQ)"
  curl -s -X POST "$ENROLLMENT/api/enrollments" -H "$JSON" \
    -d "{\"studentId\":\"$STUDENT-f\",\"studentName\":\"Aluno Falha\",\"studentEmail\":\"$STUDENT@falha.com\",\"courseId\":\"JAVA-201\"}"; echo
  echo "Aguardando as retentativas (~8s) e a atualização das estatísticas do RabbitMQ..."
  sleep 15
  echo "Mensagens na DLQ notification.course-events.queue.dlq:"
  curl -s -u admin:admin "$RABBIT/api/queues/%2F/notification.course-events.queue.dlq" \
    | grep -oE '"messages":[0-9]+' | head -1
fi

printf '\n\033[1;32mFluxo concluído.\033[0m RabbitMQ Management: %s (admin/admin)\n' "$RABBIT"
