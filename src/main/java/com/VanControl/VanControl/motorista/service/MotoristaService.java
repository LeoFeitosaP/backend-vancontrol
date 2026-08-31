package com.VanControl.VanControl.motorista.service;

import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.motorista.domain.dto.request.AtualizarTelefoneMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.request.CadastrarMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaDefaultResponseDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaResponseDto;
import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.motorista.mapper.MotoristaMapper;
import com.VanControl.VanControl.motorista.repository.MotoristaRepository;
import com.VanControl.VanControl.common.Service.CredentialsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MotoristaService {

    private final MotoristaRepository motoristaRepository;
    private final CredentialsService credentialsService;

    public MotoristaDefaultResponseDto cadastrarMotorista(CadastrarMotoristaRequestDto dto) {
        if(motoristaRepository.findByUser_Cpf(dto.cpf()).isPresent()){
            throw new ConflictException("Motorista já cadastrado");
        }

        var motorista = MotoristaMapper.converterParaMotorista(dto);
        var userMotorista = MotoristaMapper.converterParaRequestDto(dto);

        credentialsService.registrarUsuario(userMotorista);
        motoristaRepository.save(motorista);
        return new MotoristaDefaultResponseDto("Motorista cadastrado com sucesso");
    }

    public MotoristaResponseDto buscarMotoristaPorCpf(String cpf) {
        var motorista = buscarMotoristaPorCpfInterno(cpf);
        return MotoristaMapper.converterParaMotoristaDto(motorista);
    }

    public Page<MotoristaResponseDto> buscarTodosMotoristas(Pageable pageable) {
        return motoristaRepository.findAll(pageable)
                .map(MotoristaMapper::converterParaMotoristaDto);
    }

    public MotoristaDefaultResponseDto atualizarTelefoneMotorista(AtualizarTelefoneMotoristaRequestDto dto) {
        var motorista = buscarMotoristaPorCpfInterno(dto.cpf());
        motorista.setTelefone(dto.novoTelefone());
        motoristaRepository.save(motorista);
        return new MotoristaDefaultResponseDto("Telefone do motorista atualizado com sucesso");
    }

    @Transactional
    public MotoristaDefaultResponseDto deletarMotorista(String cpf) {
        var motorista = buscarMotoristaPorCpfInterno(cpf);
        motoristaRepository.delete(motorista);
        return new MotoristaDefaultResponseDto("Motorista deletado com sucesso");
    }

    private Motorista buscarMotoristaPorCpfInterno(String cpf) {
        return motoristaRepository.findByUser_Cpf(cpf)
                .orElseThrow(() -> new NotFoundException("Motorista não encontrado"));
    }
}
