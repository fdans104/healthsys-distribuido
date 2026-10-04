// Inicialização do Banco NoSQL (MongoDB) - HealthSys Prontuários
db = db.getSiblingDB('healthsys_records');

db.createCollection('prontuarios');
db.prontuarios.createIndex({ "idPaciente": 1 });

db.prontuarios.insertOne({
    idPaciente: 1,
    dataCriacao: new Date(),
    historicoClinico: "Paciente relata histórico prévio de hipertensão controlada.",
    exames: [
        { tipo: "Hemograma Completo", data: new Date(), status: "Normal" }
    ],
    medicamentos: [
        { nome: "Losartana 50mg", posologia: "1 comprimido ao dia" }
    ]
});
