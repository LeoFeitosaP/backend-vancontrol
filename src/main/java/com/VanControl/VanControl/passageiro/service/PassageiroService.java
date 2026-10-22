package com.VanControl.VanControl.passageiro.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.grafo.repository.PassageiroVeiculoDistanciaRepository;
import com.VanControl.VanControl.passageiro.domain.dto.request.AtualizarPassageiroRequestDto;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroDefaultResponseDto;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroResponseDto;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.mapper.PassageiroMapper;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
import com.VanControl.VanControl.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PassageiroService {

    private final PassageiroRepository passageiroRepository;
    private final UserRepository userRepository;
    private final EnderecoService enderecoService;
    private final PassageiroVeiculoDistanciaRepository distancias;
    private final PlatformTransactionManager transactionManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void cadastrarPassageiro(
            RegisterRequestDTO dto,
            User user,
            Endereco endereco
    ) {
        if (passageiroRepository.findByUser_Cpf(dto.cpf()).isPresent()) {
            throw new ConflictException("Passageiro já cadastrado");
        }

        var passageiro = PassageiroMapper.converterParaPassageiro(dto, endereco);
        passageiro.setUser(user);
        passageiroRepository.saveAndFlush(passageiro);
    }

    public PassageiroResponseDto buscarPassageiroPorCpf(String cpf) {
        var passageiro = buscarPassageiroPorCpfInterno(cpf);
        verificarPermissaoAcesso(passageiro);

        return PassageiroMapper.converterParaPassageiroResponseDto(passageiro);
    }

    public Page<PassageiroResponseDto> listarPassageiros(Pageable pageable) {
        return passageiroRepository.findAll(pageable)
                .map(PassageiroMapper::converterParaPassageiroResponseDto);
    }

    public PassageiroResponseDto atualizarPassageiro(
            String cpf,
            AtualizarPassageiroRequestDto dto
    ) {
        verificarPermissaoAcesso(buscarPassageiroPorCpfInterno(cpf));

        Endereco novoEndereco = dto.endereco() == null
                ? null
                : enderecoService.resolver(dto.endereco());

        return new TransactionTemplate(transactionManager).execute(status -> {
            var passageiro = buscarPassageiroPorCpfInterno(cpf);
            verificarPermissaoAcesso(passageiro);

            if (dto.email() != null) {
                if (dto.email().isBlank()) {
                    throw new BadRequestException("Informe um e-mail válido");
                }

                userRepository.findByEmail(dto.email()).ifPresent(existente -> {
                    if (!Objects.equals(
                            existente.getId(),
                            passageiro.getUser().getId()
                    )) {
                        throw new ConflictException("E-mail já cadastrado");
                    }
                });

                passageiro.getUser().setEmail(dto.email());
            }

            if (dto.nome() != null) {
                passageiro.getUser().setName(dto.nome());
            }

            if (dto.telefone() != null) {
                passageiro.setTelefone(dto.telefone());
            }

            if (dto.intituicaoEnsino() != null) {
                passageiro.setInstituicaoEnsino(dto.intituicaoEnsino());
            }

            if (dto.turno() != null) {
                passageiro.setTurno(dto.turno());
            }

            if (novoEndereco != null
                    && !novoEndereco.equals(passageiro.getEnderecoDetalhado())) {
                passageiro.definirEndereco(novoEndereco);
                distancias.deleteByPassageiro_Id(passageiro.getId());
            }

            return PassageiroMapper.converterParaPassageiroResponseDto(
                    passageiroRepository.saveAndFlush(passageiro)
            );
        });
    }

    @Transactional
    public PassageiroDefaultResponseDto deletarPassageiro(String cpf) {
        var passageiro = buscarPassageiroPorCpfInterno(cpf);
        verificarPermissaoAcesso(passageiro);

        distancias.deleteByPassageiro_Id(passageiro.getId());
        passageiroRepository.delete(passageiro);

        return new PassageiroDefaultResponseDto(
                "Passageiro deletado com sucesso"
        );
    }

    private void verificarPermissaoAcesso(Passageiro passageiro) {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof User usuario)) {
            throw new AccessDeniedException("Acesso negado");
        }

        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!admin && (
                usuario.getId() == null
                        || passageiro.getUser() == null
                        || !Objects.equals(
                        usuario.getId(),
                        passageiro.getUser().getId()
                )
        )) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    private Passageiro buscarPassageiroPorCpfInterno(String cpf) {
        return passageiroRepository.findByUser_Cpf(cpf)
                .orElseThrow(
                        () -> new NotFoundException("Passageiro não encontrado")
                );
    }
}