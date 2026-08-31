package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.motorista.service.MotoristaService;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroResumoResponseDto;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.rota.service.RotaService;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.veiculo.service.VeiculoService;
import com.VanControl.VanControl.viagem.domain.dto.request.CriarViagemRequestDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemDefaultResponseDto;
import com.VanControl.VanControl.viagemPassageiro.domain.dto.ViagemPassageirosResponseDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResponseDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResumoResponseDto;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagemPassageiro.domain.entity.ViagemPassageiro;
import com.VanControl.VanControl.viagem.mapper.ViagemMapper;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import com.VanControl.VanControl.viagem.repository.ViagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViagemService {

    private final ViagemRepository viagemRepository;
    private final ViagemPassageiroRepository viagemPassageiroRepository;
    private final PassageiroRepository passageiroRepository;
    private final RotaService rotaService;
    private final VeiculoService veiculoService;
    private final MotoristaService motoristaService;
    private final SecurityUtils securityUtils;

    public ViagemDefaultResponseDto cadastrarViagem(CriarViagemRequestDto dto) {
        rotaService.buscarRotaPorCodigo(dto.codigoRota());
        veiculoService.buscarVeiculoPorPlaca(dto.placaVeiculo());
        var motorista = motoristaService.buscarMotoristaPorCpf(dto.cpfMotorista());

        var viagem = ViagemMapper.converterParaViagem(dto);
        viagem.setDocumentoMotorista(motorista.nome());

        viagem.setCodigoViagem(
                "VIA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        viagemRepository.save(viagem);
        return new ViagemDefaultResponseDto("Viagem cadastrada.");
    }

    public ViagemResponseDto buscarViagemPorCodigo(String codigo){
        var viagem = buscarViagemPorCodigoInterno(codigo);
        validarAcessoViagem(viagem);
        return ViagemMapper.converterParaViagemDto(viagem);
    }

    public Page<ViagemResponseDto> listarTodasViagens(Pageable pageable) {
        return viagemRepository.findAll(pageable).map(ViagemMapper::converterParaViagemDto);
    }

    public ViagemDefaultResponseDto atualizarStatusViagem(String codigo) {
        var viagem = buscarViagemPorCodigoInterno(codigo);
        viagem.setViagemConcluida(true);
        viagemRepository.save(viagem);
        return new ViagemDefaultResponseDto("Status da viagem atualizado para concluída.");
    }

    @Transactional
    public ViagemDefaultResponseDto deletarViagem(String codigo) {
        var viagem = buscarViagemPorCodigoInterno(codigo);
        viagemRepository.delete(viagem);
        return new ViagemDefaultResponseDto("Viagem deletada.");
    }

    public ViagemDefaultResponseDto adicionarPassageiro(String codigo, String cpf) {
        var viagem = buscarViagemPorCodigoInterno(codigo);
        var passageiro = buscarPassageiroPorCpfInterno(cpf);

        if (viagem.isViagemConcluida()) {
            throw new BadRequestException("Viagem concluída não permite novas associações");
        }

        if (viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(viagem.getId(), passageiro.getId())) {
            throw new ConflictException("Passageiro já está associado a esta viagem");
        }

        validarLimiteCapacidade(viagem);
        validarPrazoAssociacao(viagem);

        var associacao = ViagemPassageiro.builder()
                .viagem(viagem)
                .passageiro(passageiro)
                .dataAssociacao(LocalDateTime.now())
                .build();

        viagemPassageiroRepository.save(associacao);
        return new ViagemDefaultResponseDto("Passageiro associado à viagem.");
    }

    @Transactional
    public ViagemDefaultResponseDto removerPassageiro(String codigo, String cpf) {
        var viagem = buscarViagemPorCodigoInterno(codigo);
        var passageiro = buscarPassageiroPorCpfInterno(cpf);

        if (!viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(viagem.getId(), passageiro.getId())) {
            throw new NotFoundException("Associação não encontrada");
        }

        viagemPassageiroRepository.deleteByViagem_IdAndPassageiro_Id(viagem.getId(), passageiro.getId());
        return new ViagemDefaultResponseDto("Passageiro removido da viagem.");
    }

    public ViagemPassageirosResponseDto listarPassageirosPorViagem(String codigo) {
        var viagem = buscarViagemPorCodigoInterno(codigo);
        var veiculo = veiculoService.buscarVeiculoPorPlaca(viagem.getPlacaVeiculo());
        var passageiros = viagemPassageiroRepository.findByViagem_Id(viagem.getId()).stream()
                .map(ViagemPassageiro::getPassageiro)
                .map(this::converterParaResumo)
                .toList();

        var ocupacao = viagemPassageiroRepository.countByViagem_Id(viagem.getId());

        return new ViagemPassageirosResponseDto(
                viagem.getCodigoViagem(),
                veiculo.capacidade(),
                ocupacao,
                passageiros
        );
    }

    public Page<ViagemResumoResponseDto> listarViagensPorPassageiroCpf(String cpf, Pageable pageable) {
        var passageiro = buscarPassageiroPorCpfInterno(cpf);

        return viagemPassageiroRepository.findByPassageiro_Id(passageiro.getId(), pageable)
                .map(ViagemPassageiro::getViagem)
                .map(this::converterParaResumo);
    }

    private PassageiroResumoResponseDto converterParaResumo(Passageiro passageiro) {
        return new PassageiroResumoResponseDto(
                passageiro.getUser().getName(),
                passageiro.getUser().getCpf(),
                passageiro.getUser().getEmail()
        );
    }

    private ViagemResumoResponseDto converterParaResumo(Viagem viagem) {
        return new ViagemResumoResponseDto(
                viagem.getCodigoViagem(),
                viagem.getCodigoRota(),
                viagem.getPlacaVeiculo(),
                viagem.getDataViagem(),
                viagem.getHorarioSaidaPrevisto(),
                viagem.getHorarioChegadaPrevisto(),
                viagem.isViagemConcluida()
        );
    }

    private Viagem buscarViagemPorCodigoInterno(String codigo) {
        var viagem = viagemRepository.findByCodigoViagem(codigo);
        if (viagem == null) {
            throw new NotFoundException("Viagem não encontrada");
        }
        return viagem;
    }

    private Passageiro buscarPassageiroPorCpfInterno(String cpf) {
        return passageiroRepository.findByUser_Cpf(cpf)
                .orElseThrow(() -> new NotFoundException("Passageiro não encontrado"));
    }

    private void validarAcessoViagem(Viagem viagem) {
        User user = securityUtils.getAuthenticatedUser();

        if (user.getRole() != Role.PASSAGEIRO) {
            return;
        }

        var passageiro = passageiroRepository.findByUser_Cpf(user.getCpf())
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

        boolean associado = viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(viagem.getId(), passageiro.getId());
        if (!associado) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    private void validarLimiteCapacidade(Viagem viagem) {
        var veiculo = veiculoService.buscarVeiculoPorPlaca(viagem.getPlacaVeiculo());
        var ocupacao = viagemPassageiroRepository.countByViagem_Id(viagem.getId());

        if (ocupacao >= veiculo.capacidade()) {
            throw new BadRequestException("Capacidade máxima do veículo atingida");
        }
    }

    private void validarPrazoAssociacao(Viagem viagem) {
        LocalDate dataViagem = viagem.getDataViagem();
        LocalDate limite = dataViagem.minusDays(1);

        if (LocalDate.now().isAfter(limite)) {
            throw new BadRequestException("Associar passageiro permitido apenas até um dia antes da viagem");
        }
    }
}
