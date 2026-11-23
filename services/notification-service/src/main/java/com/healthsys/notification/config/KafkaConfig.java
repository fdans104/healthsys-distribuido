package com.healthsys.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Tópicos que este serviço escuta e o tratamento de mensagens com problema.
 * - Se o processamento falhar (ex.: MongoDB fora do ar), tenta de novo 5 vezes, com 2 segundos de intervalo.
 * - Se continuar falhando, a mensagem vai para o tópico "<nome>.DLT" (dead letter topic),
 *   em vez de travar a fila ou ser perdida. Ela pode ser analisada e reprocessada depois.
 */
@Configuration
public class KafkaConfig {

    public static final String TRIAGEM = "triagem.eventos";
    public static final String PRONTUARIO = "prontuario.eventos";
    public static final String MONITORAMENTO = "monitoramento.eventos";

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public DefaultErrorHandler tratamentoDeErros(KafkaTemplate<String, String> kafka) {
        DeadLetterPublishingRecoverer paraDlt = new DeadLetterPublishingRecoverer(kafka);
        DefaultErrorHandler tratamento = new DefaultErrorHandler(paraDlt, new FixedBackOff(2000L, 5L));
        tratamento.setRetryListeners((registro, erro, tentativa) ->
                log.warn("Falha ao processar evento do tópico {} (tentativa {}): {}",
                        registro.topic(), tentativa, erro.getMessage()));
        return tratamento;
    }

    @Bean
    public NewTopic topicoTriagem() {
        return TopicBuilder.name(TRIAGEM).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicoProntuario() {
        return TopicBuilder.name(PRONTUARIO).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicoMonitoramento() {
        return TopicBuilder.name(MONITORAMENTO).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic dltTriagem() {
        return TopicBuilder.name(TRIAGEM + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic dltProntuario() {
        return TopicBuilder.name(PRONTUARIO + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic dltMonitoramento() {
        return TopicBuilder.name(MONITORAMENTO + ".DLT").partitions(3).replicas(1).build();
    }
}
