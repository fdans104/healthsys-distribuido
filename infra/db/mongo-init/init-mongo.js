// Banco NoSQL (MongoDB) do record-service: o prontuário eletrônico (RNF09).
// Roda só na primeira vez que o container sobe com o volume vazio.
// Aqui só criamos as coleções. Os índices (pacienteId etc.) são criados pelo próprio record-service
// ao iniciar (auto-index-creation): criar aqui também gera conflito de nome de índice.
db = db.getSiblingDB('healthsys_records');

// RF11: consultas médicas, RF12: exames, RF13: medicamentos. Cada registro aponta para o paciente pelo id.
db.createCollection('consultas');
db.createCollection('exames');
db.createCollection('medicamentos');
