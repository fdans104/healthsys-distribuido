package com.healthsys.patient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthsys.patient.paciente.dto.PacienteResponse;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Cache distribuído no Redis para a ficha cadastral do paciente.
 * - Guarda o paciente em JSON por 10 minutos (chave "pacientes::{id}").
 * - "transactionAware": a cópia só é gravada/apagada depois que a transação do banco confirma,
 *   então o cache nunca mostra uma edição que não foi salva.
 * - Se o Redis cair, o CacheTolerante deixa o serviço buscar direto no PostgreSQL.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PACIENTES = "pacientes";

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheDePacientes(ObjectMapper mapper) {
        Jackson2JsonRedisSerializer<PacienteResponse> json = new Jackson2JsonRedisSerializer<>(mapper, PacienteResponse.class);
        RedisCacheConfiguration pacientes = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(json));
        return builder -> builder.transactionAware().withCacheConfiguration(PACIENTES, pacientes);
    }
}
