package com.VanControl.VanControl.user.controller;

import com.VanControl.VanControl.user.domain.dto.request.ForgotPasswordRequestDto;
import com.VanControl.VanControl.user.domain.dto.request.LoginRequestDTO;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
import com.VanControl.VanControl.user.domain.dto.request.ResetPasswordRequestDto;
import com.VanControl.VanControl.user.domain.dto.response.DefaultResponseDto;
import com.VanControl.VanControl.user.domain.dto.response.ResponseDTO;
import com.VanControl.VanControl.common.Service.CredentialsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Login e registro de usuarios")
@SecurityRequirements
public class AuthController {

    private final CredentialsService credentialsService;

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Entrada: LoginRequestDTO (email, password). Saida: ResponseDTO (name, token)."
    )
    public ResponseEntity<ResponseDTO> login(@RequestBody LoginRequestDTO dto){
        return new ResponseEntity<>(credentialsService.login(dto), HttpStatus.OK);
    }

    @PostMapping("/register")
    @Operation(
            summary = "Registrar usuario",
            description = "Entrada: RegisterRequestDTO (name, email, password, cpf, telefone, instituicaoEnsino, turno, endereco, cep). Saida: ResponseDTO (name, token)."
    )
    public ResponseEntity<ResponseDTO> register(@RequestBody RegisterRequestDTO dto) {
        return new ResponseEntity<>(credentialsService.registrarUsuario(dto), HttpStatus.CREATED);
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Solicitar recuperação de senha",
            description = "Gera um PIN de 6 dígitos e envia por e-mail")
    public ResponseEntity<DefaultResponseDto> forgotPassword(@RequestBody ForgotPasswordRequestDto dto) {
        credentialsService.forgotPassword(dto.email());
        return new ResponseEntity<>(new DefaultResponseDto("PIN enviado no seu e-mail!"), HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Resetar a senha",
            description = "Valida o PIN e salva a nova senha")
    public ResponseEntity<DefaultResponseDto> resetPassword(@RequestBody ResetPasswordRequestDto dto) {
        credentialsService.resetPassword(dto.email(), dto.pin(), dto.newPassword());
        return new ResponseEntity<>(new DefaultResponseDto("Senha resetada com sucesso!"), HttpStatus.OK);
    }
}
