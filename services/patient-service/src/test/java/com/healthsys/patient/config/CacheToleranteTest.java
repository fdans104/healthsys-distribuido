package com.healthsys.patient.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Teste de tolerância a falhas: com o Redis fora do ar, nenhuma operação de cache derruba a requisição. */
class CacheToleranteTest {

    private final CacheTolerante tolerante = new CacheTolerante();
    private final RedisConnectionFailureException redisFora = new RedisConnectionFailureException("Redis fora do ar");

    @Test
    void erroDoRedisNaoInterrompeALeituraNemAGravacao() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn(CacheConfig.PACIENTES);

        assertThatCode(() -> tolerante.errorHandler().handleCacheGetError(redisFora, cache, 1)).doesNotThrowAnyException();
        assertThatCode(() -> tolerante.errorHandler().handleCachePutError(redisFora, cache, 1, "paciente")).doesNotThrowAnyException();
        assertThatCode(() -> tolerante.errorHandler().handleCacheEvictError(redisFora, cache, 1)).doesNotThrowAnyException();
        assertThatCode(() -> tolerante.errorHandler().handleCacheClearError(redisFora, cache)).doesNotThrowAnyException();
    }
}
