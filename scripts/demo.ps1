# -----------------------------------------------------------------------------
# Demonstração ponta a ponta do fluxo orientado a eventos (Windows PowerShell).
# Pré-requisito: RabbitMQ + 4 serviços em execução (docker compose up -d --build).
#
#   .\scripts\demo.ps1          -> fluxo completo: matrícula -> aulas -> certificado
#   .\scripts\demo.ps1 -Falha   -> também demonstra retry com backoff + DLQ
# -----------------------------------------------------------------------------
param([switch]$Falha)

$ErrorActionPreference = 'Stop'
$Enrollment   = 'http://localhost:8081'
$Progress     = 'http://localhost:8082'
$Certificate  = 'http://localhost:8083'
$Notification = 'http://localhost:8084'
$Rabbit       = 'http://localhost:15672'

$Student = "aluno-$(Get-Random -Maximum 99999)"
$Email   = "$Student@email.com"

function Step($text) { Write-Host "`n==> $text" -ForegroundColor Cyan }
function Show($obj)  { $obj | ConvertTo-Json -Depth 5 | Write-Host }
function Post-Json($url, $body) {
    $json = $body | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri $url -ContentType 'application/json; charset=utf-8' `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($json))
}
function Try-Request([scriptblock]$request) {
    try { Show (& $request) }
    catch {
        $body = $_.ErrorDetails.Message
        if (-not $body -and $_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream(), [System.Text.Encoding]::UTF8)
            $body = $reader.ReadToEnd()
        }
        Write-Host "HTTP $([int]$_.Exception.Response.StatusCode): $body" -ForegroundColor Yellow
    }
}

Step '1. Catálogo de cursos (Enrollment Service)'
Show (Invoke-RestMethod "$Enrollment/api/courses")

Step '2. POST /api/enrollments -> publica EnrollmentCreated (topic: enrollment.created)'
$enrollmentBody = @{ studentId = $Student; studentName = 'Aluno Demo'; studentEmail = $Email; courseId = 'EDA-101' }
$created = Post-Json "$Enrollment/api/enrollments" $enrollmentBody
Show $created
$id = $created.id
Start-Sleep -Seconds 2

Step '3. Progress Service criou o acompanhamento ao consumir EnrollmentCreated'
Show (Invoke-RestMethod "$Progress/api/progress/$id")

Step '4. Concluindo as 3 aulas -> LessonCompleted (x3) e CourseCompleted na última'
1..3 | ForEach-Object { Show (Invoke-RestMethod -Method Post "$Progress/api/progress/$id/lessons/$_/complete") }
Start-Sleep -Seconds 3

Step '5. Certificate Service emitiu o certificado (CertificateIssued via fanout)'
Show (Invoke-RestMethod "$Certificate/api/certificates/enrollment/$id")

Step '6. Enrollment Service atualizou a matrícula para CERTIFIED'
Show (Invoke-RestMethod "$Enrollment/api/enrollments/$id")

Step '7. Notification Service registrou as notificações do aluno'
Show (Invoke-RestMethod "$Notification/api/notifications?recipient=$Email")

Step '8. Tratamento de erros: aula repetida (409) e matrícula duplicada (409)'
Try-Request { Invoke-RestMethod -Method Post "$Progress/api/progress/$id/lessons/1/complete" }
Try-Request { Post-Json "$Enrollment/api/enrollments" $enrollmentBody }

if ($Falha) {
    Step '9. Retry + DLQ: e-mail @falha.com falha no Notification Service (1s, 2s, 4s -> DLQ)'
    Show (Post-Json "$Enrollment/api/enrollments" @{
        studentId = "$Student-f"; studentName = 'Aluno Falha'; studentEmail = "$Student@falha.com"; courseId = 'JAVA-201' })
    Write-Host 'Aguardando as retentativas (~8s) e a atualização das estatísticas do RabbitMQ...'
    Start-Sleep -Seconds 15
    $auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('admin:admin'))
    $dlq = Invoke-RestMethod "$Rabbit/api/queues/%2F/notification.course-events.queue.dlq" -Headers @{ Authorization = "Basic $auth" }
    Write-Host "Mensagens na DLQ notification.course-events.queue.dlq: $($dlq.messages)"
}

Write-Host "`nFluxo concluído. RabbitMQ Management: $Rabbit (admin/admin)" -ForegroundColor Green
