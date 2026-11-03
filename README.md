# 🏥 HealthSys Distribuído

[![Arquitetura](https://img.shields.io/badge/Arquitetura-Microsservi%C3%A7os-blue)](docs/diagrams/arquitetura.md)
[![Docker](https://img.shields.io/badge/Docker-Pronto-green)](docker-compose.yml)
[![Status](https://img.shields.io/badge/Sprint_1-Conclu%C3%ADda-success)](#)
[![Status](https://img.shields.io/badge/Sprint_2-Em_andamento-yellow)](#-status-da-sprint-2-semanas-34)

Plataforma de Gestão Hospitalar baseada em Arquitetura Distribuída, desenvolvida para apoiar hospitais no gerenciamento de pacientes, prontuários eletrônicos e triagens médicas com foco em alta disponibilidade e tolerância a falhas.

---

## 📁 Estrutura do Repositório

- **`docs/`**: Documentação técnica de arquitetura, diagramas (Arquitetura, Fluxo, Modelo de Dados) e relatórios.
- **`services/`**: Código-fonte de cada microsserviço independente (Spring Boot).
  - `patient-service`: Gestão cadastral de pacientes (PostgreSQL).
  - `record-service`: Prontuário eletrônico e exames (MongoDB).
  - `triage-service`: Teletriagem e classificação de risco (Kafka/PostgreSQL).
  - `auth-service`: Cadastro de usuários, autenticação e autorização (JWT/PostgreSQL). ✅ *Cadastro de usuários implementado.*
  - `notification-service`: Notificações assíncronas (Kafka/WebSocket).
- **`frontend/`**: Aplicação cliente (React + Vite, servida por nginx). ✅ *Tela de cadastro de usuários implementada.*
- **`infra/`**:
  - `docker/`: Configurações adicionais de containers.
  - `db/`: Scripts de inicialização automática de banco de dados (`postgres-init` e `mongo-init`).
  - `k8s/`: Manifests de deployment e serviços para Kubernetes.

---

## 🚀 Como Executar Localmente

Toda a stack (bancos, cache, monitoramento, backend e frontend) é containerizada. Não é preciso instalar banco de dados, Java, Maven ou Node.js na máquina: a compilação do backend e o build do frontend acontecem dentro do Docker.

### Pré-requisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop) instalado e em execução (Status: *Engine running*).
- Terminal (PowerShell, Bash ou CMD).

### 1. Iniciar os Serviços
Na raiz do repositório, execute o comando abaixo para montar as imagens e subir tudo em segundo plano:
```bash
docker compose up -d --build
```
> Na primeira execução o build leva alguns minutos (download das dependências Maven e npm). Os testes automatizados do `auth-service` rodam durante o build; se algum falhar, a imagem não é gerada.

### 2. Verificar o Status
Verificar se todos os containers subiram corretamente:
```bash
docker compose ps
```

### 3. Acessos e Credenciais

Os bancos já nascem populados com as tabelas essenciais criadas pelos scripts de inicialização.

| Serviço | Endereço | Usuário / Admin | Senha | Database Interno |
| :--- | :--- | :--- | :--- | :--- |
| **Frontend (Web)** | `http://localhost:5173` | - | - | - |
| **Auth Service (API)** | `http://localhost:8081` | - | - | `healthsys_db` |
| **PostgreSQL** | `localhost:5432` | `healthuser` | `healthpass` | `healthsys_db` |
| **MongoDB** | `localhost:27017` | `mongouser` | `mongopass` | `healthsys_records` |
| **Redis** | `localhost:6379` | - | - | - |
| **Grafana** | `http://localhost:3000`| `admin` | `admin` | - |
| **Prometheus** | `http://localhost:9090`| - | - | - |

> **Portas:** o frontend usa a `5173` (padrão do Vite) porque a `3000` já é do Grafana. A `8080` fica reservada para o futuro API Gateway; os microsserviços são numerados a partir da `8081`.

> *Dica: Utilizaremos ferramentas como **DBeaver** ou **MongoDB Compass** para visualizar os dados.*

### 4. Parar e Limpar os Serviços
Para pausar os containers temporariamente sem perder os dados:
```bash
docker compose down
```

**⚠️ Reset Completo:** Caso queiramos alterar os arquivos de inicialização (`init.sql` ou `init-mongo.js`) e recriar o banco do zero (apagando todos os dados), utilizaremos a flag `-v`:
```bash
docker compose down -v
```
No próximo `docker compose up -d`, os bancos serão recriados novinhos em folha.

---

## 👤 API de Usuários (`auth-service`)

Base: `http://localhost:8081/api/v1/usuarios` (pelo frontend: `http://localhost:5173/api/v1/usuarios`, repassado pelo nginx).

| Método | Rota | Descrição | Sucesso | Erros |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/usuarios` | Cadastra um usuário | `201` + header `Location` | `400` dados inválidos, `409` e-mail já cadastrado |
| `GET` | `/api/v1/usuarios` | Lista todos os usuários | `200` | - |
| `GET` | `/api/v1/usuarios/{id}` | Busca um usuário | `200` | `404` |
| `PUT` | `/api/v1/usuarios/{id}` | Atualiza (senha opcional; nula mantém a atual) | `200` | `400`, `404`, `409` |
| `DELETE` | `/api/v1/usuarios/{id}` | Remove um usuário | `204` | `404` |
| `GET` | `/actuator/health` | Health check | `200` | - |

Exemplo de corpo para cadastro:
```json
{ "nome": "Dra. Ana Souza", "email": "ana@hospital.com", "senha": "senhaForte123", "perfil": "MEDICO" }
```

- `perfil` aceita `ADMIN`, `MEDICO` ou `ENFERMEIRO`.
- E-mails são normalizados para minúsculas e devem ser únicos.
- Senhas têm de 8 a 72 caracteres e são gravadas apenas como hash **BCrypt**; nenhuma resposta da API devolve a senha.
- Erros seguem o formato padrão RFC 7807 (*Problem Details*); erros de validação trazem o campo `erros` com a mensagem de cada campo.

---

## 📌 Status da Sprint 2 (Semanas 3–4)

| Item | Situação |
| :--- | :--- |
| Serviço de usuários (cadastro) | ✅ Implementado |
| API REST de usuários | ✅ Implementada |
| Primeira versão da interface web (cadastro de usuários) | ✅ Implementada |
