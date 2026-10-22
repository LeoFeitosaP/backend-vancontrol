package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.grafo.domain.entity.ArestaPassageiroVeiculo;
import com.VanControl.VanControl.grafo.service.EmparelhamentoService;
import com.VanControl.VanControl.motorista.repository.MotoristaRepository;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroResumoResponseDto;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.rota.repository.RotaRepository;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.veiculo.repository.VeiculoRepository;
import com.VanControl.VanControl.viagem.domain.dto.request.CriarViagemRequestDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemDefaultResponseDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResponseDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResumoResponseDto;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagem.mapper.ViagemMapper;
import com.VanControl.VanControl.viagem.repository.ViagemRepository;
import com.VanControl.VanControl.viagemPassageiro.domain.dto.ViagemPassageirosResponseDto;
import com.VanControl.VanControl.viagemPassageiro.domain.entity.ViagemPassageiro;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViagemService {

    private final ViagemRepository viagemRepository;
    private final ViagemPassageiroRepository viagemPassageiroRepository;
    private final PassageiroRepository passageiroRepository;
    private final EmparelhamentoService emparelhamentoService;
    private final RotaRepository rotaRepository;
    private final VeiculoRepository veiculoRepository;
    private final MotoristaRepository motoristaRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public ViagemDefaultResponseDto cadastrarViagem(CriarViagemRequestDto dto) {
        var rota = rotaRepository.findByCodigoRota(dto.codigoRota())
                .orElseThrow(() -> new NotFoundException("Rota inexistente"));

        var veiculo = veiculoRepository.findByPlaca(dto.placaVeiculo());

        if (veiculo == null) {
            throw new NotFoundException("Veículo não encontrado");
        }

        String cpf = dto.cpfMotorista() == null
                ? ""
                : dto.cpfMotorista().trim();

        if (!cpf.matches(
                "[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}"
        )) {
            throw new BadRequestException(
                    "Informe o CPF do motorista com 11 dígitos"
            );
        }

        cpf = cpf.replace(".", "").replace("-", "");

        var motoristas = motoristaRepository.findAllByCpfNormalizado(cpf);

        if (motoristas.isEmpty()) {
            throw new NotFoundException("Motorista não encontrado");
        }

        if (motoristas.size() != 1) {
            throw new ConflictException(
                    "CPF associado a mais de um motorista; revise o cadastro"
            );
        }

        var viagem = ViagemMapper.converterParaViagem(
                dto,
                rota,
                veiculo,
                motoristas.getFirst()
        );

        viagem.setCodigoViagem(
                "VIA-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        viagemRepository.saveAndFlush(viagem);
        return new ViagemDefaultResponseDto("Viagem cadastrada.");
    }

    @Transactional(readOnly = true)
    public ViagemResponseDto buscarViagemPorCodigo(String codigo) {
        var viagem = buscarViagemPorCodigoInterno(codigo);
        validarAcessoViagem(viagem);
        return ViagemMapper.converterParaViagemDto(viagem);
    }

    @Transactional(readOnly = true)
    public Page<ViagemResponseDto> listarTodasViagens(Pageable pageable) {
        User usuarioLogado = securityUtils.getAuthenticatedUser();

        if (usuarioLogado.getRole() != Role.PASSAGEIRO) {
            return viagemRepository.findAll(pageable)
                    .map(ViagemMapper::converterParaViagemDto);
        }

        var passageiro = passageiroRepository.findByUser_Cpf(
                usuarioLogado.getCpf()
        ).orElseThrow(
                () -> new NotFoundException("Passageiro não encontrado")
        );

        var viagensNaoConcluidas = viagemRepository.findAll().stream()
                .filter(v -> !v.isViagemConcluida())
                .toList();

        var recomendadas =
                emparelhamentoService.recomendarViagensParaPassageiro(
                        passageiro,
                        viagensNaoConcluidas
                );

        var dtosOrdenados = recomendadas.stream()
                .map(ArestaPassageiroVeiculo::viagem)
                .map(ViagemMapper::converterParaViagemDto)
                .toList();

        return paginarManualmente(dtosOrdenados, pageable);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ViagemDefaultResponseDto atualizarStatusViagem(String codigo) {
        var viagem = buscarViagemParaAtualizacao(codigo);
        viagem.setViagemConcluida(true);
        viagemRepository.save(viagem);

        return new ViagemDefaultResponseDto(
                "Status da viagem atualizado para concluída."
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ViagemDefaultResponseDto deletarViagem(String codigo) {
        var viagem = buscarViagemParaAtualizacao(codigo);
        viagemRepository.delete(viagem);
        return new ViagemDefaultResponseDto("Viagem deletada.");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ViagemDefaultResponseDto adicionarPassageiro(
            String codigo,
            String cpf
    ) {
        var viagem = buscarViagemParaAtualizacao(codigo);

        var veiculo = veiculoRepository.buscarParaAtualizacao(
                viagem.getVeiculo().getIdVeiculo()
        );

        if (veiculo == null) {
            throw new NotFoundException("Veículo não encontrado");
        }

        viagem.setVeiculo(veiculo);

        var passageiro = buscarPassageiroPorCpfInterno(cpf);

        if (viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(
                viagem.getId(),
                passageiro.getId()
        )) {
            throw new ConflictException(
                    "Passageiro já está associado a esta viagem"
            );
        }

        long ocupacao = viagemPassageiroRepository.countByViagem_Id(
                viagem.getId()
        );

        RegrasReserva.validar(
                viagem,
                ocupacao,
                LocalDate.now(RegrasReserva.FUSO)
        );

        var associacao = ViagemPassageiro.builder()
                .viagem(viagem)
                .passageiro(passageiro)
                .dataAssociacao(LocalDateTime.now(RegrasReserva.FUSO))
                .build();

        viagemPassageiroRepository.saveAndFlush(associacao);

        return new ViagemDefaultResponseDto(
                "Passageiro associado à viagem."
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ViagemDefaultResponseDto removerPassageiro(
            String codigo,
            String cpf
    ) {
        var viagem = buscarViagemParaAtualizacao(codigo);
        var passageiro = buscarPassageiroPorCpfInterno(cpf);

        if (!viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(
                viagem.getId(),
                passageiro.getId()
        )) {
            throw new NotFoundException("Associação não encontrada");
        }

        viagemPassageiroRepository.deleteByViagem_IdAndPassageiro_Id(
                viagem.getId(),
                passageiro.getId()
        );

        return new ViagemDefaultResponseDto(
                "Passageiro removido da viagem."
        );
    }

    @Transactional(readOnly = true)
    public ViagemPassageirosResponseDto listarPassageirosPorViagem(
            String codigo
    ) {
        var viagem = buscarViagemPorCodigoInterno(codigo);
        var veiculo = viagem.getVeiculo();

        var passageiros =
                viagemPassageiroRepository.findByViagem_Id(viagem.getId())
                        .stream()
                        .map(ViagemPassageiro::getPassageiro)
                        .map(this::converterParaResumo)
                        .toList();

        var ocupacao =
                viagemPassageiroRepository.countByViagem_Id(viagem.getId());

        return new ViagemPassageirosResponseDto(
                viagem.getCodigoViagem(),
                veiculo.getCapacidade(),
                ocupacao,
                passageiros
        );
    }

    @Transactional(readOnly = true)
    public Page<ViagemResumoResponseDto> listarViagensPorPassageiroCpf(
            String cpf,
            Pageable pageable
    ) {
        var passageiro = buscarPassageiroPorCpfInterno(cpf);
        var pageableConsulta = prepararPaginacaoViagensPassageiro(pageable);

        var pagina = viagemPassageiroRepository.findByPassageiro_Id(
                passageiro.getId(),
                pageableConsulta
        );

        var conteudo = pagina.getContent().stream()
                .map(ViagemPassageiro::getViagem)
                .map(this::converterParaResumo)
                .toList();

        return new PageImpl<>(
                conteudo,
                pageable,
                pagina.getTotalElements()
        );
    }

    private Pageable prepararPaginacaoViagensPassageiro(Pageable pageable) {
        Sort ordenacao = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "dataViagem");

        List<Sort.Order> ordens = ordenacao.stream()
                .map(ordem -> ordem.withProperty(
                        propriedadeOrdenacaoViagensPassageiro(
                                ordem.getProperty()
                        )
                ))
                .toList();

        Sort ordenacaoConsulta = Sort.by(ordens);

        if (ordenacaoConsulta.getOrderFor("id") == null) {
            ordenacaoConsulta = ordenacaoConsulta.and(Sort.by("id"));
        }

        if (pageable.isUnpaged()) {
            return Pageable.unpaged(ordenacaoConsulta);
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ordenacaoConsulta
        );
    }

    private String propriedadeOrdenacaoViagensPassageiro(
            String propriedade
    ) {
        return switch (propriedade) {
            case "codigoViagem", "viagem.codigoViagem" ->
                    "viagem.codigoViagem";

            case "codigoRota", "viagem.codigoRota" ->
                    "viagem.codigoRota";

            case "placaVeiculo", "viagem.placaVeiculo" ->
                    "viagem.placaVeiculo";

            case "dataViagem", "viagem.dataViagem" ->
                    "viagem.dataViagem";

            case "horarioSaidaPrevisto", "viagem.horarioSaidaPrevisto" ->
                    "viagem.horarioSaidaPrevisto";

            case "horarioChegadaPrevisto", "viagem.horarioChegadaPrevisto" ->
                    "viagem.horarioChegadaPrevisto";

            case "viagemConcluida", "viagem.viagemConcluida" ->
                    "viagem.viagemConcluida";

            case "dataAssociacao", "id" -> propriedade;

            default -> throw new BadRequestException(
                    "Campo de ordenação inválido para viagens do passageiro: "
                            + propriedade
            );
        };
    }

    private PassageiroResumoResponseDto converterParaResumo(
            Passageiro passageiro
    ) {
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

    private Viagem buscarViagemParaAtualizacao(String codigo) {
        var viagem = viagemRepository.buscarParaAtualizacao(codigo);

        if (viagem == null) {
            throw new NotFoundException("Viagem não encontrada");
        }

        return viagem;
    }

    private Passageiro buscarPassageiroPorCpfInterno(String cpf) {
        return passageiroRepository.findByUser_Cpf(cpf)
                .orElseThrow(
                        () -> new NotFoundException("Passageiro não encontrado")
                );
    }

    private void validarAcessoViagem(Viagem viagem) {
        User user = securityUtils.getAuthenticatedUser();

        if (user.getRole() != Role.PASSAGEIRO) {
            return;
        }

        var passageiro = passageiroRepository.findByUser_Cpf(user.getCpf())
                .orElseThrow(
                        () -> new AccessDeniedException("Acesso negado")
                );

        boolean associado =
                viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(
                        viagem.getId(),
                        passageiro.getId()
                );

        if (!associado) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    private Page<ViagemResponseDto> paginarManualmente(
            List<ViagemResponseDto> lista,
            Pageable pageable
    ) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(lista);
        }

        if (pageable.getOffset() >= lista.size()) {
            return new PageImpl<>(List.of(), pageable, lista.size());
        }

        int inicio = (int) pageable.getOffset();
        int fim = Math.min(inicio + pageable.getPageSize(), lista.size());

        return new PageImpl<>(
                lista.subList(inicio, fim),
                pageable,
                lista.size()
        );
    }
}