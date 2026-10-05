### 1. Entidades Relacionais (PostgreSQL)
Este diagrama reflete os dados altamente estruturados gerenciados pelos microsserviços de **Usuários**, **Pacientes** e **Triagem**.

```mermaid
erDiagram
    USUARIOS {
        int id_usuario PK
        string nome
        string perfil "MEDICO, ENFERMEIRO, ADMIN"
        string email UK
        string senha_hash "Bcrypt"
        timestamp criado_em
    }
    
    PACIENTES {
        int id_paciente PK
        string nome
        date data_nascimento
        string sexo
        string telefone
        timestamp criado_em
    }
    
    ATENDIMENTOS {
        int id_atendimento PK
        int id_paciente FK
        string tipo_atendimento
        timestamp data
        text observacoes
    }
    
    VACINAS {
        int id_vacina PK
        int id_paciente FK
        string nome_vacina
        date data_aplicacao
    }
    
    TRIAGENS {
        int id_triagem PK
        int id_paciente FK
        string nivel_risco "BAIXO, MEDIO, ALTO, CRITICO"
        string status "Pendente, Em atendimento, Finalizada"
        text sintomas
        timestamp data_triagem
    }

    PACIENTES ||--o{ ATENDIMENTOS : "possui"
    PACIENTES ||--o{ VACINAS : "recebe"
    PACIENTES ||--o{ TRIAGENS : "passa por"
```

---

### 2. Entidades Orientadas a Documentos (MongoDB)
Este diagrama reflete a estrutura flexível JSON (NoSQL) gerenciada pelo microsserviço de **Prontuários Eletrônicos**, onde um único "Documento de Prontuário" pode conter infinitos subarrays (exames, consultas, medicamentos) sem a necessidade de _Joins_ complexos.

```mermaid
classDiagram
    class Prontuario_Documento {
        +ObjectId _id
        +Integer idPaciente_Referencia
        +Date dataCriacao
        +String historicoClinicoGeral
        +Array~Exame~ exames
        +Array~Medicamento~ medicamentos
        +Array~Consulta~ consultasClinicas
    }
    
    class Exame {
        +String tipoExame
        +Date dataRealizacao
        +String resultado
        +String status
    }
    
    class Medicamento {
        +String nome
        +String dosagem
        +String posologia
        +String observacoes
    }
    
    class Consulta {
        +String medicoResponsavel
        +Date dataConsulta
        +String diagnostico
        +String cid
    }
    
    Prontuario_Documento *-- Exame : contém lista de
    Prontuario_Documento *-- Medicamento : contém lista de
    Prontuario_Documento *-- Consulta : contém lista de
```