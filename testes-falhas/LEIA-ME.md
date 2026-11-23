# Testes de tolerância a falhas

Estes testes mostram que, num sistema distribuído, **uma peça caindo não derruba as outras**, e que tudo **se recupera sozinho** quando a peça volta.

Há duas formas de testar.

## 1. Automática (script)

Com o sistema ligado (`docker compose up -d --build`), no PowerShell, na pasta do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File .\testes-falhas\testar-tolerancia-a-falhas.ps1
```

O script cria usuários e um paciente de teste, desliga uma peça de cada vez com `docker stop`, faz as conferências, liga a peça de novo e no fim mostra um resumo com `[OK]` ou `[FALHOU]`. Leva de 5 a 10 minutos.

Para rodar só alguns cenários: `... -Cenarios A,D`

| Cenário | O que é desligado | O que precisa acontecer |
| :--- | :--- | :--- |
| A | Redis (cache) | A ficha do paciente continua abrindo, direto do PostgreSQL. |
| B | Kafka (mensageria) | A triagem continua sendo aberta; o evento fica guardado na caixa de saída e é entregue quando o Kafka volta; a enfermagem recebe o aviso. |
| C | Serviço de notificações | A triagem continua funcionando; o gateway responde 503 na hora; ao voltar, o serviço lê do Kafka o que perdeu. |
| D | Serviço de pacientes | O gateway responde 503 rápido; a triagem recusa com 503 (sem erro 500); a fila continua abrindo; o circuit breaker abre e depois fecha sozinho. |
| E | Serviço de login | Quem já estava logado continua usando o sistema; um login novo recebe 503. |
| F | Monitores dos leitos (simulador IoT) e broker MQTT | Os sinais chegam ao vivo; uma piora gera alerta e notificação; sem monitor aparece "sem sinal"; sem broker o serviço reconecta sozinho. |
| G | Serviço de monitoramento | Os leitos recebem 503; o prontuário continua funcionando. |

Além do script, cada serviço tem **testes automáticos** (JUnit) que rodam durante o `docker compose up --build`. Os de tolerância a falhas são:

- `ChamadaResilienteTest` (triagem, prontuário e monitoramento): nova tentativa, abertura e fechamento do circuit breaker, e "paciente não encontrado" que não conta como falha.
- `CaixaDeSaidaTest` (triagem, prontuário e monitoramento): com o Kafka fora, o evento continua guardado e a ordem é mantida; quando ele volta, o evento é enviado.
- `CacheToleranteTest` (pacientes): erro do Redis não derruba a requisição.
- `NotificacaoServiceTest` (notificações): o mesmo evento recebido duas vezes não duplica o aviso, e mensagem inválida vai para o tópico DLT.

## 2. Manual (para mostrar na apresentação)

Abra o site em http://localhost:3000, o painel do Grafana em http://localhost:3001 e o Kafka UI em http://localhost:8090. Depois, num terminal:

1. **Kafka fora:** `docker stop healthsys-kafka`. Abra uma triagem pela recepção: funciona. No Grafana, o gráfico "Eventos esperando o Kafka" sobe. `docker start healthsys-kafka`: o gráfico volta a zero e o aviso aparece no sino da enfermagem.
2. **Serviço de pacientes fora:** `docker stop healthsys-patient-service`. A tela de Pacientes mostra "serviço indisponível" na hora, e a Triagem continua abrindo. No Grafana, o painel "Circuit breakers" mostra ABERTO. `docker start healthsys-patient-service`: em até 1 minuto, volta ao normal.
3. **Paciente piorando (IoT):** interne um paciente no leito UTI-01 pela tela de Leitos e rode
   `docker exec healthsys-mosquitto mosquitto_pub -t healthsys/simulador/comando -m "piorar UTI-01"`.
   Em poucos segundos os sinais ficam vermelhos e chega um alerta crítico para o médico e a enfermagem.
   Para voltar: `... -m "normalizar UTI-01"`.
4. **Monitor desligado:** `docker stop healthsys-iot-simulator`. Depois de 15 segundos o leito mostra "sem sinal do monitor". `docker start healthsys-iot-simulator` e os sinais voltam.
