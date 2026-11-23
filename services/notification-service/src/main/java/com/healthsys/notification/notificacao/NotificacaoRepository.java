package com.healthsys.notification.notificacao;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificacaoRepository extends MongoRepository<Notificacao, String> {
}
