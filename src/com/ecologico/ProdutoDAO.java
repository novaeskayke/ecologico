package com.ecologico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** DAO = classe que "conversa" com o banco. Esta cuida da tabela de produtos. */
public class ProdutoDAO {

    public List<Produto> listarTodos() throws SQLException {
        List<Produto> lista = new ArrayList<>();
        try (Connection con = Database.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nome, categoria FROM produtos ORDER BY nome")) {
            while (rs.next()) {
                lista.add(new Produto(rs.getInt("id"), rs.getString("nome"), rs.getString("categoria")));
            }
        }
        return lista;
    }

    /** Nomes dos produtos que alguém está vendendo agora (usado nos botões de atalho da busca). */
    public List<String> nomesComOferta() throws SQLException {
        List<String> nomes = new ArrayList<>();
        String sql = "SELECT DISTINCT p.nome FROM disponibilidade d "
                   + "JOIN produtos p ON p.id = d.produto_id "
                   + "WHERE d.quantidade_disponivel > 0 ORDER BY p.nome";
        try (Connection con = Database.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) nomes.add(rs.getString("nome"));
        }
        return nomes;
    }

    public String nomePorId(int id) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT nome FROM produtos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("nome");
            }
        }
        return null;
    }
}
