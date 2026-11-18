-- Schema do patient-service (executado a cada inicialização; todos os comandos são idempotentes).
-- Em um banco novo, o infra/db/postgres-patient-init/init.sql já cria as tabelas completas e nada aqui muda.
CREATE TABLE IF NOT EXISTS pacientes (
    id_paciente SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(11),
    data_nascimento DATE NOT NULL,
    sexo CHAR(1),
    telefone VARCHAR(20),
    email VARCHAR(150),
    endereco VARCHAR(255),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP
);

-- Bancos criados pela versão anterior do init.sql não tinham estas colunas.
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS cpf VARCHAR(11);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS email VARCHAR(150);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS endereco VARCHAR(255);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS atualizado_em TIMESTAMP;
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS versao BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_pacientes_cpf ON pacientes (cpf);

-- Ficha clínica (RF06, RF08, RF09). Cada registro guarda quem registrou, como num prontuário.
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
CREATE TABLE IF NOT EXISTS alergias (
    id_alergia SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES pacientes(id_paciente) ON DELETE CASCADE,
    descricao VARCHAR(150) NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE atendimentos ADD COLUMN IF NOT EXISTS registrado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE atendimentos ADD COLUMN IF NOT EXISTS registrado_por_id INT;
ALTER TABLE atendimentos ADD COLUMN IF NOT EXISTS registrado_por_nome VARCHAR(150);
ALTER TABLE atendimentos ADD COLUMN IF NOT EXISTS registrado_por_perfil VARCHAR(30);

ALTER TABLE vacinas ADD COLUMN IF NOT EXISTS dose VARCHAR(30);
ALTER TABLE vacinas ADD COLUMN IF NOT EXISTS lote VARCHAR(30);
ALTER TABLE vacinas ADD COLUMN IF NOT EXISTS registrado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE vacinas ADD COLUMN IF NOT EXISTS registrado_por_id INT;
ALTER TABLE vacinas ADD COLUMN IF NOT EXISTS registrado_por_nome VARCHAR(150);
ALTER TABLE vacinas ADD COLUMN IF NOT EXISTS registrado_por_perfil VARCHAR(30);

ALTER TABLE alergias ADD COLUMN IF NOT EXISTS registrado_por_id INT;
ALTER TABLE alergias ADD COLUMN IF NOT EXISTS registrado_por_nome VARCHAR(150);
ALTER TABLE alergias ADD COLUMN IF NOT EXISTS registrado_por_perfil VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_atendimentos_paciente ON atendimentos (id_paciente);
CREATE INDEX IF NOT EXISTS idx_vacinas_paciente ON vacinas (id_paciente);
CREATE INDEX IF NOT EXISTS idx_alergias_paciente ON alergias (id_paciente);

-- Completa os pacientes de exemplo antigos, que não tinham CPF.
UPDATE pacientes SET cpf = '52998224725' WHERE nome = 'Paciente Teste 1' AND cpf IS NULL;
UPDATE pacientes SET cpf = '11144477735' WHERE nome = 'Paciente Teste 2' AND cpf IS NULL;
