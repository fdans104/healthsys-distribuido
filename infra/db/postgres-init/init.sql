-- Inicialização do Banco Relacional (PostgreSQL) - HealthSys
CREATE TABLE IF NOT EXISTS pacientes (
    id_paciente SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    data_nascimento DATE NOT NULL,
    sexo CHAR(1),
    telefone VARCHAR(20),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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

-- Carga inicial de exemplo para testes da equipe
INSERT INTO pacientes (nome, data_nascimento, sexo, telefone) VALUES 
('Paciente Teste 1', '1990-05-15', 'M', '(85) 98888-0001'),
('Paciente Teste 2', '1985-11-20', 'F', '(85) 98888-0002')
ON CONFLICT DO NOTHING;
