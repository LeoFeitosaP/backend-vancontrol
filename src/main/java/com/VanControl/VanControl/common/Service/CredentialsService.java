package com.VanControl.VanControl.common.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.passageiro.service.PassageiroService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.user.domain.dto.request.LoginRequestDTO;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
import com.VanControl.VanControl.user.domain.dto.response.ResponseDTO;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CredentialsService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PassageiroService passageiroService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public ResponseDTO registrarUsuario(RegisterRequestDTO dto) {
        User user = criarUsuario(dto, Role.PASSAGEIRO);
        passageiroService.cadastrarPassageiro(dto, user);
        userRepository.flush();

        return new ResponseDTO(
                user.getName(),
                tokenService.generateToken(user)
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public User criarUsuarioMotorista(RegisterRequestDTO dto) {
        return criarUsuario(dto, Role.MOTORISTA);
    }

    private User criarUsuario(RegisterRequestDTO dto, Role role) {
        if (dto.email() == null || dto.email().isBlank()) {
            throw new BadRequestException("Informe o e-mail");
        }

        if (dto.cpf() == null || dto.cpf().isBlank()) {
            throw new BadRequestException("Informe o CPF");
        }

        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new ConflictException("E-mail já cadastrado");
        }

        if (userRepository.existsByCpfNormalizado(dto.cpf())) {
            throw new ConflictException("CPF já cadastrado");
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setCpf(dto.cpf());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(role);

        return userRepository.saveAndFlush(user);
    }

    public ResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (passwordEncoder.matches(dto.password(), user.getPassword())) {
            return new ResponseDTO(
                    user.getName(),
                    tokenService.generateToken(user)
            );
        }

        throw new BadRequestException("Credenciais inválidas");
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String pin = String.format(
                "%06d",
                new java.util.Random().nextInt(999999)
        );

        user.setResetPassword(pin);
        user.setExpirationPin(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.enviarEmailToken(
                user.getEmail(),
                user.getName(),
                pin
        );
    }

    public void resetPassword(String email, String pin, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getResetPassword() == null
                || !user.getResetPassword().equals(pin)) {
            throw new RuntimeException("Código PIN inválido");
        }

        if (user.getExpirationPin().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Código PIN expirado");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPassword(null);
        user.setExpirationPin(null);

        userRepository.save(user);
    }
}