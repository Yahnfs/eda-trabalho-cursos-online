# EDA — Sistema de Gerenciamento de Cursos Online

Trabalho prático da disciplina **Sistemas Baseados em Eventos e Mensageria Distribuída** (Prof. Rômulo F. Douro).
**Tema escolhido:** Proposta 04 — Sistema de Gerenciamento de Cursos Online.

Quatro microsserviços **Spring Boot 4 / Java 21** que se comunicam **exclusivamente por eventos** através do
**RabbitMQ** (Docker), cada um com seu próprio banco **SQLite**:

| Serviço | Porta | Papel |
|---------|-------|-------|
| **Enrollment Service** | 8081 | Catálogo de cursos e matrículas — **inicia o fluxo** (`POST /api/enrollments`) |
| **Progress Service** | 8082 | Registra aulas concluídas e detecta a conclusão do curso |
| **Certificate Service** | 8083 | Emite o certificado quando o curso é concluído |
| **Notification Service** | 8084 | Notifica o aluno a cada evento (serviço extra) |

```
POST /enrollments ─► EnrollmentCreated ─► Progress (acompanha)   ─┐
                                        └► Notification (e-mail)   │
POST /lessons/{n}/complete ─► LessonCompleted ─► Enrollment (%)    │  topic: courses.events
                          └─► CourseCompleted ─► Certificate ──────┘
                                              ├► Enrollment (COMPLETED)
                                              └► Notification
Certificate ─► CertificateIssued ─► fanout: certificates.fanout ─► Enrollment (CERTIFIED) + Notification
Falhas (após retry 1s/2s/4s) ─► direct: courses.dlx ─► <fila>.dlq
```

📄 **Documentação arquitetural completa (diagramas, eventos, decisões técnicas):** [`documentacao/arquitetura.md`](documentacao/arquitetura.md)

---

## 1. Pré-requisitos e dependências

### Para executar tudo em containers (recomendado)

| Ferramenta | Versão mínima | Verificar com |
|------------|---------------|---------------|
| Docker Engine / Docker Desktop | 24+ | `docker --version` |
| Docker Compose (v2) | 2.20+ | `docker compose version` |
| curl (ou PowerShell) | — | para testar a API |

Não é necessário ter Java nem Maven instalados: o build acontece dentro dos containers.

### Para executar os serviços fora do Docker (IDE / linha de comando)

| Ferramenta | Versão |
|------------|--------|
| JDK | **21** ou superior (`java -version`) |
| Maven | Não precisa instalar — use o **Maven Wrapper** incluso (`./mvnw` ou `mvnw.cmd`) |
| Docker + Docker Compose | Para subir o RabbitMQ (obrigatório via Docker) |

### Portas utilizadas

`5672` (AMQP), `15672` (RabbitMQ Management), `8081`–`8084` (serviços). Libere-as antes de iniciar.

### Dependências do projeto (gerenciadas pelo Maven)

| Dependência | Uso |
|-------------|-----|
| `spring-boot-starter-webmvc` | API REST |
| `spring-boot-starter-amqp` (Spring AMQP) | Integração com RabbitMQ |
| `spring-boot-starter-data-jpa` (Hibernate 7) | Persistência |
| `spring-boot-starter-validation` | Validação dos requests |
| `spring-boot-starter-actuator` | Health check (`/actuator/health`) |
| `org.xerial:sqlite-jdbc` + `hibernate-community-dialects` | Banco SQLite |
| `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) | Testes automatizados |

Imagens Docker: `rabbitmq:3-management-alpine`, `maven:3.9-eclipse-temurin-21` (build) e `eclipse-temurin:21-jre-alpine` (runtime).

---

## 2. Como executar

### Opção A — Tudo com Docker Compose (broker + 4 serviços)

```bash
# 1. Clonar o repositório
git clone <url-do-repositorio> eda-trabalho-cursos-online
cd eda-trabalho-cursos-online

# 2. Construir as imagens e subir o ambiente (a primeira execução baixa dependências: ~3-5 min)
docker compose up -d --build

# 3. Acompanhar a inicialização (aguarde "Started ...Application" nos 4 serviços)
docker compose ps
docker compose logs -f enrollment-service progress-service certificate-service notification-service
```

Os serviços só iniciam depois que o healthcheck do RabbitMQ fica `healthy`.

### Opção B — RabbitMQ no Docker + serviços na máquina local

```bash
# 1. Subir somente o RabbitMQ
docker compose up -d rabbitmq

# 2. Em 4 terminais diferentes, a partir da raiz do projeto:
cd enrollment-service   && ./mvnw spring-boot:run
cd progress-service     && ./mvnw spring-boot:run
cd certificate-service  && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
```

> No Windows (cmd/PowerShell) use `mvnw.cmd spring-boot:run`. Também é possível abrir o projeto na IDE
> (IntelliJ / Eclipse / VS Code) e executar as classes `*ServiceApplication`.
> Os bancos SQLite são criados em `<serviço>/data/*.db`.

**Suba os quatro serviços antes de gerar tráfego:** cada consumidor declara a própria fila. Se um evento for
publicado antes de a fila existir, o produtor registra `[RETURN] Evento sem fila de destino` no log.

### Parar o ambiente

```bash
docker compose down        # para os containers, mantendo filas e bancos
docker compose down -v     # para e APAGA os volumes (filas do RabbitMQ e bancos SQLite)
```

---

## 3. Testando o fluxo

### 3.1 Script de demonstração (recomendado)

Executa o fluxo completo e imprime cada etapa:

```bash
bash scripts/demo.sh           # Linux / macOS / Git Bash
bash scripts/demo.sh --falha   # + demonstração de retry com backoff e DLQ
```

```powershell
.\scripts\demo.ps1            # Windows PowerShell
.\scripts\demo.ps1 -Falha
```

> Se o PowerShell bloquear o script: `powershell -ExecutionPolicy Bypass -File .\scripts\demo.ps1`

### 3.2 Passo a passo manual (curl)

```bash
# 1) Ver cursos disponíveis (EDA-101 tem 3 aulas)
curl http://localhost:8081/api/courses

# 2) INICIAR O FLUXO: criar matrícula -> publica EnrollmentCreated
curl -X POST http://localhost:8081/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentId":"aluno-1","studentName":"Maria Silva","studentEmail":"maria@email.com","courseId":"EDA-101"}'
#    -> 201 {"id":1, "status":"ACTIVE", ...}

# 3) Progress Service criou o acompanhamento ao consumir o evento
curl http://localhost:8082/api/progress/1

# 4) Concluir as 3 aulas -> LessonCompleted (x3) + CourseCompleted
curl -X POST http://localhost:8082/api/progress/1/lessons/1/complete
curl -X POST http://localhost:8082/api/progress/1/lessons/2/complete
curl -X POST http://localhost:8082/api/progress/1/lessons/3/complete

# 5) Certificado emitido (CourseCompleted -> CertificateIssued)
curl http://localhost:8083/api/certificates/enrollment/1

# 6) Matrícula atualizada para CERTIFIED com o código do certificado
curl http://localhost:8081/api/enrollments/1

# 7) Notificações enviadas ao aluno (matrícula, conclusão, certificado)
curl "http://localhost:8084/api/notifications?recipient=maria@email.com"

# 8) Cancelamento (em outra matrícula ativa) -> EnrollmentCancelled
curl -X POST http://localhost:8081/api/enrollments/2/cancel \
  -H "Content-Type: application/json" -d '{"reason":"Sem tempo"}'
```

> **Windows:** no PowerShell 5.1, `curl` é um alias de `Invoke-WebRequest`; use `curl.exe` ou o script `demo.ps1`.
> Evite acentos no JSON passado pela linha de comando do Windows (a codificação dos argumentos pode corromper o
> corpo da requisição) — os scripts já enviam o corpo em UTF-8.

O arquivo [`documentacao/api-requests.http`](documentacao/api-requests.http) contém as mesmas requisições para o
REST Client do VS Code ou o HTTP Client do IntelliJ.

### 3.3 Demonstrar retry com backoff + Dead Letter Queue

E-mails do domínio `@falha.com` simulam o provedor de e-mail fora do ar no Notification Service:

```bash
curl -X POST http://localhost:8081/api/enrollments -H "Content-Type: application/json" \
  -d '{"studentId":"aluno-9","studentName":"Joao","studentEmail":"joao@falha.com","courseId":"JAVA-201"}'

docker compose logs notification-service | grep -E "EMAIL|DLQ"
```

Serão exibidas 4 tentativas (intervalos de 1s, 2s, 4s) seguidas de `[DLQ] Retentativas esgotadas`. A mensagem
aparece em **`notification.course-events.queue.dlq`** no RabbitMQ Management. O Progress Service, que consome o
mesmo evento, **não é afetado**.

### 3.4 Demonstrar idempotência

Publique a **mesma** mensagem duas vezes (mesmo `eventId`) pelo RabbitMQ Management
(*Exchanges → courses.events → Publish message*), com routing key `progress.course.completed`, header
`__TypeId__ = CourseCompleted`, propriedade `content_type = application/json` e o payload:

```json
{"eventId":"dup-1","occurredAt":"2026-08-01T10:00:00Z","enrollmentId":77,"studentId":"x","studentName":"Teste",
 "studentEmail":"teste@email.com","courseId":"EDA-101","courseName":"EDA","totalLessons":3,"completedAt":"2026-08-01T10:00:00Z"}
```

Somente **um** certificado é emitido; na segunda entrega os logs mostram
`[IDEMPOTENCY] Evento CourseCompleted eventId=dup-1 já processado anteriormente - ignorando duplicata`.

---

## 4. Verificações

| O quê | Como |
|-------|------|
| **RabbitMQ Management** | <http://localhost:15672> — usuário `admin`, senha `admin`. Abas *Exchanges* (`courses.events` topic, `certificates.fanout` fanout, `courses.dlx` direct) e *Queues* (6 filas + 6 DLQs) |
| **Logs** | `docker compose logs -f <serviço>` — prefixos `[PUBLISH]`, `[CONSUME]`, `[IDEMPOTENCY]`, `[DLQ]`, `[EMAIL]` ... |
| **Dados (via API)** | Endpoints `GET` de cada serviço (tabela abaixo) |
| **Dados (banco SQLite)** | Execução local: arquivos em `<serviço>/data/*.db`. Em Docker: `docker cp enrollment-service:/app/data/enrollment.db .` e abra com [DB Browser for SQLite](https://sqlitebrowser.org/) ou `sqlite3 enrollment.db "select * from enrollments;"` |
| **Saúde** | `curl http://localhost:8081/actuator/health` (inclui `rabbit` e `db`) |

### Endpoints

| Serviço | Método e rota | Descrição |
|---------|---------------|-----------|
| Enrollment :8081 | `GET /api/courses` | Catálogo de cursos |
| | **`POST /api/enrollments`** | **Cria matrícula (inicia o fluxo)** |
| | `GET /api/enrollments[?studentId=]` | Lista matrículas |
| | `GET /api/enrollments/{id}` | Detalhe (status, %, certificado) |
| | `POST /api/enrollments/{id}/cancel` | Cancela matrícula |
| Progress :8082 | `GET /api/progress[?studentId=]` | Lista acompanhamentos |
| | `GET /api/progress/{enrollmentId}` | Progresso da matrícula |
| | `GET /api/progress/{enrollmentId}/lessons` | Aulas concluídas |
| | `POST /api/progress/{enrollmentId}/lessons/{n}/complete` | Conclui a aula *n* |
| Certificate :8083 | `GET /api/certificates[?studentId=]` | Lista certificados |
| | `GET /api/certificates/{code}` | Verifica certificado pelo código |
| | `GET /api/certificates/enrollment/{enrollmentId}` | Certificado da matrícula |
| Notification :8084 | `GET /api/notifications[?recipient=]` | Histórico de notificações |

Detalhes de payloads, códigos HTTP e regras: [seção 5 da documentação](documentacao/arquitetura.md#5-endpoints).

---

## 5. Testes automatizados

```bash
./mvnw verify        # Linux / macOS / Git Bash  (roda os testes dos 4 serviços)
mvnw.cmd verify      # Windows
```

29 testes unitários (JUnit 5 + Mockito + AssertJ) cobrindo regras de negócio, publicação de eventos,
idempotência, tratamento de eventos fora de ordem, contrato JSON das mensagens e propagação de falhas para retry.
Também é possível testar um serviço isoladamente: `cd progress-service && ./mvnw test`.

---

## 6. Requisitos atendidos

### Critérios mínimos (seção 9)

| # | Requisito | Atendimento |
|---|-----------|-------------|
| 1 | ≥ 2 microsserviços independentes | 4 serviços, cada um com `pom.xml`, banco e Dockerfile próprios |
| 2 | Comunicação assíncrona via RabbitMQ (Docker) | Spring AMQP + `rabbitmq:3-management-alpine` |
| 3 | Eventos em JSON | `JacksonJsonMessageConverter`, `content_type: application/json` |
| 4 | ≥ 2 tipos de exchange | **3 tipos**: topic (`courses.events`), fanout (`certificates.fanout`), direct (`courses.dlx`) |
| 5 | Endpoint REST que inicia o fluxo | `POST /api/enrollments` |
| 6 | Logs detalhados de cada etapa | Prefixos padronizados em todas as etapas (publicação, consumo, efeito, erro) |
| 7 | Persistência em cada serviço | SQLite em todos os serviços |
| 8 | Tratamento básico de erros | Problem Details (400/404/409/503), retry, DLQ, idempotência |
| 9 | Docker Compose funcional | `docker compose up -d rabbitmq` ou `docker compose up -d --build` |
| 10 | README com instruções | Este arquivo |

### Diferenciais (seção 14)

| Diferencial | Status | Onde |
|-------------|--------|------|
| Dead Letter Queue | ✅ | `courses.dlx` + 6 filas `*.dlq` — `DeadLetterSupport.java` |
| Idempotência | ✅ | Tabela `processed_events` — `IdempotencyGuard.java` |
| Retry com Backoff | ✅ | 1s → 2s → 4s — `spring.rabbitmq.listener.simple.retry.*` |
| Mais de 3 serviços | ✅ | Notification Service |
| Testes Automatizados | ✅ | 29 testes — `./mvnw verify` |
| Containerização Completa | ✅ | Dockerfile por serviço + `docker compose up -d --build` |
| Uso de SQLite | ✅ | Todos os serviços |

---

## 7. Estrutura do repositório

```
eda-trabalho-cursos-online/
├── docker-compose.yml          # RabbitMQ + 4 serviços
├── pom.xml                     # agregador (build/teste de todos os serviços)
├── mvnw, mvnw.cmd, .mvn/       # Maven Wrapper
├── README.md
├── documentacao/
│   ├── arquitetura.md          # documentação arquitetural
│   └── api-requests.http       # requisições prontas (VS Code / IntelliJ)
├── scripts/
│   ├── demo.sh                 # demonstração ponta a ponta (bash)
│   └── demo.ps1                # demonstração ponta a ponta (PowerShell)
├── enrollment-service/         # :8081
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/br/com/cursosonline/enrollment/
│       ├── config/             # topologia RabbitMQ + propriedades
│       ├── domain/             # entidades JPA + repositórios
│       ├── event/              # contratos dos eventos (records)
│       ├── exception/
│       ├── idempotency/        # processed_events
│       ├── messaging/          # listeners, publisher, conversor JSON, DLQ
│       ├── service/            # regras de negócio
│       └── web/                # controllers, DTOs, tratamento de erros
├── progress-service/           # :8082 (mesma organização)
├── certificate-service/        # :8083
└── notification-service/       # :8084
```

---

## 8. Solução de problemas

| Sintoma | Causa provável / solução |
|---------|--------------------------|
| `Bind for 0.0.0.0:5672 failed: port is already allocated` | Outro RabbitMQ rodando. Pare-o (`docker ps`) ou libere a porta |
| Serviço reinicia em loop com `Connection refused` | RabbitMQ ainda iniciando — aguarde; `restart: on-failure` resolve sozinho |
| `404 Progresso não encontrado` logo após criar a matrícula | O evento ainda está sendo processado (assíncrono). Aguarde 1-2 segundos |
| `[RETURN] Evento sem fila de destino` | Evento publicado antes de o consumidor criar sua fila. Suba todos os serviços antes de testar |
| Quero começar do zero | `docker compose down -v` (e, na execução local, apague as pastas `*/data`) |
| `java: release 21 not supported` | Instale o JDK 21+ ou use a Opção A (Docker) |
