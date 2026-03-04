package dao;

import core.CronogramaAula;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CronogramaDAO {

    // ── Inserir / Atualizar ────────────────────────────────────────────────────

    public boolean inserir(String professorId, String turmaId, String diaSemana,
                           String horarioInicio, String horarioFim) {
        String sql = """
            INSERT INTO cronograma_aulas
              (professor_id, turma_id, dia_semana, horario_inicio, horario_fim)
            VALUES (?::uuid, ?::uuid, ?, ?::time, ?::time)
            """;
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, turmaId);
            stmt.setString(3, diaSemana);
            stmt.setString(4, horarioInicio);
            stmt.setString(5, horarioFim);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir cronograma: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(String id, String diaSemana,
                             String horarioInicio, String horarioFim) {
        String sql = """
            UPDATE cronograma_aulas
            SET dia_semana = ?, horario_inicio = ?::time, horario_fim = ?::time
            WHERE id = ?::uuid
            """;
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, diaSemana);
            stmt.setString(2, horarioInicio);
            stmt.setString(3, horarioFim);
            stmt.setString(4, id);
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

    // ── Listagens ──────────────────────────────────────────────────────────────

    /** Todos os slots do professor, com nome da turma. */
    public List<CronogramaAula> listarPorProfessor(String professorId) {
        String sql = """
            SELECT ca.id, ca.professor_id, ca.turma_id, t.nome as turma_nome,
                   ca.dia_semana,
                   TO_CHAR(ca.horario_inicio, 'HH24:MI') as horario_inicio,
                   TO_CHAR(ca.horario_fim,    'HH24:MI') as horario_fim
            FROM cronograma_aulas ca
            JOIN turmas t ON t.id = ca.turma_id
            WHERE ca.professor_id = ?::uuid
            ORDER BY
              CASE ca.dia_semana
                WHEN 'SEGUNDA' THEN 1 WHEN 'TERÇA'  THEN 2
                WHEN 'QUARTA'  THEN 3 WHEN 'QUINTA' THEN 4
                WHEN 'SEXTA'   THEN 5 WHEN 'SÁBADO' THEN 6
              END,
              ca.horario_inicio
            """;
        return buscar(sql, professorId);
    }

    /** Slots do professor para um dia específico — usado pelo módulo de chamada. */
    public List<CronogramaAula> listarPorProfessorEDia(String professorId, String diaSemana) {
        String sql = """
            SELECT ca.id, ca.professor_id, ca.turma_id, t.nome as turma_nome,
                   ca.dia_semana,
                   TO_CHAR(ca.horario_inicio, 'HH24:MI') as horario_inicio,
                   TO_CHAR(ca.horario_fim,    'HH24:MI') as horario_fim
            FROM cronograma_aulas ca
            JOIN turmas t ON t.id = ca.turma_id
            WHERE ca.professor_id = ?::uuid AND ca.dia_semana = ?
            ORDER BY ca.horario_inicio
            """;
        List<CronogramaAula> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, diaSemana);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar cronograma por dia: " + e.getMessage());
        }
        return lista;
    }

    /** Slots de uma turma específica do professor. */
    public List<CronogramaAula> listarPorTurma(String professorId, String turmaId) {
        String sql = """
            SELECT ca.id, ca.professor_id, ca.turma_id, t.nome as turma_nome,
                   ca.dia_semana,
                   TO_CHAR(ca.horario_inicio, 'HH24:MI') as horario_inicio,
                   TO_CHAR(ca.horario_fim,    'HH24:MI') as horario_fim
            FROM cronograma_aulas ca
            JOIN turmas t ON t.id = ca.turma_id
            WHERE ca.professor_id = ?::uuid AND ca.turma_id = ?::uuid
            ORDER BY
              CASE ca.dia_semana
                WHEN 'SEGUNDA' THEN 1 WHEN 'TERÇA'  THEN 2
                WHEN 'QUARTA'  THEN 3 WHEN 'QUINTA' THEN 4
                WHEN 'SEXTA'   THEN 5 WHEN 'SÁBADO' THEN 6
              END
            """;
        List<CronogramaAula> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, turmaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar cronograma por turma: " + e.getMessage());
        }
        return lista;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<CronogramaAula> buscar(String sql, String param) {
        List<CronogramaAula> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
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
                rs.getString("turma_id"),
                rs.getString("turma_nome"),
                rs.getString("dia_semana"),
                rs.getString("horario_inicio"),
                rs.getString("horario_fim")
        );
    }
}