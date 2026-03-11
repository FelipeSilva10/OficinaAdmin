package dao;

import core.DiarioAula;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DiarioDAO {

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /** Insere nova entrada e retorna o UUID gerado, ou null em caso de erro. */
    public String inserir(String professorId, String turmaId, LocalDate dataAula,
                          String titulo, String conteudo, String observacoes) {
        String sql = """
            INSERT INTO diario_aulas
              (professor_id, turma_id, data_aula, titulo, conteudo, observacoes)
            VALUES (?::uuid, ?::uuid, ?::date, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, turmaId);
            stmt.setDate(3, Date.valueOf(dataAula));
            stmt.setString(4, titulo);
            stmt.setString(5, conteudo);
            stmt.setString(6, observacoes);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir diário: " + e.getMessage());
        }
        return null;
    }

    /** Atualiza título, conteúdo, observações e data de uma entrada existente. */
    public boolean atualizar(String id, LocalDate dataAula, String titulo,
                             String conteudo, String observacoes) {
        String sql = """
            UPDATE diario_aulas
            SET data_aula = ?::date, titulo = ?, conteudo = ?, observacoes = ?
            WHERE id = ?::uuid
            """;
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(dataAula));
            stmt.setString(2, titulo);
            stmt.setString(3, conteudo);
            stmt.setString(4, observacoes);
            stmt.setString(5, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar diário: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(String id) {
        String sql = "DELETE FROM diario_aulas WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir diário: " + e.getMessage());
            return false;
        }
    }

    // ── LISTAGENS ────────────────────────────────────────────────────────────

    /** Todas as entradas do professor, independente de turma. */
    public List<DiarioAula> listarPorProfessor(String professorId) {
        return buscar(BASE_SQL + "WHERE d.professor_id = ?::uuid " + ORDER_BY,
                List.of(professorId));
    }

    /** Entradas de uma turma específica do professor. */
    public List<DiarioAula> listarPorTurma(String professorId, String turmaId) {
        return buscar(
                BASE_SQL + "WHERE d.professor_id = ?::uuid AND d.turma_id = ?::uuid " + ORDER_BY,
                List.of(professorId, turmaId));
    }

    /** Busca textual por título ou conteúdo. */
    public List<DiarioAula> buscarTexto(String professorId, String termo) {
        String sql = BASE_SQL + """
            WHERE d.professor_id = ?::uuid
              AND (d.titulo      ILIKE ?
                OR d.conteudo    ILIKE ?
                OR d.observacoes ILIKE ?)
            """ + ORDER_BY;
        String like = "%" + termo + "%";
        return buscar(sql, List.of(professorId, like, like, like));
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private static final String BASE_SQL = """
        SELECT d.id, d.professor_id,
               d.turma_id, t.nome AS turma_nome, e.nome AS escola_nome,
               d.data_aula, d.titulo, d.conteudo, d.observacoes
        FROM diario_aulas d
        JOIN turmas  t ON t.id = d.turma_id
        JOIN escolas e ON e.id = t.escola_id
        """;

    private static final String ORDER_BY = "ORDER BY d.data_aula DESC, d.created_at DESC";

    private List<DiarioAula> buscar(String sql, List<String> params) {
        List<DiarioAula> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++)
                stmt.setString(i + 1, params.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar diário: " + e.getMessage());
        }
        return lista;
    }

    private DiarioAula mapear(ResultSet rs) throws SQLException {
        Date d = rs.getDate("data_aula");
        return new DiarioAula(
                rs.getString("id"),
                rs.getString("professor_id"),
                rs.getString("turma_id"),
                rs.getString("turma_nome"),
                rs.getString("escola_nome"),
                d != null ? d.toLocalDate() : LocalDate.now(),
                rs.getString("titulo"),
                rs.getString("conteudo"),
                rs.getString("observacoes")
        );
    }
}
