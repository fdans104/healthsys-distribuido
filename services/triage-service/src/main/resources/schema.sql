-- Schema do triage-service (executado a cada inicialização; todos os comandos são idempotentes).
-- RNF09: este banco pertence só ao triage-service. O paciente é guardado pelo id e pelo nome
-- informados pelo patient-service, sem chave estrangeira entre bancos de serviços diferentes.
CREATE TABLE IF NOT EXISTS triagens (
    id_triagem SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL,
    paciente_nome VARCHAR(150),
    queixa_principal VARCHAR(500) NOT NULL,
    sintomas TEXT,
    pressao_arterial VARCHAR(10),
    temperatura NUMERIC(4,1),
    frequencia_cardiaca INT,
    saturacao INT,
    classificacao_risco VARCHAR(10), -- RN38: BAIXO, MEDIO, ALTO ou CRITICO
    status VARCHAR(20) NOT NULL,      -- RN40: PENDENTE, EM_ATENDIMENTO, FINALIZADA ou CANCELADA
    aberta_em TIMESTAMP NOT NULL,
    aberta_por_nome VARCHAR(150),
    aberta_por_perfil VARCHAR(30),
    avaliada_em TIMESTAMP,
    avaliada_por_nome VARCHAR(150),
    atualizada_em TIMESTAMP,
    atualizada_por_nome VARCHAR(150),
    versao BIGINT NOT NULL DEFAULT 0, -- RN54: número de edição do registro
    CONSTRAINT ck_triagens_risco CHECK (classificacao_risco IS NULL OR classificacao_risco IN ('BAIXO', 'MEDIO', 'ALTO', 'CRITICO')),
    CONSTRAINT ck_triagens_status CHECK (status IN ('PENDENTE', 'EM_ATENDIMENTO', 'FINALIZADA', 'CANCELADA')),
    -- RN39: triagem finalizada sempre tem classificação de risco (garantido também pelo banco).
    CONSTRAINT ck_triagens_finalizada_com_risco CHECK (status <> 'FINALIZADA' OR classificacao_risco IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_triagens_paciente ON triagens (id_paciente);
CREATE INDEX IF NOT EXISTS idx_triagens_status ON triagens (status);

-- Caixa de saída (transactional outbox): eventos gravados junto com a triagem e enviados depois ao Kafka.
-- Se o Kafka estiver fora do ar, os eventos esperam aqui e nenhum aviso se perde.
CREATE TABLE IF NOT EXISTS eventos_pendentes (
    id_evento BIGSERIAL PRIMARY KEY,
    evento_id VARCHAR(36) NOT NULL UNIQUE,
    topico VARCHAR(100) NOT NULL,
    chave VARCHAR(100),
    payload TEXT NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    enviado_em TIMESTAMPTZ,
    tentativas INT NOT NULL DEFAULT 0,
    ultimo_erro VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_eventos_nao_enviados ON eventos_pendentes (id_evento) WHERE enviado_em IS NULL;
