-- Schema do monitoring-service (executado a cada inicialização; todos os comandos são idempotentes).
-- RNF09: este banco pertence só ao monitoring-service. O paciente é guardado pelo id e pelo nome
-- informados pelo patient-service, sem chave estrangeira entre bancos de serviços diferentes.

CREATE TABLE IF NOT EXISTS leitos (
    id_leito SERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    setor VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'LIVRE',
    id_paciente INT,
    paciente_nome VARCHAR(150),
    ocupado_em TIMESTAMP,
    ocupado_por_nome VARCHAR(150),
    atualizado_em TIMESTAMP,
    atualizado_por_nome VARCHAR(150),
    versao BIGINT NOT NULL DEFAULT 0, -- RN54: número de edição do registro
    CONSTRAINT ck_leitos_setor CHECK (setor IN ('UTI', 'ENFERMARIA', 'EMERGENCIA')),
    CONSTRAINT ck_leitos_status CHECK (status IN ('LIVRE', 'OCUPADO', 'HIGIENIZACAO')),
    -- Leito ocupado sempre tem paciente; leito livre ou em limpeza nunca tem.
    CONSTRAINT ck_leitos_ocupado_tem_paciente CHECK ((status = 'OCUPADO') = (id_paciente IS NOT NULL))
);

-- Um paciente só ocupa um leito por vez (garantido também pelo banco).
CREATE UNIQUE INDEX IF NOT EXISTS uq_leitos_paciente ON leitos (id_paciente) WHERE id_paciente IS NOT NULL;

-- Leitos iniciais do hospital de demonstração (só na primeira vez, com a tabela vazia).
-- Os códigos são os mesmos usados pelo simulador de monitores (infra/iot-simulator).
INSERT INTO leitos (codigo, setor, status)
SELECT v.codigo, v.setor, 'LIVRE'
FROM (VALUES ('UTI-01', 'UTI'), ('UTI-02', 'UTI'), ('UTI-03', 'UTI'), ('UTI-04', 'UTI'),
             ('ENF-01', 'ENFERMARIA'), ('ENF-02', 'ENFERMARIA'), ('ENF-03', 'ENFERMARIA'),
             ('ENF-04', 'ENFERMARIA'), ('ENF-05', 'ENFERMARIA'), ('ENF-06', 'ENFERMARIA'),
             ('EMG-01', 'EMERGENCIA'), ('EMG-02', 'EMERGENCIA')) AS v(codigo, setor)
WHERE NOT EXISTS (SELECT 1 FROM leitos);

CREATE TABLE IF NOT EXISTS alertas (
    id_alerta SERIAL PRIMARY KEY,
    leito_codigo VARCHAR(20) NOT NULL,
    setor VARCHAR(20),
    id_paciente INT NOT NULL,
    paciente_nome VARCHAR(150),
    tipo VARCHAR(30) NOT NULL,
    gravidade VARCHAR(10) NOT NULL,
    valor VARCHAR(30),
    mensagem VARCHAR(300) NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    reconhecido_em TIMESTAMP,
    reconhecido_por_nome VARCHAR(150),
    CONSTRAINT ck_alertas_gravidade CHECK (gravidade IN ('ATENCAO', 'CRITICO'))
);

CREATE INDEX IF NOT EXISTS idx_alertas_leito_tipo ON alertas (leito_codigo, tipo, criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_alertas_ativos ON alertas (criado_em DESC) WHERE reconhecido_em IS NULL;

-- Caixa de saída (transactional outbox): eventos gravados junto com a mudança e enviados depois ao Kafka.
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
