package com.VanControl.VanControl.viagem.repository;

import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.MetodoDistancia;
import com.VanControl.VanControl.viagem.domain.dto.request.ConsultarRecomendacoesRequestDto;
import com.VanControl.VanControl.viagem.domain.dto.response.RecomendacaoViagemResponseDto;
import com.VanControl.VanControl.viagem.domain.dto.response.RecomendacaoViagemResponseDto.Distancia;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResponseDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public class RecomendacaoRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public RecomendacaoRepository(JdbcTemplate jdbc) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public List<RecomendacaoViagemResponseDto> buscar(
            UUID passageiroId,
            UUID rotaId,
            ConsultarRecomendacoesRequestDto filtro,
            LocalDate hoje
    ) {
        var parametros = new MapSqlParameterSource()
                .addValue("passageiro", passageiroId)
                .addValue("rota", rotaId)
                .addValue(
                        "data",
                        java.sql.Date.valueOf(filtro.dataViagem())
                )
                .addValue(
                        "hoje",
                        java.sql.Date.valueOf(hoje)
                )
                .addValue(
                        "de",
                        Timestamp.valueOf(filtro.chegadaDe())
                )
                .addValue(
                        "ate",
                        Timestamp.valueOf(filtro.chegadaAte())
                );

        return jdbc.query("""
                with ocupacao as (
                    select vp.viagem_id, count(*) as total
                    from viagem_passageiro vp
                    join viagem v on v.id=vp.viagem_id
                    where v.rota_id=:rota
                        and v.data_viagem=:data
                    group by vp.viagem_id
                ), elegiveis as (
                    select
                        v.*,
                        r.destino,
                        e.capacidade-coalesce(oc.total,0) as vagas,
                        v.data_viagem + v.horario_chegada_previsto
                            + case
                                when v.horario_chegada_previsto
                                        < v.horario_saida_previsto
                                then interval '1 day'
                                else interval '0 day'
                            end as chegada
                    from viagem v
                    join rota r on r.id=v.rota_id
                    join veiculo e on e.id_veiculo=v.veiculo_id
                    left join ocupacao oc on oc.viagem_id=v.id
                    where v.rota_id=:rota
                        and v.data_viagem=:data
                        and v.data_viagem>:hoje
                        and v.viagem_concluida=false
                        and v.horario_saida_previsto is not null
                        and v.horario_chegada_previsto is not null
                        and e.status='DISPONIVEL'
                        and e.capacidade>coalesce(oc.total,0)
                        and not exists (
                            select 1
                            from viagem_passageiro vp
                            where vp.viagem_id=v.id
                                and vp.passageiro_id=:passageiro
                        )
                )
                select
                    v.codigo_viagem,
                    v.codigo_rota,
                    v.placa_veiculo,
                    v.documento_motorista,
                    v.data_viagem,
                    v.horario_saida_previsto,
                    v.horario_chegada_previsto,
                    v.viagem_concluida,
                    v.destino,
                    v.vagas,
                    v.chegada,
                    coalesce(o.estado,'AUSENTE') as estado_origem,
                    coalesce(p.estado,'AUSENTE') as estado_passageiro,
                    case
                        when o.estado is distinct from 'CONFIRMADA'
                            or p.estado is distinct from 'CONFIRMADA'
                        then 'GEOCODIFICACAO_PENDENTE'
                        else coalesce(
                            d.estado,
                            'NAO_CALCULADA_OU_EXPIRADA'
                        )
                    end as estado_distancia,
                    d.metodo,
                    d.distancia_km,
                    d.calculado_em,
                    d.expira_em,
                    d.atribuicao
                from elegiveis v
                left join geo_localizacao o on o.viagem_id=v.id
                left join geo_localizacao p
                    on p.passageiro_id=:passageiro
                left join lateral (
                    select d.*
                    from geo_distancia d
                    where d.origem_id=o.id
                        and d.destino_id=p.id
                        and d.origem_revisao=o.revisao
                        and d.destino_revisao=p.revisao
                        and o.estado='CONFIRMADA'
                        and p.estado='CONFIRMADA'
                        and d.expira_em>statement_timestamp()
                    order by
                        d.calculado_em desc,
                        d.permitir_estimativa asc
                    limit 1
                ) d on true
                where v.chegada between :de and :ate
                    and (
                        d.estado is null
                        or d.estado not in (
                            'SEM_ROTA',
                            'PONTO_FORA_DA_VIA'
                        )
                    )
                """,
                parametros,
                (rs, n) -> converter(rs)
        );
    }

    private RecomendacaoViagemResponseDto converter(
            ResultSet rs
    ) throws SQLException {
        String metodo = rs.getString("metodo");

        return new RecomendacaoViagemResponseDto(
                new ViagemResponseDto(
                        rs.getString("codigo_viagem"),
                        rs.getString("codigo_rota"),
                        rs.getString("placa_veiculo"),
                        rs.getString("documento_motorista"),
                        rs.getDate("data_viagem").toLocalDate(),
                        rs.getObject(
                                "horario_saida_previsto",
                                LocalTime.class
                        ),
                        rs.getObject(
                                "horario_chegada_previsto",
                                LocalTime.class
                        ),
                        rs.getBoolean("viagem_concluida")
                ),
                rs.getString("destino"),
                rs.getTimestamp("chegada").toLocalDateTime(),
                rs.getLong("vagas"),
                new Distancia(
                        rs.getString("estado_distancia"),
                        metodo == null
                                ? null
                                : MetodoDistancia.valueOf(metodo),
                        rs.getObject("distancia_km", Double.class),
                        rs.getString("estado_origem"),
                        rs.getString("estado_passageiro"),
                        instante(rs, "calculado_em"),
                        instante(rs, "expira_em"),
                        rs.getString("atribuicao")
                ),
                null
        );
    }

    private Instant instante(
            ResultSet rs,
            String coluna
    ) throws SQLException {
        Timestamp valor = rs.getTimestamp(coluna);
        return valor == null ? null : valor.toInstant();
    }
}