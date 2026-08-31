package com.VanControl.VanControl.common.Service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.passageiro.service.PassageiroService;
import com.VanControl.VanControl.user.domain.dto.request.LoginRequestDTO;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
import com.VanControl.VanControl.user.domain.dto.response.ResponseDTO;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CredentialsService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PassageiroService passageiroService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ResponseDTO registrarUsuario(RegisterRequestDTO dto) {
        Optional<User> user = this.userRepository.findByEmail(dto.email());

        if(user.isEmpty()) {
            User newUser = new User();
            newUser.setPassword(passwordEncoder.encode(dto.password()));
            newUser.setEmail(dto.email());
            newUser.setName(dto.name());
            newUser.setCpf(dto.cpf());
            if (dto.instituicaoEnsino() == null) {
                newUser.setRole(Role.MOTORISTA);
            } else {
                newUser.setRole(Role.PASSAGEIRO);
            }
            this.userRepository.save(newUser);

            if (newUser.getRole() == Role.PASSAGEIRO) {
                this.passageiroService.cadastrarPassageiro(dto, newUser);
            }

            String token = this.tokenService.generateToken(newUser);
            return new ResponseDTO(newUser.getName(), token);
        }

        throw new ConflictException("Passageiro já cadastrado");
    }

    public ResponseDTO login(LoginRequestDTO dto) {
        User user = this.userRepository.findByEmail(dto.email()).orElseThrow(() -> new NotFoundException("User not found"));

        if(passwordEncoder.matches(dto.password(), user.getPassword())){
            String token = this.tokenService.generateToken(user);
            return new ResponseDTO(user.getName(), token);
        }

        throw new BadRequestException("Credenciais inválidas");
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String pin = String.format("%06d", new java.util.Random().nextInt(999999));

        user.setResetPassword(pin);
        user.setExpirationPin(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.enviarEmailToken(user.getEmail(), user.getName(), pin);
    }

    public void resetPassword(String email, String pin, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getResetPassword() == null || !user.getResetPassword().equals(pin)) {
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
