# 🏥 HealthSys Distribuído

[![Arquitetura](https://img.shields.io/badge/Arquitetura-Microsservi%C3%A7os-blue)](docs/diagrams/arquitetura.md)
[![Docker](https://img.shields.io/badge/Docker-Pronto-green)](docker-compose.yml)
[![Status](https://img.shields.io/badge/Sprint_1-Conclu%C3%ADda-success)](#)

Plataforma de Gestão Hospitalar baseada em Arquitetura Distribuída, desenvolvida para apoiar hospitais no gerenciamento de pacientes, prontuários eletrônicos e triagens médicas com foco em alta disponibilidade e tolerância a falhas.

---

## 📁 Estrutura do Repositório

- **`docs/`**: Documentação técnica de arquitetura, diagramas (Arquitetura, Fluxo, Modelo de Dados) e relatórios.
- **`services/`**: Código-fonte de cada microsserviço independente (Spring Boot).
  - `patient-service`: Gestão cadastral de pacientes (PostgreSQL).
  - `record-service`: Prontuário eletrônico e exames (MongoDB).
  - `triage-service`: Teletriagem e classificação de risco (Kafka/PostgreSQL).
  - `auth-service`: Autenticação e autorização (JWT/PostgreSQL).
  - `notification-service`: Notificações assíncronas (Kafka/WebSocket).
- **`frontend/`**: Aplicação cliente interativa (React / Angular).
- **`infra/`**:
  - `docker/`: Configurações adicionais de containers.
  - `db/`: Scripts de inicialização automática de banco de dados (`postgres-init` e `mongo-init`).
  - `k8s/`: Manifests de deployment e serviços para Kubernetes.

---

## 🚀 Como Executar a Infraestrutura Localmente

A infraestrutura de suporte (Bancos de Dados, Cache e Monitoramento) foi totalmente containerizada. Você não precisa instalar nenhum banco de dados na sua máquina.

### Pré-requisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop) instalado e em execução (Status: *Engine running*).
- Terminal (PowerShell, Bash ou CMD).

### 1. Iniciar os Serviços Básicos
Na raiz do repositório, execute o comando abaixo para baixar e rodar toda a infraestrutura em segundo plano:
```bash
docker compose up -d
```

### 2. Verificar o Status
Verifique se todos os containers (Postgres, Mongo, Redis, Prometheus e Grafana) subiram corretamente:
```bash
docker compose ps
```

### 3. Acessos e Credenciais

Os bancos já nascem populados com as tabelas essenciais criadas pelos scripts de inicialização.

| Serviço | Endereço | Usuário / Admin | Senha | Database Interno |
| :--- | :--- | :--- | :--- | :--- |
| **PostgreSQL** | `localhost:5432` | `healthuser` | `healthpass` | `healthsys_db` |
| **MongoDB** | `localhost:27017` | `mongouser` | `mongopass` | `healthsys_records` |
| **Redis** | `localhost:6379` | - | - | - |
| **Grafana** | `http://localhost:3000`| `admin` | `admin` | - |
| **Prometheus** | `http://localhost:9090`| - | - | - |

> *Dica: Utilize ferramentas como **DBeaver** ou **MongoDB Compass** para visualizar os dados.*

### 4. Parar e Limpar os Serviços
Para pausar os containers temporariamente sem perder os dados:
```bash
docker compose down
```

**⚠️ Reset Completo:** Caso você altere os arquivos de inicialização (`init.sql` ou `init-mongo.js`) e queira recriar o banco do zero (apagando todos os dados), utilize a flag `-v`:
```bash
docker compose down -v
```
No próximo `docker compose up -d`, os bancos serão recriados novinhos em folha.
