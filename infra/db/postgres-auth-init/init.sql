-- Banco do auth-service (usuários e login).
-- RNF09: este banco pertence só ao auth-service; nenhum outro serviço tem acesso a ele.
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    perfil VARCHAR(50) NOT NULL, -- Ex: MEDICO, ENFERMEIRO, ADMIN
    email VARCHAR(150) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL, -- Hash BCrypt da senha (nunca a senha em texto puro)
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- O usuário administrador padrão (admin@healthsys.com / admin123) NÃO é inserido aqui:
-- o auth-service o cria na inicialização já com a senha em hash BCrypt (ver AdminInicializador).
