package com.healthsys.auth.config;

import com.healthsys.auth.usuario.Perfil;
import com.healthsys.auth.usuario.Usuario;
import com.healthsys.auth.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Garante, ao iniciar, que exista um administrador para o primeiro login.
 * - Se não existir: cria com ADMIN_EMAIL / ADMIN_SENHA.
 * - Se existir com a senha em texto puro (carga antiga do init.sql): regrava como hash BCrypt.
 */
@Component
public class AdminInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicializador.class);
    private static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminInicializador(UsuarioRepository repository,
                              PasswordEncoder passwordEncoder,
                              @Value("${healthsys.admin.email}") String email,
                              @Value("${healthsys.admin.senha}") String senha) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        repository.findByEmail(email).ifPresentOrElse(this::corrigirSenhaLegada, this::criarAdmin);
    }

    private void corrigirSenhaLegada(Usuario admin) {
        if (!pareceBcrypt(admin.getSenhaHash())) {
            admin.setSenhaHash(passwordEncoder.encode(admin.getSenhaHash()));
            repository.save(admin);
            log.warn("Senha do usuário {} estava em texto puro e foi convertida para hash BCrypt.", email);
        }
    }

    private void criarAdmin() {
        try {
            repository.saveAndFlush(new Usuario("Administrador", Perfil.ADMIN, email, passwordEncoder.encode(senha)));
            log.info("Administrador padrão {} criado.", email);
        } catch (DataIntegrityViolationException e) {
            // Outra réplica do serviço criou o admin ao mesmo tempo: nada a fazer.
            log.info("Administrador padrão {} já foi criado por outra instância.", email);
        }
    }

    static boolean pareceBcrypt(String valor) {
        return valor != null && BCRYPT.matcher(valor).matches();
    }
}
