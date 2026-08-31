package com.VanControl.VanControl.common.util;

import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication; // <-- Adicionei este import
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }

        throw new AccessDeniedException("Usuário não autenticado");
    }

    public String getAuthenticatedUserCpf() {
        User user = getAuthenticatedUser();

        if (user.getCpf() == null || user.getCpf().isEmpty()) {
            throw new AccessDeniedException("Usuário não possui CPF associado");
        }

        return user.getCpf();
    }

    public void validateCpfAccess(String cpf) {
        User user = getAuthenticatedUser();

        if (user.getRole() == Role.ADMIN || user.getRole() == Role.MOTORISTA) {
            return;
        }

        String authenticatedCpf = getAuthenticatedUserCpf();
        if (!authenticatedCpf.equals(cpf)) {
            throw new AccessDeniedException("Você não tem permissão para acessar dados de outro usuário");
        }
    }
}