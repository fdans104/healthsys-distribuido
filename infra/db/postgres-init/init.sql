-- Inicialização do Banco Relacional (PostgreSQL) - HealthSys
-- Tabela do patient-service (o serviço também roda o seu schema.sql, que atualiza bancos antigos).
CREATE TABLE IF NOT EXISTS pacientes (
    id_paciente SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) UNIQUE, -- somente dígitos
    data_nascimento DATE NOT NULL,
    sexo CHAR(1), -- M, F ou O
    telefone VARCHAR(20),
    email VARCHAR(150),
    endereco VARCHAR(255),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS atendimentos (
    id_atendimento SERIAL PRIMARY KEY,
    id_paciente INT REFERENCES pacientes(id_paciente) ON DELETE CASCADE,
    tipo_atendimento VARCHAR(50) NOT NULL,
    data TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    observacoes TEXT
);

CREATE TABLE IF NOT EXISTS vacinas (
    id_vacina SERIAL PRIMARY KEY,
    id_paciente INT REFERENCES pacientes(id_paciente) ON DELETE CASCADE,
    nome_vacina VARCHAR(100) NOT NULL,
    data_aplicacao DATE NOT NULL
);

-- Adicionar Tabela de Usuários (Serviço de Autenticação/Usuários)
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    perfil VARCHAR(50) NOT NULL, -- Ex: MEDICO, ENFERMEIRO, ADMIN
    email VARCHAR(150) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL, -- Hash BCrypt da senha (nunca a senha em texto puro)
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Adicionar Tabela de Triagem (Serviço de Triagem)
CREATE TABLE IF NOT EXISTS triagens (
    id_triagem SERIAL PRIMARY KEY,
    id_paciente INT REFERENCES pacientes(id_paciente) ON DELETE CASCADE,
    sintomas TEXT NOT NULL,
    nivel_risco VARCHAR(20) NOT NULL, -- BAIXO, MEDIO, ALTO, CRITICO
    status VARCHAR(20) DEFAULT 'Pendente', -- Pendente, Em atendimento, Finalizada, Cancelada
    data_triagem TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Carga inicial de exemplo para testes da equipe (CPFs válidos, fictícios)
INSERT INTO pacientes (nome, cpf, data_nascimento, sexo, telefone) VALUES
('Paciente Teste 1', '52998224725', '1990-05-15', 'M', '(85) 98888-0001'),
('Paciente Teste 2', '11144477735', '1985-11-20', 'F', '(85) 98888-0002')
ON CONFLICT DO NOTHING;

-- O usuário administrador padrão (admin@healthsys.com / admin123) NÃO é inserido aqui:
-- o auth-service o cria na inicialização já com a senha em hash BCrypt (ver AdminInicializador).
