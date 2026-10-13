package com.VanControl.VanControl.grafo.repository;

import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.*;
import com.VanControl.VanControl.grafo.domain.dto.GeografiaPersistida.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GeografiaRepository {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;

    public GeografiaRepository(
            JdbcTemplate jdbc,
            PlatformTransactionManager manager
    ) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(manager);
    }

    public Localizacao buscar(Tipo tipo, String chave) {
        String vinculo = switch (tipo) {
            case PASSAGEIRO ->
                    "join passageiro p on p.id=g.passageiro_id "
                            + "join users u on u.id=p.user_id where u.cpf=?";

            case VEICULO ->
                    "join veiculo v on v.id_veiculo=g.veiculo_id "
                            + "where v.placa=?";

            case VIAGEM ->
                    "join viagem v on v.id=g.viagem_id "
                            + "where v.codigo_viagem=?";
        };

        return unico(
                jdbc.query(
                        "select g.* from geo_localizacao g " + vinculo,
                        (rs, n) -> localizacao(rs),
                        chave
                )
        );
    }

    public Localizacao salvarResultado(
            Localizacao anterior,
            Geocodificacao resultado
    ) {
        Ponto p = resultado.coordenadas();

        var rows = jdbc.query("""
                update geo_localizacao
                set estado=?,
                    latitude=?,
                    longitude=?,
                    mensagem=?,
                    atribuicao=?,
                    consultado_em=current_timestamp
                where id=? and revisao=?
                returning *
                """,
                (rs, n) -> localizacao(rs),
                resultado.estado().name(),
                p == null ? null : p.latitude(),
                p == null ? null : p.longitude(),
                resultado.mensagem(),
                resultado.atribuicao(),
                anterior.id(),
                anterior.revisao()
        );

        if (rows.isEmpty()) {
            throw desatualizado();
        }

        return rows.getFirst();
    }

    public Localizacao revisarOrigem(
            String codigo,
            Endereco e,
            String justificativa
    ) {
        return transacao.execute(status -> {
            var viagens = jdbc.query("""
                    select id,
                           data_viagem,
                           horario_saida_previsto,
                           viagem_concluida
                    from viagem
                    where codigo_viagem=?
                    for update
                    """,
                    (rs, n) -> new Object[]{
                            rs.getObject("id", UUID.class),
                            rs.getDate("data_viagem"),
                            rs.getTime("horario_saida_previsto"),
                            rs.getBoolean("viagem_concluida")
                    },
                    codigo
            );

            if (viagens.isEmpty()) {
                throw new NotFoundException("Viagem não encontrada");
            }

            var atual = buscar(Tipo.VIAGEM, codigo);
            Object[] v = viagens.getFirst();

            if (!"REVISAO_NECESSARIA".equals(atual.origemEndereco())) {
                var data = (java.sql.Date) v[1];
                var hora = (java.sql.Time) v[2];

                if ((boolean) v[3]
                        || data == null
                        || hora == null
                        || !LocalDateTime.of(
                        data.toLocalDate(),
                        hora.toLocalTime()
                ).isAfter(
                        LocalDateTime.now(
                                ZoneId.of("America/Sao_Paulo")
                        )
                )) {
                    throw new ConflictException(
                            "Origem de viagem iniciada ou concluída não pode mudar"
                    );
                }
            }

            return unico(
                    jdbc.query("""
                            update geo_localizacao
                            set cep=?,
                                logradouro=?,
                                numero=?,
                                complemento=?,
                                bairro=?,
                                cidade=?,
                                uf=?,
                                origem_endereco='REVISAO_MANUAL',
                                justificativa=?
                            where viagem_id=?
                            returning *
                            """,
                            (rs, n) -> localizacao(rs),
                            e.getCep(),
                            e.getLogradouro(),
                            e.getNumero(),
                            e.getComplemento(),
                            e.getBairro(),
                            e.getCidade(),
                            e.getUf(),
                            justificativa.trim(),
                            v[0]
                    )
            );
        });
    }

    public Optional<Calculo> consultarDistancia(
            Localizacao origem,
            Localizacao destino,
            boolean estimativa
    ) {
        return jdbc.query("""
                select d.*
                from geo_distancia d
                join geo_localizacao o on o.id=d.origem_id
                join geo_localizacao p on p.id=d.destino_id
                where d.origem_id=?
                    and d.destino_id=?
                    and d.permitir_estimativa=?
                    and d.origem_revisao=o.revisao
                    and d.destino_revisao=p.revisao
                    and o.estado='CONFIRMADA'
                    and p.estado='CONFIRMADA'
                    and d.expira_em>current_timestamp
                """,
                (rs, n) -> calculo(rs),
                origem.id(),
                destino.id(),
                estimativa
        ).stream().findFirst();
    }

    public Calculo salvarDistancia(
            Localizacao origem,
            Localizacao destino,
            boolean estimativa,
            Distancia resultado
    ) {
        return transacao.execute(status -> {
            var pontos = jdbc.query("""
                    select *
                    from geo_localizacao
                    where id in (?,?)
                    order by id
                    for update
                    """,
                    (rs, n) -> localizacao(rs),
                    origem.id(),
                    destino.id()
            );

            if (pontos.size() != 2 || pontos.stream().anyMatch(p ->
                    !"CONFIRMADA".equals(p.estado())
                            || p.revisao() != (
                            p.id().equals(origem.id())
                                    ? origem.revisao()
                                    : destino.revisao()
                    ))) {
                throw desatualizado();
            }

            int validadeSegundos =
                    resultado.estado() == EstadoDistancia.CALCULADA
                            ? 86400
                            : 300;

            return jdbc.queryForObject("""
                    insert into geo_distancia (
                        origem_id,
                        destino_id,
                        permitir_estimativa,
                        origem_revisao,
                        destino_revisao,
                        estado,
                        metodo,
                        distancia_km,
                        motivo,
                        atribuicao,
                        expira_em
                    )
                    values (
                        ?,?,?,?,?,?,?,?,?,?,
                        current_timestamp + (? * interval '1 second')
                    )
                    on conflict (
                        origem_id,
                        destino_id,
                        permitir_estimativa
                    )
                    do update set
                        origem_revisao=excluded.origem_revisao,
                        destino_revisao=excluded.destino_revisao,
                        estado=excluded.estado,
                        metodo=excluded.metodo,
                        distancia_km=excluded.distancia_km,
                        motivo=excluded.motivo,
                        atribuicao=excluded.atribuicao,
                        calculado_em=current_timestamp,
                        expira_em=excluded.expira_em
                    returning *
                    """,
                    (rs, n) -> calculo(rs),
                    origem.id(),
                    destino.id(),
                    estimativa,
                    origem.revisao(),
                    destino.revisao(),
                    resultado.estado().name(),
                    resultado.metodo() == null
                            ? null
                            : resultado.metodo().name(),
                    resultado.distanciaKm(),
                    resultado.motivo(),
                    resultado.atribuicao(),
                    validadeSegundos
            );
        });
    }

    public Map<UUID, Double> distanciasRodoviarias(UUID passageiroId) {
        Map<UUID, Double> resultado = new HashMap<>();

        jdbc.query("""
                select distinct on (o.viagem_id)
                    o.viagem_id,
                    d.distancia_km
                from geo_distancia d
                join geo_localizacao o on o.id=d.origem_id
                join geo_localizacao p on p.id=d.destino_id
                where p.passageiro_id=?
                    and o.viagem_id is not null
                    and d.metodo='RODOVIARIA'
                    and d.estado='CALCULADA'
                    and d.expira_em>current_timestamp
                    and o.estado='CONFIRMADA'
                    and p.estado='CONFIRMADA'
                    and d.origem_revisao=o.revisao
                    and d.destino_revisao=p.revisao
                order by o.viagem_id,d.calculado_em desc
                """,
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        resultado.put(
                                rs.getObject(1, UUID.class),
                                rs.getDouble(2)
                        ),
                passageiroId
        );

        return resultado;
    }

    private static Localizacao unico(List<Localizacao> rows) {
        if (rows.isEmpty()) {
            throw new NotFoundException("Localização não encontrada");
        }

        if (rows.size() != 1) {
            throw new ConflictException(
                    "Identificador duplicado; revise o cadastro"
            );
        }

        return rows.getFirst();
    }

    private static ConflictException desatualizado() {
        return new ConflictException(
                "Endereço ou geocodificação mudou durante a consulta; repita a operação"
        );
    }

    private static Localizacao localizacao(ResultSet rs) throws SQLException {
        var data = rs.getTimestamp("consultado_em");
        Double lat = rs.getObject("latitude", Double.class);

        return new Localizacao(
                rs.getObject("id", UUID.class),

                new Endereco(
                        rs.getString("cep"),
                        rs.getString("logradouro"),
                        rs.getString("numero"),
                        rs.getString("complemento"),
                        rs.getString("bairro"),
                        rs.getString("cidade"),
                        rs.getString("uf")
                ),

                rs.getString("origem_endereco"),
                rs.getString("justificativa"),
                rs.getLong("revisao"),
                rs.getString("estado"),

                lat == null
                        ? null
                        : new Ponto(lat, rs.getDouble("longitude")),

                rs.getString("mensagem"),
                rs.getString("atribuicao"),
                data == null ? null : data.toInstant()
        );
    }

    private static Calculo calculo(ResultSet rs) throws SQLException {
        String metodo = rs.getString("metodo");

        return new Calculo(
                rs.getObject("origem_id", UUID.class),
                rs.getObject("destino_id", UUID.class),
                rs.getLong("origem_revisao"),
                rs.getLong("destino_revisao"),
                rs.getBoolean("permitir_estimativa"),

                new Distancia(
                        EstadoDistancia.valueOf(rs.getString("estado")),
                        metodo == null
                                ? null
                                : MetodoDistancia.valueOf(metodo),
                        rs.getObject("distancia_km", Double.class),
                        rs.getString("motivo"),
                        rs.getString("atribuicao")
                ),

                rs.getTimestamp("calculado_em").toInstant(),
                rs.getTimestamp("expira_em").toInstant()
        );
    }
}