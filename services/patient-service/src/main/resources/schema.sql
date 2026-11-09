-- Schema do patient-service (executado a cada inicialização; todos os comandos são idempotentes).
-- Em um banco novo, o infra/db/postgres-init/init.sql já cria a tabela completa e nada aqui muda.
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

-- Completa os pacientes de exemplo antigos, que não tinham CPF.
UPDATE pacientes SET cpf = '52998224725' WHERE nome = 'Paciente Teste 1' AND cpf IS NULL;
UPDATE pacientes SET cpf = '11144477735' WHERE nome = 'Paciente Teste 2' AND cpf IS NULL;
