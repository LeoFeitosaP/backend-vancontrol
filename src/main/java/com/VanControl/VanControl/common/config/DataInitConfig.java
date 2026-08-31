package com.VanControl.VanControl.common.config;

import com.VanControl.VanControl.common.exception.model.InternalServerErrorException;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Slf4j
public class DataInitConfig {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.admin.email:admin@vancontrol.com}")
    private String adminEmail;

    @Value("${app.admin.name:Administrador}")
    private String adminName;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createDefaultAdmin() {
        try {
            log.info("========== Iniciando verificação de criação de usuário admin ==========");
            long userCount = userRepository.count();
            log.info("Quantidade de usuários no banco: {}", userCount);
            
            if(userCount == 0){
                log.info("Nenhum usuário encontrado. Criando usuário admin...");
                User admin = new User();
                admin.setName(adminName);
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                log.info("========== ✓ Usuário admin criado com sucesso: {} ==========", adminEmail);
            } else {
                log.info("Usuários já existem no banco de dados. Pulando criação de admin.");
            }
        } catch (Exception e) {
            log.error("Erro ao criar usuário admin: ", e);
            throw new InternalServerErrorException("Falha ao inicializar usuário admin", e);
        }
    }
}
