package com.VanControl.VanControl.passageiro.service;

import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.passageiro.domain.dto.request.AtualizarPassageiroRequestDto;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroDefaultResponseDto;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroResponseDto;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.mapper.PassageiroMapper;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
import com.VanControl.VanControl.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.VanControl.VanControl.passageiro.mapper.PassageiroMapper.converterParaPassageiro;

@Service
@RequiredArgsConstructor
public class PassageiroService {

    private final PassageiroRepository passageiroRepository;

    public void cadastrarPassageiro(RegisterRequestDTO dto, User user) {
        if(passageiroRepository.findByUser_Cpf(dto.cpf()).isPresent()){
            throw new ConflictException("Passageiro já cadastrado");
        }
        var passageiro = converterParaPassageiro(dto);
        passageiro.setUser(user);
        passageiroRepository.save(passageiro);
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

    public PassageiroResponseDto atualizarPassageiro(String cpf, AtualizarPassageiroRequestDto dto) {
        var passageiro = buscarPassageiroPorCpfInterno(cpf);

        verificarPermissaoAcesso(passageiro);

        if (dto.nome() != null) {
            if(passageiro.getUser() != null) {
                passageiro.getUser().setName(dto.nome());
            }
        }
        if (dto.telefone() != null) passageiro.setTelefone(dto.telefone());
        if (dto.email() != null) {
            if (passageiro.getUser() != null) {
                passageiro.getUser().setEmail(dto.email());
            }
        }
        if (dto.intituicaoEnsino() != null) passageiro.setInstituicaoEnsino(dto.intituicaoEnsino());
        if (dto.turno() != null) passageiro.setTurno(dto.turno());
        if (dto.Endereco() != null) passageiro.setEndereco(dto.Endereco());
        if (dto.cep() != null) passageiro.setCep(dto.cep());

        var passageiroAtualizado = passageiroRepository.save(passageiro);

        return PassageiroMapper.converterParaPassageiroResponseDto(passageiroAtualizado);
    }

    @Transactional
    public PassageiroDefaultResponseDto deletarPassageiro(String cpf) {
        var passageiro = buscarPassageiroPorCpfInterno(cpf);

        verificarPermissaoAcesso(passageiro);

        passageiroRepository.delete(passageiro);
        return new PassageiroDefaultResponseDto("Passageiro deletado com sucesso");
    }

    private void verificarPermissaoAcesso(Passageiro passageiro) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assert auth != null;
        var usuarioLogado = (User) auth.getPrincipal();

        boolean hasAdminRole = auth.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if(!hasAdminRole) {
            assert usuarioLogado != null;
            if (!Objects.equals(usuarioLogado.getEmail(), passageiro.getUser().getEmail())) {
                throw new AccessDeniedException("Acesso negado");
            }
        }
    }

    private Passageiro buscarPassageiroPorCpfInterno(String cpf) {
        return passageiroRepository.findByUser_Cpf(cpf)
                .orElseThrow(() -> new NotFoundException("Passageiro não encontrado"));
    }
}
