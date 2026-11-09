-- Banco do patient-service (pacientes e registros clínicos básicos).
-- RNF09: este banco pertence só ao patient-service; nenhum outro serviço tem acesso a ele.
-- (O patient-service também roda o seu schema.sql, que atualiza bancos criados antes desta versão.)
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
    atualizado_em TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0 -- RN54: número de edição do registro
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

-- RF08: alergias sempre vinculadas a um paciente (idAlergia, idPaciente, descricao).
CREATE TABLE IF NOT EXISTS alergias (
    id_alergia SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES pacientes(id_paciente) ON DELETE CASCADE,
    descricao VARCHAR(150) NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- A tabela "triagens" não fica aqui: ela pertence ao triage-service, que terá o seu próprio banco.
-- (Uma chave estrangeira não atravessa bancos de serviços diferentes.)

-- Carga inicial de exemplo para testes da equipe (CPFs válidos, fictícios)
INSERT INTO pacientes (nome, cpf, data_nascimento, sexo, telefone) VALUES
('Paciente Teste 1', '52998224725', '1990-05-15', 'M', '(85) 98888-0001'),
('Paciente Teste 2', '11144477735', '1985-11-20', 'F', '(85) 98888-0002')
ON CONFLICT DO NOTHING;
