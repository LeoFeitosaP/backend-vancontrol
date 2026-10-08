package com.VanControl.VanControl.veiculo.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.grafo.repository.PassageiroVeiculoDistanciaRepository;
import com.VanControl.VanControl.veiculo.domain.dto.request.AtualizarStatusVeiculoRequestDto;
import com.VanControl.VanControl.veiculo.domain.dto.request.CadastrarVeiculoRequestDto;
import com.VanControl.VanControl.veiculo.domain.dto.response.VeiculoDefaultResponseDto;
import com.VanControl.VanControl.veiculo.domain.dto.response.VeiculoResponseDto;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.veiculo.domain.enums.StatusEnum;
import com.VanControl.VanControl.veiculo.mapper.VeiculoMapper;
import com.VanControl.VanControl.veiculo.repository.VeiculoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Objects;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final EnderecoService enderecoService;
    private final PassageiroVeiculoDistanciaRepository distancias;
    private final TransactionTemplate transacao;

    public VeiculoService(
            VeiculoRepository veiculoRepository,
            EnderecoService enderecoService,
            PassageiroVeiculoDistanciaRepository distancias,
            PlatformTransactionManager transactionManager
    ) {
        this.veiculoRepository = veiculoRepository;
        this.enderecoService = enderecoService;
        this.distancias = distancias;
        this.transacao = new TransactionTemplate(transactionManager);
    }

    public VeiculoDefaultResponseDto cadastrarVeiculo(
            CadastrarVeiculoRequestDto dto
    ) {
        if (veiculoRepository.findByPlaca(dto.placa()) != null) {
            throw new ConflictException("Veículo já cadastrado");
        }

        var endereco = enderecoService.resolver(dto.endereco());

        return transacao.execute(status -> {
            veiculoRepository.saveAndFlush(
                    VeiculoMapper.converterParaVeiculo(dto, endereco)
            );

            return new VeiculoDefaultResponseDto(
                    "Veículo cadastrado com sucesso"
            );
        });
    }

    public VeiculoResponseDto atualizarEndereco(
            String placa,
            EnderecoRequestDto dto
    ) {
        buscarVeiculoPorPlacaInterno(placa);

        var endereco = enderecoService.resolver(dto);

        return transacao.execute(status -> {
            var veiculo = buscarVeiculoPorPlacaInterno(placa);

            if (!Objects.equals(veiculo.getEnderecoBase(), endereco)) {
                veiculo.definirEndereco(endereco);

                distancias.deleteByVeiculo_idVeiculo(
                        veiculo.getIdVeiculo()
                );

                veiculoRepository.saveAndFlush(veiculo);
            }

            return VeiculoMapper.converterParaVeiculoDto(veiculo);
        });
    }

    public VeiculoResponseDto buscarVeiculoPorPlaca(String placa) {
        return VeiculoMapper.converterParaVeiculoDto(
                buscarVeiculoPorPlacaInterno(placa)
        );
    }

    public Page<VeiculoResponseDto> listarVeiculos(Pageable pageable) {
        return veiculoRepository.findAll(pageable)
                .map(VeiculoMapper::converterParaVeiculoDto);
    }

    @Transactional
    public VeiculoDefaultResponseDto atualizarStatusVeiculo(
            AtualizarStatusVeiculoRequestDto dto
    ) {
        var veiculo = buscarVeiculoPorPlacaInterno(dto.placa());

        try {
            veiculo.setStatus(
                    StatusEnum.valueOf(
                            dto.status().toUpperCase(Locale.ROOT)
                    )
            );
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Status de veículo inválido");
        }

        veiculoRepository.save(veiculo);

        return new VeiculoDefaultResponseDto(
                "Status do veículo atualizado com sucesso"
        );
    }

    @Transactional
    public VeiculoDefaultResponseDto deletarVeiculo(String placa) {
        var veiculo = buscarVeiculoPorPlacaInterno(placa);
        veiculoRepository.delete(veiculo);

        return new VeiculoDefaultResponseDto(
                "Veículo deletado com sucesso"
        );
    }

    private Veiculo buscarVeiculoPorPlacaInterno(String placa) {
        var veiculo = veiculoRepository.findByPlaca(placa);

        if (veiculo == null) {
            throw new NotFoundException("Veículo não encontrado");
        }

        return veiculo;
    }
}