package dao;

import core.CronogramaAula;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CronogramaDAO {

    // ─────────────────────────────────────────────────────────────────────────
    // INSERT / UPDATE / DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Admin cria um slot recorrente (AULA) com período definido.
     * tipo = 'AULA', criado_por = 'ADMIN'
     */
    public boolean inserirAdmin(String professorId, String turmaId, String diaSemana,
                                String horarioInicio, String horarioFim,
                                LocalDate dataInicio, LocalDate dataFim) {
        return inserirInterno(professorId, turmaId, diaSemana, horarioInicio, horarioFim,
                "AULA", dataInicio, dataFim, "ADMIN");
    }

    /**
     * Admin ou professor cria um evento ocasional (REUNIÃO ou AULA_SUBSTITUTA).
     * O diaSemana é derivado da data específica pelo chamador.
     */
    public boolean inserirOcasional(String professorId, String turmaId, String diaSemana,
                                    String horarioInicio, String horarioFim,
                                    String tipo, LocalDate dataEspecifica, String criadoPor) {
        return inserirInterno(professorId, turmaId, diaSemana, horarioInicio, horarioFim,
                tipo, dataEspecifica, dataEspecifica, criadoPor);
    }

    private boolean inserirInterno(String professorId, String turmaId, String diaSemana,
                                   String horarioInicio, String horarioFim, String tipo,
                                   LocalDate dataInicio, LocalDate dataFim, String criadoPor) {
        String sql = """
            INSERT INTO cronograma_aulas
              (professor_id, turma_id, dia_semana, horario_inicio, horario_fim,
               tipo, data_inicio, data_fim, criado_por)
            VALUES (?::uuid, ?::uuid, ?, ?::time, ?::time, ?, ?, ?, ?)
            """;
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, turmaId);
            stmt.setString(3, diaSemana);
            stmt.setString(4, horarioInicio);
            stmt.setString(5, horarioFim);
            stmt.setString(6, tipo);
            if (dataInicio != null) stmt.setDate(7, Date.valueOf(dataInicio));
            else                    stmt.setNull(7, Types.DATE);
            if (dataFim != null)    stmt.setDate(8, Date.valueOf(dataFim));
            else                    stmt.setNull(8, Types.DATE);
            stmt.setString(9, criadoPor);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir cronograma: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(String id, String diaSemana,
                             String horarioInicio, String horarioFim,
                             LocalDate dataInicio, LocalDate dataFim) {
        String sql = """
            UPDATE cronograma_aulas
            SET dia_semana = ?, horario_inicio = ?::time, horario_fim = ?::time,
                data_inicio = ?, data_fim = ?
            WHERE id = ?::uuid
            """;
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, diaSemana);
            stmt.setString(2, horarioInicio);
            stmt.setString(3, horarioFim);
            if (dataInicio != null) stmt.setDate(4, Date.valueOf(dataInicio));
            else                    stmt.setNull(4, Types.DATE);
            if (dataFim != null)    stmt.setDate(5, Date.valueOf(dataFim));
            else                    stmt.setNull(5, Types.DATE);
            stmt.setString(6, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar cronograma: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(String id) {
        String sql = "DELETE FROM cronograma_aulas WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir cronograma: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTAGENS
    // ─────────────────────────────────────────────────────────────────────────

    /** Todos os slots do professor (para a view do cronograma do professor). */
    public List<CronogramaAula> listarPorProfessor(String professorId) {
        String sql = BASE_SELECT +
                "WHERE ca.professor_id = ?::uuid " +
                ORDER_DIA;
        return buscar(sql, List.of(professorId));
    }

    /**
     * Slots ativos para o professor em determinado dia e data.
     * Respeita data_inicio e data_fim do cronograma.
     * Usado pela Chamada inteligente.
     */
    public List<CronogramaAula> listarAtivosParaDia(String professorId,
                                                    String diaSemana,
                                                    LocalDate data) {
        String sql = BASE_SELECT + """
            WHERE ca.professor_id = ?::uuid
              AND ca.dia_semana   = ?
              AND (ca.data_inicio IS NULL OR ca.data_inicio <= ?)
              AND (ca.data_fim    IS NULL OR ca.data_fim    >= ?)
            ORDER BY ca.horario_inicio
            """;
        List<CronogramaAula> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, diaSemana);
            stmt.setDate(3, Date.valueOf(data));
            stmt.setDate(4, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar cronograma por dia: " + e.getMessage());
        }
        return lista;
    }

    /** Todos os slots de todos os professores — para a view admin. */
    public List<CronogramaAula> listarTodos() {
        String sql = BASE_SELECT + ORDER_DIA;
        return buscar(sql, List.of());
    }

    /** Slots de um professor específico — para a view admin filtrada. */
    public List<CronogramaAula> listarTodosPorProfessor(String professorId) {
        return listarPorProfessor(professorId);
    }

    /** Mantém compatibilidade com chamadas antigas via (professorId, diaSemana). */
    public List<CronogramaAula> listarPorProfessorEDia(String professorId, String diaSemana) {
        return listarAtivosParaDia(professorId, diaSemana, LocalDate.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static final String BASE_SELECT = """
        SELECT ca.id, ca.professor_id, pf.nome AS professor_nome,
               ca.turma_id, t.nome AS turma_nome,
               ca.dia_semana,
               TO_CHAR(ca.horario_inicio, 'HH24:MI') AS horario_inicio,
               TO_CHAR(ca.horario_fim,    'HH24:MI') AS horario_fim,
               ca.tipo, ca.criado_por,
               ca.data_inicio::text AS data_inicio,
               ca.data_fim::text    AS data_fim
        FROM cronograma_aulas ca
        JOIN turmas t  ON t.id  = ca.turma_id
        JOIN perfis pf ON pf.id = ca.professor_id
        """;

    private static final String ORDER_DIA = """
        ORDER BY
          CASE ca.dia_semana
            WHEN 'SEGUNDA' THEN 1 WHEN 'TERÇA'  THEN 2
            WHEN 'QUARTA'  THEN 3 WHEN 'QUINTA' THEN 4
            WHEN 'SEXTA'   THEN 5 WHEN 'SÁBADO' THEN 6
          END, ca.horario_inicio
        """;

    private List<CronogramaAula> buscar(String sql, List<String> params) {
        List<CronogramaAula> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++)
                stmt.setString(i + 1, params.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar cronograma: " + e.getMessage());
        }
        return lista;
    }

    private CronogramaAula mapear(ResultSet rs) throws SQLException {
        return new CronogramaAula(
                rs.getString("id"),
                rs.getString("professor_id"),
                rs.getString("professor_nome"),
                rs.getString("turma_id"),
                rs.getString("turma_nome"),
                rs.getString("dia_semana"),
                rs.getString("horario_inicio"),
                rs.getString("horario_fim"),
                rs.getString("tipo"),
                rs.getString("data_inicio"),
                rs.getString("data_fim"),
                rs.getString("criado_por")
        );
    }
}