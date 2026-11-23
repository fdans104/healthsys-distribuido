package com.healthsys.patient.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * Tolerância a falhas do cache: se o Redis estiver fora do ar, o erro é só registrado no log
 * e a leitura segue para o PostgreSQL. O usuário não percebe nada, só fica um pouco mais lento.
 */
@Configuration
public class CacheTolerante implements CachingConfigurer, CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheTolerante.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return this;
    }

    @Override
    public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
        avisar("ler", cache, key, e);
    }

    @Override
    public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
        avisar("gravar", cache, key, e);
    }

    @Override
    public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
        avisar("apagar", cache, key, e);
    }

    @Override
    public void handleCacheClearError(RuntimeException e, Cache cache) {
        avisar("limpar", cache, null, e);
    }

    private static void avisar(String acao, Cache cache, Object key, RuntimeException e) {
        log.warn("Cache indisponível ao {} {}::{} ({}). Seguindo sem cache.", acao,
                cache == null ? "?" : cache.getName(), key, e.getMessage());
    }
}
