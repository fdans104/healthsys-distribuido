package com.healthsys.notification.notificacao;

import com.healthsys.notification.config.KafkaConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka (grupo "notification-service"). Escuta os eventos de três serviços.
 * Se este serviço ficar fora do ar, as mensagens esperam no Kafka e são lidas quando ele voltar,
 * a partir de onde parou (o Kafka guarda a posição de leitura do grupo).
 */
@Component
public class OuvinteDeEventos {

    private final NotificacaoService service;

    public OuvinteDeEventos(NotificacaoService service) {
        this.service = service;
    }

    @KafkaListener(topics = {KafkaConfig.TRIAGEM, KafkaConfig.PRONTUARIO, KafkaConfig.MONITORAMENTO},
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void receber(String mensagem) {
        service.receber(mensagem);
    }
}
