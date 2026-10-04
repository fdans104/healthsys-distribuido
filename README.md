# HealthSys Distribuído

Plataforma de Gestão Hospitalar baseada em Arquitetura Distribuída e Microserviços tolerantes a falhas.

## Estrutura de Divisão do Projeto
- **`services/`**: Código-fonte de cada microserviço independente (Spring Boot).
  - `patient-service`: Gestão cadastral de pacientes (PostgreSQL).
  - `record-service`: Prontuário eletrônico e exames (MongoDB).
  - `triage-service`: Teletriagem e classificação de risco (Kafka).
  - `auth-service`: Autenticação e autorização (JWT).
  - `notification-service`: Notificações assíncronas (RabbitMQ/Kafka).
- **`frontend/`**: Aplicação cliente (React / Angular).
- **`infra/`**:
  - `docker/`: Configurações adicionais de containers.
  - `db/`: Scripts de inicialização (`postgres-init`, `mongo-init`).
  - `k8s/`: Manifests de deployment e serviços para Kubernetes.
- **`docs/`**: Documentação técnica de arquitetura, diagramas e relatórios.

---

## Como Executar o Ambiente Localmente

### Pré-requisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop) instalado e rodando.
- Git instalado.

### 1. Subir os Bancos de Dados e Cache
Na raiz do repositório, execute:
```bash
docker compose up -d
```

### 2. Status dos Serviços
Verifique se todos os containers subiram:
```bash
docker compose ps
```

- **PostgreSQL**: `localhost:5432` | Banco: `healthsys_db` | User: `healthuser` | Pass: `healthpass`
- **MongoDB**: `localhost:27017` | Banco: `healthsys_records` | User: `mongouser` | Pass: `mongopass`
- **Redis**: `localhost:6379`

### 3. Parar os Serviços
Para pausar os containers sem perder dados:
```bash
docker compose down
```
