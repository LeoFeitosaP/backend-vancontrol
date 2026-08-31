package com.VanControl.VanControl.rota.service;


import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.rota.domain.dto.request.AtualizarDescricaoRotaRequestDto;
import com.VanControl.VanControl.rota.domain.dto.request.CadastrarRotaRequestDto;
import com.VanControl.VanControl.rota.domain.dto.response.RotaDefaultResponseDto;
import com.VanControl.VanControl.rota.domain.dto.response.RotaResponseDto;
import com.VanControl.VanControl.rota.domain.entity.Rota;
import com.VanControl.VanControl.rota.mapper.RotasMapper;
import com.VanControl.VanControl.rota.repository.RotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RotaService {

    private final RotaRepository rotaRepository;

    public RotaDefaultResponseDto cadastrarRota(CadastrarRotaRequestDto dto){
        if(rotaRepository.findByDescricao(dto.descricao()) != null){
            throw new ConflictException("Já existe uma rota cadastrada com essa descrição");
        }

        var rota  = RotasMapper.converterParaRota(dto);
        rota.setCodigoRota(
                "ROT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        rotaRepository.save(rota);;
        return new RotaDefaultResponseDto("Rota cadastrada com sucesso");
    }

    public RotaResponseDto buscarRotaPorCodigo(String codigo){
        var rota = rotaRepository.findByCodigoRota(codigo);
        if(rota.isEmpty()){
            throw new NotFoundException("Rota inexistente");
        }
        return RotasMapper.converterParaRotaDto(rota.get());
    }

    public Page<RotaResponseDto> buscarRotaPorDestino(String destino, Pageable pageable){
        var rota = rotaRepository.findByDestinoContainingIgnoreCase(destino, pageable);
        if(rota.isEmpty()){
            throw new NotFoundException("Rota inexistente");
        }
        return rota.map(RotasMapper::converterParaRotaDto);
    }

    public Page<RotaResponseDto> buscarTodasRotas(Pageable pageable){
        return rotaRepository.findAll(pageable).map(RotasMapper::converterParaRotaDto);
    }

    public RotaDefaultResponseDto atualizarDescricaoRota(String codigoRota, AtualizarDescricaoRotaRequestDto dto){
        Rota rota = rotaRepository.findByCodigoRota(codigoRota)
                .orElseThrow(() -> new NotFoundException("Rota não encontrada"));

        if (!rota.getDescricao().equals(dto.novaDescricao()) &&
                rotaRepository.existsByDescricao(dto.novaDescricao())) {
            throw new ConflictException("Já existe uma rota cadastrada com esta descrição.");
        }

        rota.setDescricao(dto.novaDescricao());

        rota = rotaRepository.save(rota);
        return new RotaDefaultResponseDto("Rota atualizada com sucesso");
    }

    @Transactional
    public RotaDefaultResponseDto deletarRota(String codigoRota){
        var rota = rotaRepository.findByCodigoRota(codigoRota);
        if(rota == null){
            throw new NotFoundException("Rota inexistente");
        }
        rotaRepository.deleteByCodigoRota(codigoRota);
        return new RotaDefaultResponseDto("Rota removida com sucesso");
    }



}
