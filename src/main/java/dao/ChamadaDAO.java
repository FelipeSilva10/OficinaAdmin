package dao;

import core.Chamada;
import core.ChamadaPresenca;
import core.RegistroHoras;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChamadaDAO {

    // ── CHAMADAS ───────────────────────────────────────────────────────────────

    /**
     * Abre uma nova chamada e grava a presença de cada aluno.
     * Retorna o ID da chamada criada, ou null em caso de erro.
     * Usa transação para garantir atomicidade entre as duas tabelas.
     */
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
                // 1. Insere a chamada
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
                        if (!rs.next()) throw new SQLException("Chamada não foi inserida.");
                        chamadaId = rs.getString("id");
                    }
                }

                // 2. Insere as presenças
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
            System.err.println("Erro de conexão ao abrir chamada: " + e.getMessage());
            return null;
        }
    }

    /** Verifica se já existe chamada para professor+turma+data. */
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

    /** Lista todas as chamadas do professor, ordenadas por data decrescente. */
    public List<Chamada> listarPorProfessor(String professorId) {
        String sql = """
            SELECT c.id, c.professor_id, c.turma_id, t.nome as turma_nome,
                   c.cronograma_id, c.data_aula,
                   TO_CHAR(c.horario_inicio, 'HH24:MI') as horario_inicio,
                   TO_CHAR(c.horario_fim,    'HH24:MI') as horario_fim,
                   COUNT(cp.id)                                         as total_alunos,
                   COUNT(cp.id) FILTER (WHERE cp.presente = true)       as total_presentes
            FROM chamadas c
            JOIN turmas t ON t.id = c.turma_id
            LEFT JOIN chamada_presencas cp ON cp.chamada_id = c.id
            WHERE c.professor_id = ?::uuid
            GROUP BY c.id, t.nome
            ORDER BY c.data_aula DESC, c.horario_inicio
            """;
        return buscarChamadas(sql, professorId, null, null);
    }

    // ── PRESENÇAS ──────────────────────────────────────────────────────────────

    /** Carrega a lista de presença de uma chamada específica. */
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
                            rs.getString("id"),
                            rs.getString("chamada_id"),
                            rs.getString("aluno_id"),
                            rs.getString("aluno_nome"),
                            rs.getBoolean("presente")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar presenças: " + e.getMessage());
        }
        return lista;
    }

    // ── REGISTRO DE HORAS ─────────────────────────────────────────────────────

    /**
     * Lista o registro de horas do professor.
     * @param mes  1–12 ou null para todos os meses
     * @param ano  ex: 2026 ou null para todos os anos
     */
    public List<RegistroHoras> listarRegistroHoras(String professorId,
                                                   Integer mes, Integer ano) {
        StringBuilder sql = new StringBuilder("""
            SELECT chamada_id, turma_id, turma_nome, escola_nome,
                   data_aula, horario_inicio, horario_fim,
                   horas_ministradas, total_alunos, total_presentes, total_ausentes
            FROM v_registro_horas
            WHERE professor_id = ?::uuid
            """);

        if (mes != null) sql.append(" AND EXTRACT(MONTH FROM data_aula) = ").append(mes);
        if (ano != null) sql.append(" AND EXTRACT(YEAR  FROM data_aula) = ").append(ano);
        sql.append(" ORDER BY data_aula DESC, horario_inicio");

        List<RegistroHoras> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, professorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new RegistroHoras(
                            rs.getString("chamada_id"),
                            rs.getString("turma_id"),
                            rs.getString("turma_nome"),
                            rs.getString("escola_nome"),
                            rs.getDate("data_aula").toLocalDate(),
                            rs.getString("horario_inicio"),
                            rs.getString("horario_fim"),
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

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<Chamada> buscarChamadas(String sql, String professorId,
                                         Integer mes, Integer ano) {
        List<Chamada> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Chamada(
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
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar chamadas: " + e.getMessage());
        }
        return lista;
    }
}