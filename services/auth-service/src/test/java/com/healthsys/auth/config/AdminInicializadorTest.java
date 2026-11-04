package com.healthsys.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AdminInicializadorTest {

    @Test
    void reconheceHashBcryptESenhaEmTextoPuro() {
        assertThat(AdminInicializador.pareceBcrypt(new BCryptPasswordEncoder().encode("admin123"))).isTrue();
        assertThat(AdminInicializador.pareceBcrypt("admin123")).isFalse();
        assertThat(AdminInicializador.pareceBcrypt(null)).isFalse();
    }
}
