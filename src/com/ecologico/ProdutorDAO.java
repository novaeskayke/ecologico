package com.ecologico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ProdutorDAO {

    /** Salva um produtor novo e devolve o id que o banco deu a ele. */
    public int cadastrar(Produtor p) throws SQLException {
        String sql = "INSERT INTO produtores (nome, email, telefone, cidade, bairro, latitude, longitude) "
                   + "VALUES (?,?,?,?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.nome);
            ps.setString(2, p.email);
            ps.setString(3, p.telefone);
            ps.setString(4, p.cidade);
            ps.setString(5, p.bairro);
            if (p.latitude != null) ps.setDouble(6, p.latitude); else ps.setNull(6, Types.REAL);
            if (p.longitude != null) ps.setDouble(7, p.longitude); else ps.setNull(7, Types.REAL);
            ps.executeUpdate();
            return Database.ultimoId(con);
        }
    }

    public List<Produtor> listarTodos() throws SQLException {
        List<Produtor> lista = new ArrayList<>();
        try (Connection con = Database.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nome, cidade, bairro FROM produtores ORDER BY nome")) {
            while (rs.next()) {
                Produtor p = new Produtor();
                p.id = rs.getInt("id");
                p.nome = rs.getString("nome");
                p.cidade = rs.getString("cidade");
                p.bairro = rs.getString("bairro");
                lista.add(p);
            }
        }
        return lista;
    }

    public Produtor buscarPorId(int id) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM produtores WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Produtor p = new Produtor();
                    p.id = rs.getInt("id");
                    p.nome = rs.getString("nome");
                    p.email = rs.getString("email");
                    p.telefone = rs.getString("telefone");
                    p.cidade = rs.getString("cidade");
                    p.bairro = rs.getString("bairro");
                    return p;
                }
            }
        }
        return null;
    }

    /** O produtor anuncia que tem um produto disponível. */
    public void adicionarDisponibilidade(int produtorId, int produtoId, double quantidade,
                                         String unidade, Double preco) throws SQLException {
        String sql = "INSERT INTO disponibilidade (produtor_id, produto_id, quantidade_disponivel, unidade, preco) "
                   + "VALUES (?,?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, produtorId);
            ps.setInt(2, produtoId);
            ps.setDouble(3, quantidade);
            ps.setString(4, unidade);
            if (preco != null) ps.setDouble(5, preco); else ps.setNull(5, Types.REAL);
            ps.executeUpdate();
        }
    }

    /** Lista (já em texto pronto) os produtos que este produtor anunciou. */
    public List<String> listarOfertasDoProdutor(int produtorId) throws SQLException {
        String sql = "SELECT p.nome AS produto, d.quantidade_disponivel, d.unidade, d.preco "
                   + "FROM disponibilidade d JOIN produtos p ON p.id = d.produto_id "
                   + "WHERE d.produtor_id = ? ORDER BY d.id DESC";
        List<String> lista = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, produtorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double preco = rs.getDouble("preco");
                    boolean temPreco = !rs.wasNull();
                    String linha = String.format("%s: %.1f %s", rs.getString("produto"),
                            rs.getDouble("quantidade_disponivel"), rs.getString("unidade"));
                    if (temPreco) linha += String.format(" (R$ %.2f)", preco);
                    lista.add(linha);
                }
            }
        }
        return lista;
    }

    /**
     * A busca do consumidor: quem tem este produto disponível?
     * Se o consumidor informou a localização, calculamos a distância e ordenamos do mais perto ao mais longe.
     */
    public List<ResultadoBusca> buscarPorProduto(String nomeProduto, Double latConsumidor, Double lonConsumidor)
            throws SQLException {
        String sql = "SELECT pr.nome AS produtor_nome, pr.cidade, pr.bairro, pr.telefone, "
                   + "pr.latitude, pr.longitude, d.quantidade_disponivel, d.unidade, d.preco "
                   + "FROM disponibilidade d "
                   + "JOIN produtores pr ON pr.id = d.produtor_id "
                   + "JOIN produtos p ON p.id = d.produto_id "
                   + "WHERE p.nome_busca LIKE ? AND d.quantidade_disponivel > 0";
        List<ResultadoBusca> resultados = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // "maçã", "MAÇÃ" ou "maca" viram "maca"; o % deixa achar também "maca verde", por exemplo
            ps.setString(1, "%" + Texto.normalizar(nomeProduto) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ResultadoBusca r = new ResultadoBusca();
                    r.produtorNome = rs.getString("produtor_nome");
                    r.cidade = rs.getString("cidade");
                    r.bairro = rs.getString("bairro");
                    r.telefone = rs.getString("telefone");
                    r.quantidadeDisponivel = rs.getDouble("quantidade_disponivel");
                    r.unidade = rs.getString("unidade");

                    double preco = rs.getDouble("preco");
                    r.preco = rs.wasNull() ? null : preco;

                    double latP = rs.getDouble("latitude");
                    boolean temLat = !rs.wasNull();
                    double lonP = rs.getDouble("longitude");
                    boolean temLon = !rs.wasNull();

                    if (latConsumidor != null && lonConsumidor != null && temLat && temLon) {
                        r.distanciaKm = Distancia.haversineKm(latConsumidor, lonConsumidor, latP, lonP);
                    }
                    resultados.add(r);
                }
            }
        }
        // mais perto primeiro; quem não tem distância vai para o fim, em ordem alfabética
        resultados.sort((a, b) -> {
            if (a.distanciaKm == null && b.distanciaKm == null) return a.produtorNome.compareTo(b.produtorNome);
            if (a.distanciaKm == null) return 1;
            if (b.distanciaKm == null) return -1;
            return Double.compare(a.distanciaKm, b.distanciaKm);
        });
        return resultados;
    }
}
