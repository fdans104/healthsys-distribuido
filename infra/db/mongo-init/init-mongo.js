// Banco NoSQL (MongoDB) do record-service: o prontuário eletrônico (RNF09).
// Roda só na primeira vez que o container sobe com o volume vazio.
// O record-service também cria os índices sozinho ao iniciar (auto-index-creation).
db = db.getSiblingDB('healthsys_records');

// RF11: consultas médicas, RF12: exames, RF13: medicamentos. Cada registro aponta para o paciente pelo id.
db.createCollection('consultas');
db.createCollection('exames');
db.createCollection('medicamentos');

db.consultas.createIndex({ pacienteId: 1 });
db.exames.createIndex({ pacienteId: 1 });
db.medicamentos.createIndex({ pacienteId: 1 });
