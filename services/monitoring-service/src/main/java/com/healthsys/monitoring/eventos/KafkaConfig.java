package com.healthsys.monitoring.eventos;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Cria o tópico deste serviço no Kafka (3 partições) e liga as tarefas agendadas (o publicador). */
@Configuration
@EnableScheduling
public class KafkaConfig {

    @Bean
    public NewTopic topicoDeEventos() {
        return TopicBuilder.name(RegistroDeEventos.TOPICO).partitions(3).replicas(1).build();
    }
}
