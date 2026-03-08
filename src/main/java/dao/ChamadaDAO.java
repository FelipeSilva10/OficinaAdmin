package dao;

import core.Chamada;
import core.ChamadaPresenca;
import core.RegistroHoras;
import core.ResumoTurma;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChamadaDAO {

    // ─────────────────────────────────────────────────────────────────────────
    // CHAMADAS
    // ─────────────────────────────────────────────────────────────────────────

    public String abrirChamada(String professorId, String turmaId,
                               String cronogramaId, LocalDate dataAula,
                               String horarioInicio, String horarioFim,
                               List<ChamadaPresenca> presencas) {
        String sqlChamada = """
            INSERT INTO chamadas
              (professor_id, turma_id, cronograma_id, data_aula, horario_inicio, horario_fim)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?::date, ?::time, ?::time)
            RETURNING id
            """;
        String sqlPresenca = """
            INSERT INTO chamada_presencas (chamada_id, aluno_id, presente)
            VALUES (?::uuid, ?::uuid, ?)
            ON CONFLICT (chamada_id, aluno_id) DO UPDATE SET presente = EXCLUDED.presente
            """;
        try (Connection conn = ConexaoBD.conectar()) {
            conn.setAutoCommit(false);
            try {
                String chamadaId;
                try (PreparedStatement stmt = conn.prepareStatement(sqlChamada)) {
                    stmt.setString(1, professorId);
                    stmt.setString(2, turmaId);
                    if (cronogramaId != null) stmt.setString(3, cronogramaId);
                    else                       stmt.setNull(3, Types.OTHER);
                    stmt.setDate(4, Date.valueOf(dataAula));
                    stmt.setString(5, horarioInicio);
                    stmt.setString(6, horarioFim);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Chamada não inserida.");
                        chamadaId = rs.getString("id");
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement(sqlPresenca)) {
                    for (ChamadaPresenca p : presencas) {
                        stmt.setString(1, chamadaId);
                        stmt.setString(2, p.getAlunoId());
                        stmt.setBoolean(3, p.isPresente());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
                conn.commit();
                return chamadaId;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erro ao abrir chamada (rollback): " + e.getMessage());
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Erro de conexão: " + e.getMessage());
            return null;
        }
    }

    public boolean excluirChamada(String chamadaId) {
        // Deleta presenças e chamada em transação
        String sqlPres = "DELETE FROM chamada_presencas WHERE chamada_id = ?::uuid";
        String sqlCham = "DELETE FROM chamadas WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement s = conn.prepareStatement(sqlPres)) {
                    s.setString(1, chamadaId); s.executeUpdate();
                }
                try (PreparedStatement s = conn.prepareStatement(sqlCham)) {
                    s.setString(1, chamadaId);
                    int rows = s.executeUpdate();
                    conn.commit();
                    return rows > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erro ao excluir chamada: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erro de conexão: " + e.getMessage());
            return false;
        }
    }

    public boolean chamadaJaExiste(String professorId, String turmaId, LocalDate data) {
        String sql = """
            SELECT 1 FROM chamadas
            WHERE professor_id = ?::uuid AND turma_id = ?::uuid AND data_aula = ?::date
            """;
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, turmaId);
            stmt.setDate(3, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    public List<Chamada> listarPorProfessor(String professorId) {
        String sql = """
            SELECT c.id, c.professor_id, c.turma_id, t.nome as turma_nome,
                   c.cronograma_id, c.data_aula,
                   TO_CHAR(c.horario_inicio, 'HH24:MI') as horario_inicio,
                   TO_CHAR(c.horario_fim,    'HH24:MI') as horario_fim,
                   COUNT(cp.id) as total_alunos,
                   COUNT(cp.id) FILTER (WHERE cp.presente) as total_presentes
            FROM chamadas c
            JOIN turmas t ON t.id = c.turma_id
            LEFT JOIN chamada_presencas cp ON cp.chamada_id = c.id
            WHERE c.professor_id = ?::uuid
            GROUP BY c.id, t.nome
            ORDER BY c.data_aula DESC, c.horario_inicio
            """;
        List<Chamada> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapearChamada(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar chamadas: " + e.getMessage());
        }
        return lista;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRESENÇAS
    // ─────────────────────────────────────────────────────────────────────────

    public List<ChamadaPresenca> listarPresencas(String chamadaId) {
        String sql = """
            SELECT cp.id, cp.chamada_id, cp.aluno_id, p.nome as aluno_nome, cp.presente
            FROM chamada_presencas cp
            JOIN perfis p ON p.id = cp.aluno_id
            WHERE cp.chamada_id = ?::uuid
            ORDER BY p.nome
            """;
        List<ChamadaPresenca> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chamadaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ChamadaPresenca(
                            rs.getString("id"), rs.getString("chamada_id"),
                            rs.getString("aluno_id"), rs.getString("aluno_nome"),
                            rs.getBoolean("presente")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar presenças: " + e.getMessage());
        }
        return lista;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESUMO POR TURMA — preview na tela de chamada
    // ─────────────────────────────────────────────────────────────────────────

    public List<ResumoTurma> resumoPorTurma(String professorId) {
        String sql = """
            SELECT t.id AS turma_id, t.nome AS turma_nome, e.nome AS escola_nome,
                   COUNT(DISTINCT c.id) AS total_chamadas,
                   MAX(c.data_aula) AS ultima_chamada,
                   ROUND(
                     100.0 * COUNT(cp.id) FILTER (WHERE cp.presente)::numeric
                     / NULLIF(COUNT(cp.id), 0), 1
                   ) AS media_presenca
            FROM turmas t
            JOIN escolas e ON e.id = t.escola_id
            LEFT JOIN chamadas c ON c.turma_id = t.id AND c.professor_id = ?::uuid
            LEFT JOIN chamada_presencas cp ON cp.chamada_id = c.id
            WHERE t.professor_id = ?::uuid
            GROUP BY t.id, t.nome, e.nome
            ORDER BY t.nome
            """;
        List<ResumoTurma> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, professorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date ultData = rs.getDate("ultima_chamada");
                    lista.add(new ResumoTurma(
                            rs.getString("turma_id"),
                            rs.getString("turma_nome"),
                            rs.getString("escola_nome"),
                            rs.getInt("total_chamadas"),
                            rs.getDouble("media_presenca"),
                            ultData != null ? ultData.toLocalDate() : null
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar resumo de turmas: " + e.getMessage());
        }
        return lista;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRO DE HORAS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Para o professor: filtra pelo próprio ID.
     */
    public List<RegistroHoras> listarRegistroHoras(String professorId, Integer mes, Integer ano) {
        return listarRegistroHorasInterno(professorId, mes, ano);
    }

    /**
     * Para o admin: professorId pode ser null (todos os professores).
     */
    public List<RegistroHoras> listarRegistroHorasAdmin(String professorId, Integer mes, Integer ano) {
        return listarRegistroHorasInterno(professorId, mes, ano);
    }

    private List<RegistroHoras> listarRegistroHorasInterno(String professorId,
                                                           Integer mes, Integer ano) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT chamada_id, professor_id, professor_nome,
                   turma_id, turma_nome, escola_nome,
                   data_aula, horario_inicio, horario_fim, tipo_aula,
                   horas_ministradas, total_alunos, total_presentes, total_ausentes
            FROM v_registro_horas
            WHERE 1=1
            """);
        if (professorId != null) {
            sql.append(" AND professor_id = ?::uuid");
            params.add(professorId);
        }
        if (mes != null) {
            sql.append(" AND mes = ?");
            params.add(mes);
        }
        if (ano != null) {
            sql.append(" AND ano = ?");
            params.add(ano);
        }
        sql.append(" ORDER BY data_aula DESC, professor_nome, horario_inicio");

        List<RegistroHoras> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String) stmt.setString(i + 1, (String) p);
                else if (p instanceof Integer) stmt.setInt(i + 1, (Integer) p);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new RegistroHoras(
                            rs.getString("chamada_id"),
                            rs.getString("professor_id"),
                            rs.getString("professor_nome"),
                            rs.getString("turma_id"),
                            rs.getString("turma_nome"),
                            rs.getString("escola_nome"),
                            rs.getDate("data_aula").toLocalDate(),
                            rs.getString("horario_inicio"),
                            rs.getString("horario_fim"),
                            rs.getString("tipo_aula"),
                            rs.getDouble("horas_ministradas"),
                            rs.getInt("total_alunos"),
                            rs.getInt("total_presentes"),
                            rs.getInt("total_ausentes")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar registro de horas: " + e.getMessage());
        }
        return lista;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Chamada mapearChamada(ResultSet rs) throws SQLException {
        return new Chamada(
                rs.getString("id"),
                rs.getString("professor_id"),
                rs.getString("turma_id"),
                rs.getString("turma_nome"),
                rs.getString("cronograma_id"),
                rs.getDate("data_aula").toLocalDate(),
                rs.getString("horario_inicio"),
                rs.getString("horario_fim"),
                rs.getInt("total_alunos"),
                rs.getInt("total_presentes")
        );
    }
}