<#
  Testes de tolerância a falhas do HealthSys.

  O script derruba uma peça do sistema de cada vez (docker stop), confere que o resto continua
  funcionando, liga a peça de novo (docker start) e confere que tudo se recupera sozinho.

  Como usar (no PowerShell, na pasta do projeto, com o sistema ligado por "docker compose up -d"):
      powershell -ExecutionPolicy Bypass -File .\testes-falhas\testar-tolerancia-a-falhas.ps1

  Para rodar só alguns cenários:
      powershell -ExecutionPolicy Bypass -File .\testes-falhas\testar-tolerancia-a-falhas.ps1 -Cenarios A,D

  Cenários:
    A  Redis (cache) fora do ar
    B  Kafka (mensageria) fora do ar
    C  Serviço de notificações fora do ar
    D  Serviço de pacientes fora do ar (circuit breaker)
    E  Serviço de login fora do ar
    F  Monitores dos leitos (IoT/MQTT) fora do ar
    G  Serviço de monitoramento fora do ar

  Leva de 5 a 10 minutos (alguns serviços precisam reiniciar). No final mostra um resumo.
#>
param(
    [string[]]$Cenarios = @('A', 'B', 'C', 'D', 'E', 'F', 'G')
)

# Pelo "powershell -File", "-Cenarios B,C" chega como um texto só ("B,C"): separa pela vírgula.
$Cenarios = @($Cenarios | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim().ToUpper() } | Where-Object { $_ })

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$Api = 'http://localhost:8080'
$Senha = 'Teste12345'
$Resultados = New-Object System.Collections.ArrayList

# ------------------------------------------------------------------ utilidades

function Titulo([string]$texto) {
    Write-Host ''
    Write-Host "=== $texto ===" -ForegroundColor Cyan
}

function Registrar([string]$cenario, [string]$verificacao, [bool]$passou, [string]$detalhe = '') {
    [void]$Resultados.Add([pscustomobject]@{ Cenario = $cenario; Verificacao = $verificacao; Passou = $passou; Detalhe = $detalhe })
    if ($passou) {
        Write-Host "  [OK]     $verificacao $detalhe" -ForegroundColor Green
    } else {
        Write-Host "  [FALHOU] $verificacao $detalhe" -ForegroundColor Red
    }
}

function Chamar {
    param([string]$Metodo, [string]$Caminho, $Corpo = $null, [string]$Token = '', [int]$TimeoutSeg = 20, [string]$Base = $Api)
    $cabecalhos = @{}
    if ($Token) { $cabecalhos['Authorization'] = "Bearer $Token" }
    $parametros = @{ Method = $Metodo; Uri = "$Base$Caminho"; Headers = $cabecalhos; TimeoutSec = $TimeoutSeg; UseBasicParsing = $true }
    if ($null -ne $Corpo) {
        $parametros['Body'] = [System.Text.Encoding]::UTF8.GetBytes(($Corpo | ConvertTo-Json -Depth 6))
        $parametros['ContentType'] = 'application/json; charset=utf-8'
    }
    $inicio = Get-Date
    try {
        $resposta = Invoke-WebRequest @parametros
        $dados = $null
        if ($resposta.Content) {
            try { $dados = $resposta.Content | ConvertFrom-Json } catch { $dados = $resposta.Content }
        }
        return [pscustomobject]@{ Status = [int]$resposta.StatusCode; Dados = $dados; Segundos = ((Get-Date) - $inicio).TotalSeconds }
    } catch {
        $status = 0
        $mensagem = $_.Exception.Message
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            if ($_.ErrorDetails -and $_.ErrorDetails.Message) { $mensagem = $_.ErrorDetails.Message }
        }
        return [pscustomobject]@{ Status = $status; Dados = $null; Segundos = ((Get-Date) - $inicio).TotalSeconds; Erro = $mensagem }
    }
}

# Obs.: não pode se chamar "Docker": no PowerShell maiúscula e minúscula são iguais e a função chamaria a si mesma.
function ExecutarDocker([string[]]$argumentos) {
    # Mensagens do docker no stderr não devem interromper o script.
    $ErrorActionPreference = 'Continue'
    $saida = & docker.exe @argumentos 2>&1
    if ($LASTEXITCODE -ne 0) { Write-Host "  (docker $($argumentos -join ' '): $saida)" -ForegroundColor DarkYellow }
}

function Parar([string]$container) {
    Write-Host "  -> desligando $container" -ForegroundColor DarkGray
    ExecutarDocker @('stop', $container)
}

function Ligar([string]$container) {
    Write-Host "  -> ligando $container" -ForegroundColor DarkGray
    ExecutarDocker @('start', $container)
}

# Espera até a condição dar verdadeiro (ou acabar o tempo). Devolve $true se deu certo.
function Esperar([scriptblock]$condicao, [int]$segundos, [int]$intervalo = 3) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        try { if (& $condicao) { return $true } } catch { }
        Start-Sleep -Seconds $intervalo
    }
    return $false
}

function Saudavel([int]$porta) {
    $r = Chamar -Metodo GET -Caminho '/actuator/health' -Base "http://localhost:$porta" -TimeoutSeg 5
    return ($r.Status -eq 200)
}

function EsperarSaude([int]$porta, [int]$segundos = 240) {
    return (Esperar { Saudavel $porta } $segundos 5)
}

# Lê um número das métricas do Prometheus de um serviço (ex.: healthsys_eventos_pendentes).
function Metrica([int]$porta, [string]$padrao) {
    $r = Invoke-WebRequest -Uri "http://localhost:$porta/actuator/prometheus" -UseBasicParsing -TimeoutSec 5
    foreach ($linha in ($r.Content -split "`n")) {
        if ($linha -match "^$padrao\s+([0-9.eE+-]+)") { return [double]$Matches[1] }
    }
    return $null
}

function GerarCpf {
    $d = @()
    for ($i = 0; $i -lt 9; $i++) { $d += Get-Random -Minimum 0 -Maximum 10 }
    $s = 0; for ($i = 0; $i -lt 9; $i++) { $s += $d[$i] * (10 - $i) }
    $r = ($s * 10) % 11; if ($r -eq 10) { $r = 0 }; $d += $r
    $s = 0; for ($i = 0; $i -lt 10; $i++) { $s += $d[$i] * (11 - $i) }
    $r = ($s * 10) % 11; if ($r -eq 10) { $r = 0 }; $d += $r
    return ($d -join '')
}

# Logo depois de ligar, o primeiro login pode demorar (o Java ainda está "esquentando") e o gateway
# responder 503. Por isso tenta por até 2 minutos antes de desistir.
function Entrar([string]$email, [string]$senha) {
    $limite = (Get-Date).AddSeconds(120)
    do {
        $r = Chamar -Metodo POST -Caminho '/api/v1/auth/login' -Corpo @{ email = $email; senha = $senha }
        if ($r.Status -eq 200) { return $r.Dados.token }
        if ($r.Status -eq 401) { throw "E-mail ou senha recusados para $email." }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $limite)
    throw "Não foi possível entrar como $email (último status $($r.Status)). Rode 'docker compose logs auth-service api-gateway --tail 80'."
}

function ContarNotificacoes([string]$token, [string]$tipo, [int]$pacienteId) {
    $r = Chamar -Metodo GET -Caminho '/api/v1/notificacoes' -Token $token
    if ($r.Status -ne 200) { return -1 }
    return @($r.Dados | Where-Object { $_.tipo -eq $tipo -and $_.pacienteId -eq $pacienteId }).Count
}

function AbrirTriagem([string]$token, [int]$pacienteId, [string]$queixa) {
    return (Chamar -Metodo POST -Caminho '/api/v1/triagens' -Token $token -Corpo @{ pacienteId = $pacienteId; queixaPrincipal = $queixa })
}

function CancelarTriagens([string]$token, [int]$pacienteId) {
    $r = Chamar -Metodo GET -Caminho "/api/v1/triagens?pacienteId=$pacienteId" -Token $token
    if ($r.Status -ne 200) { return }
    foreach ($t in @($r.Dados | Where-Object { $_.status -eq 'PENDENTE' -or $_.status -eq 'EM_ATENDIMENTO' })) {
        [void](Chamar -Metodo PUT -Caminho "/api/v1/triagens/$($t.id)/status" -Token $token -Corpo @{ status = 'CANCELADA'; versao = $t.versao })
    }
}

# ------------------------------------------------------------------ preparação

Titulo 'Preparação'
Write-Host '  Conferindo se os serviços estão no ar (pode levar alguns minutos logo depois do docker compose up)...'
$portas = @{ 'api-gateway' = 8080; 'auth-service' = 8081; 'patient-service' = 8082; 'record-service' = 8083; 'triage-service' = 8084; 'notification-service' = 8085; 'monitoring-service' = 8086 }
foreach ($nome in $portas.Keys) {
    if (-not (EsperarSaude $portas[$nome] 300)) { throw "O $nome (porta $($portas[$nome])) não respondeu. Rode 'docker compose ps' e 'docker compose logs $nome'." }
}
Write-Host '  Todos os serviços estão no ar.' -ForegroundColor Green

$tokenAdmin = Entrar 'admin@healthsys.com' 'admin123'
$perfis = @(
    @{ email = 'teste.recepcao@healthsys.com'; nome = 'Teste Recepcao'; perfil = 'RECEPCIONISTA' },
    @{ email = 'teste.enfermeiro@healthsys.com'; nome = 'Teste Enfermeiro'; perfil = 'ENFERMEIRO' },
    @{ email = 'teste.medico@healthsys.com'; nome = 'Teste Medico'; perfil = 'MEDICO' }
)
foreach ($p in $perfis) {
    $r = Chamar -Metodo POST -Caminho '/api/v1/usuarios' -Token $tokenAdmin -Corpo @{ nome = $p.nome; email = $p.email; senha = $Senha; perfil = $p.perfil }
    if ($r.Status -ne 201 -and $r.Status -ne 409) { throw "Não foi possível criar o usuário $($p.email) (status $($r.Status))." }
}
$tokenRecepcao = Entrar 'teste.recepcao@healthsys.com' $Senha
$tokenEnfermeiro = Entrar 'teste.enfermeiro@healthsys.com' $Senha
$tokenMedico = Entrar 'teste.medico@healthsys.com' $Senha

$r = Chamar -Metodo POST -Caminho '/api/v1/pacientes' -Token $tokenRecepcao -Corpo @{
    nome = 'Paciente Teste de Falhas'; cpf = (GerarCpf); dataNascimento = '1990-01-01'; sexo = 'F' }
if ($r.Status -ne 201) { throw "Não foi possível cadastrar o paciente de teste (status $($r.Status))." }
$pacienteId = [int]$r.Dados.id
Write-Host "  Usuários de teste prontos e paciente de teste nº $pacienteId cadastrado." -ForegroundColor Green

# ------------------------------------------------------------------ cenários

if ($Cenarios -contains 'A') {
    Titulo 'A) Redis (cache distribuído) fora do ar'
    try {
        [void](Chamar -Metodo GET -Caminho "/api/v1/pacientes/$pacienteId" -Token $tokenRecepcao)
        Parar 'healthsys-redis'
        Start-Sleep -Seconds 2
        $r = Chamar -Metodo GET -Caminho "/api/v1/pacientes/$pacienteId" -Token $tokenRecepcao
        Registrar 'A' 'Ficha do paciente continua abrindo sem o cache (busca direto no PostgreSQL)' ($r.Status -eq 200) "(status $($r.Status), $([math]::Round($r.Segundos, 1)) s)"
    } finally {
        Ligar 'healthsys-redis'
    }
    $ok = Esperar { (Chamar -Metodo GET -Caminho "/api/v1/pacientes/$pacienteId" -Token $tokenRecepcao).Status -eq 200 } 30
    Registrar 'A' 'Com o Redis de volta, tudo normal' $ok
}

if ($Cenarios -contains 'B') {
    Titulo 'B) Kafka (mensageria) fora do ar: nenhum evento pode se perder'
    CancelarTriagens $tokenRecepcao $pacienteId
    $antes = ContarNotificacoes $tokenEnfermeiro 'TRIAGEM_ABERTA' $pacienteId
    try {
        Parar 'healthsys-kafka'
        Start-Sleep -Seconds 3
        $r = AbrirTriagem $tokenRecepcao $pacienteId 'Teste: Kafka fora do ar'
        Registrar 'B' 'Recepção consegue abrir a triagem mesmo sem o Kafka' ($r.Status -eq 201) "(status $($r.Status), $([math]::Round($r.Segundos, 1)) s) $($r.Erro)"
        $guardado = Esperar { (Metrica 8084 'healthsys_eventos_pendentes') -ge 1 } 30
        Registrar 'B' 'O evento ficou guardado na caixa de saída do triage-service (healthsys_eventos_pendentes >= 1)' $guardado
    } finally {
        Ligar 'healthsys-kafka'
    }
    $enviado = Esperar { (Metrica 8084 'healthsys_eventos_pendentes') -eq 0 } 180 5
    Registrar 'B' 'Com o Kafka de volta, o evento guardado foi enviado (pendentes = 0)' $enviado
    $chegou = Esperar { (ContarNotificacoes $tokenEnfermeiro 'TRIAGEM_ABERTA' $pacienteId) -gt $antes } 120 5
    Registrar 'B' 'A enfermagem recebeu o aviso "Novo paciente na triagem" (nada se perdeu)' $chegou
    CancelarTriagens $tokenRecepcao $pacienteId
}

if ($Cenarios -contains 'C') {
    Titulo 'C) Serviço de notificações fora do ar'
    CancelarTriagens $tokenRecepcao $pacienteId
    $antes = ContarNotificacoes $tokenEnfermeiro 'TRIAGEM_ABERTA' $pacienteId
    try {
        Parar 'healthsys-notification-service'
        Start-Sleep -Seconds 2
        $r = AbrirTriagem $tokenRecepcao $pacienteId 'Teste: notificações fora do ar'
        Registrar 'C' 'A triagem continua funcionando sem o serviço de notificações' ($r.Status -eq 201) "(status $($r.Status), $([math]::Round($r.Segundos, 1)) s) $($r.Erro)"
        $r = Chamar -Metodo GET -Caminho '/api/v1/notificacoes' -Token $tokenEnfermeiro
        Registrar 'C' 'O gateway responde 503 (serviço indisponível) na hora, sem travar' ($r.Status -eq 503 -and $r.Segundos -lt 12) "(status $($r.Status), $([math]::Round($r.Segundos, 1)) s)"
    } finally {
        Ligar 'healthsys-notification-service'
    }
    [void](EsperarSaude 8085)
    $chegou = Esperar { (ContarNotificacoes $tokenEnfermeiro 'TRIAGEM_ABERTA' $pacienteId) -gt $antes } 180 5
    Registrar 'C' 'Ao voltar, o serviço leu do Kafka o que perdeu e avisou a enfermagem' $chegou
    CancelarTriagens $tokenRecepcao $pacienteId
}

if ($Cenarios -contains 'D') {
    Titulo 'D) Serviço de pacientes fora do ar (circuit breaker)'
    CancelarTriagens $tokenRecepcao $pacienteId
    try {
        Parar 'healthsys-patient-service'
        Start-Sleep -Seconds 2
        $r = Chamar -Metodo GET -Caminho '/api/v1/pacientes' -Token $tokenRecepcao
        Registrar 'D' 'Gateway responde 503 com mensagem clara, sem deixar o usuário esperando' ($r.Status -eq 503 -and $r.Segundos -lt 12) "(status $($r.Status), $([math]::Round($r.Segundos, 1)) s)"
        $r = AbrirTriagem $tokenEnfermeiro $pacienteId 'Teste: pacientes fora do ar'
        Registrar 'D' 'Triagem recusa com 503 (não dá para confirmar o paciente), sem erro 500' ($r.Status -eq 503) "(status $($r.Status))"
        $r = Chamar -Metodo GET -Caminho '/api/v1/triagens' -Token $tokenEnfermeiro
        Registrar 'D' 'A fila da triagem continua abrindo (o problema não se espalha)' ($r.Status -eq 200) "(status $($r.Status))"
        for ($i = 0; $i -lt 5; $i++) { [void](AbrirTriagem $tokenEnfermeiro $pacienteId 'Teste: insistindo') }
        $aberto = Esperar { (Metrica 8084 'resilience4j_circuitbreaker_state\{[^}]*name="pacientes"[^}]*state="open"[^}]*\}') -eq 1 } 15 2
        Registrar 'D' 'Depois de várias falhas, o circuit breaker "pacientes" do triage-service ABRIU' $aberto
        $r = AbrirTriagem $tokenEnfermeiro $pacienteId 'Teste: circuito aberto'
        Registrar 'D' 'Com o circuito aberto, a resposta é imediata' ($r.Status -eq 503 -and $r.Segundos -lt 3) "($([math]::Round($r.Segundos, 2)) s)"
    } finally {
        Ligar 'healthsys-patient-service'
    }
    [void](EsperarSaude 8082)
    $voltou = Esperar { (Chamar -Metodo GET -Caminho '/api/v1/pacientes' -Token $tokenRecepcao).Status -eq 200 } 90 5
    Registrar 'D' 'Com o serviço de volta, o gateway fecha o circuito sozinho e tudo volta ao normal' $voltou
    $voltou = Esperar { (AbrirTriagem $tokenEnfermeiro $pacienteId 'Teste: pacientes de volta').Status -eq 201 } 90 5
    Registrar 'D' 'A triagem volta a confirmar o paciente (circuito do triage-service fechado)' $voltou
    CancelarTriagens $tokenRecepcao $pacienteId
}

if ($Cenarios -contains 'E') {
    Titulo 'E) Serviço de login fora do ar'
    try {
        Parar 'healthsys-auth-service'
        Start-Sleep -Seconds 2
        $r = Chamar -Metodo GET -Caminho '/api/v1/triagens' -Token $tokenEnfermeiro
        Registrar 'E' 'Quem já estava logado continua usando o sistema (o token é conferido em cada serviço)' ($r.Status -eq 200) "(status $($r.Status))"
        $r = Chamar -Metodo POST -Caminho '/api/v1/auth/login' -Corpo @{ email = 'teste.medico@healthsys.com'; senha = $Senha }
        Registrar 'E' 'Um login novo recebe 503 (e não um erro genérico)' ($r.Status -eq 503) "(status $($r.Status))"
    } finally {
        Ligar 'healthsys-auth-service'
    }
    [void](EsperarSaude 8081)
    $voltou = Esperar { (Chamar -Metodo POST -Caminho '/api/v1/auth/login' -Corpo @{ email = 'teste.medico@healthsys.com'; senha = $Senha }).Status -eq 200 } 90 5
    Registrar 'E' 'Com o serviço de volta, o login funciona de novo' $voltou
}

if ($Cenarios -contains 'F') {
    Titulo 'F) Monitores dos leitos (IoT / MQTT)'
    $leitos = (Chamar -Metodo GET -Caminho '/api/v1/leitos' -Token $tokenEnfermeiro).Dados
    $leito = @($leitos | Where-Object { $_.pacienteId -eq $pacienteId })[0]
    if (-not $leito) {
        $leito = @($leitos | Where-Object { $_.status -eq 'LIVRE' -and $_.codigo -match '^(UTI|ENF|EMG)-0[0-9]$' })[0]
        if (-not $leito) { throw 'Não há leito livre do simulador (UTI-01..04, ENF-01..06, EMG-01..02) para o teste.' }
        $r = Chamar -Metodo PUT -Caminho "/api/v1/leitos/$($leito.id)/ocupar" -Token $tokenEnfermeiro -Corpo @{ pacienteId = $pacienteId; versao = $leito.versao }
        Registrar 'F' "Paciente internado no leito $($leito.codigo)" ($r.Status -eq 200) "(status $($r.Status))"
    }
    $codigo = $leito.codigo
    $monitor = { @((Chamar -Metodo GET -Caminho '/api/v1/monitoramento/leitos' -Token $tokenEnfermeiro).Dados.leitos | Where-Object { $_.codigo -eq $codigo })[0] }

    $ok = Esperar { $m = & $monitor; $m -and -not $m.semSinal -and $m.leitura } 30
    Registrar 'F' "Sinais vitais do $codigo chegando ao vivo pelo MQTT" $ok

    ExecutarDocker @('exec', 'healthsys-mosquitto', 'mosquitto_pub', '-t', 'healthsys/simulador/comando', '-m', "piorar $codigo")
    $alerta = Esperar { @((Chamar -Metodo GET -Caminho '/api/v1/monitoramento/alertas' -Token $tokenEnfermeiro).Dados | Where-Object { $_.leitoCodigo -eq $codigo -and $_.gravidade -eq 'CRITICO' }).Count -ge 1 } 90 5
    Registrar 'F' 'Paciente piorou: alerta CRÍTICO criado' $alerta
    $aviso = Esperar { (ContarNotificacoes $tokenMedico 'ALERTA_SINAIS_VITAIS' $pacienteId) -ge 1 } 60 5
    Registrar 'F' 'O médico recebeu a notificação do alerta (MQTT -> monitoramento -> Kafka -> notificações)' $aviso
    ExecutarDocker @('exec', 'healthsys-mosquitto', 'mosquitto_pub', '-t', 'healthsys/simulador/comando', '-m', "normalizar $codigo")

    try {
        Parar 'healthsys-iot-simulator'
        $semSinal = Esperar { $m = & $monitor; $m -and $m.semSinal } 40
        Registrar 'F' 'Monitor desligado: a tela mostra "sem sinal do monitor" (o serviço não trava)' $semSinal
    } finally {
        Ligar 'healthsys-iot-simulator'
    }
    $voltou = Esperar { $m = & $monitor; $m -and -not $m.semSinal } 40
    Registrar 'F' 'Monitor ligado de novo: os sinais voltam sozinhos' $voltou

    try {
        Parar 'healthsys-mosquitto'
        $caiu = Esperar { (Metrica 8086 'healthsys_mqtt_conectado') -eq 0 } 60
        Registrar 'F' 'Broker MQTT fora: o monitoring-service percebe e continua respondendo' $caiu
        $r = Chamar -Metodo GET -Caminho '/api/v1/leitos' -Token $tokenEnfermeiro
        Registrar 'F' 'A lista de leitos continua funcionando sem o MQTT' ($r.Status -eq 200) "(status $($r.Status))"
    } finally {
        Ligar 'healthsys-mosquitto'
    }
    $reconectou = Esperar { (Metrica 8086 'healthsys_mqtt_conectado') -eq 1 } 60
    Registrar 'F' 'Broker de volta: o monitoring-service reconecta sozinho' $reconectou

    # Limpeza: reconhece os alertas do leito, dá alta e marca como higienizado.
    foreach ($a in @((Chamar -Metodo GET -Caminho '/api/v1/monitoramento/alertas' -Token $tokenEnfermeiro).Dados | Where-Object { $_.leitoCodigo -eq $codigo })) {
        [void](Chamar -Metodo PUT -Caminho "/api/v1/monitoramento/alertas/$($a.id)/reconhecer" -Token $tokenEnfermeiro)
    }
    $l = @((Chamar -Metodo GET -Caminho '/api/v1/leitos' -Token $tokenEnfermeiro).Dados | Where-Object { $_.codigo -eq $codigo })[0]
    $r = Chamar -Metodo PUT -Caminho "/api/v1/leitos/$($l.id)/liberar" -Token $tokenEnfermeiro -Corpo @{ versao = $l.versao }
    if ($r.Status -eq 200) {
        [void](Chamar -Metodo PUT -Caminho "/api/v1/leitos/$($l.id)/higienizado" -Token $tokenEnfermeiro -Corpo @{ versao = $r.Dados.versao })
    }
}

if ($Cenarios -contains 'G') {
    Titulo 'G) Serviço de monitoramento fora do ar'
    try {
        Parar 'healthsys-monitoring-service'
        Start-Sleep -Seconds 2
        $r = Chamar -Metodo GET -Caminho '/api/v1/leitos' -Token $tokenEnfermeiro
        Registrar 'G' 'Gateway responde 503 para os leitos' ($r.Status -eq 503) "(status $($r.Status))"
        $r = Chamar -Metodo GET -Caminho "/api/v1/prontuarios/$pacienteId" -Token $tokenMedico
        Registrar 'G' 'O prontuário continua funcionando' ($r.Status -eq 200) "(status $($r.Status))"
    } finally {
        Ligar 'healthsys-monitoring-service'
    }
    [void](EsperarSaude 8086)
    $voltou = Esperar { (Chamar -Metodo GET -Caminho '/api/v1/leitos' -Token $tokenEnfermeiro).Status -eq 200 } 90 5
    Registrar 'G' 'Com o serviço de volta, os leitos aparecem de novo' $voltou
}

# ------------------------------------------------------------------ resumo

Titulo 'Resumo'
$Resultados | Format-Table Cenario, Passou, Verificacao, Detalhe -AutoSize -Wrap | Out-String -Width 250 | Write-Host
$falhas = @($Resultados | Where-Object { -not $_.Passou }).Count
$total = $Resultados.Count
if ($falhas -eq 0) {
    Write-Host "Todas as $total verificações passaram." -ForegroundColor Green
    exit 0
}
Write-Host "$falhas de $total verificações falharam. Veja os detalhes acima e 'docker compose logs <serviço>'." -ForegroundColor Red
exit 1
