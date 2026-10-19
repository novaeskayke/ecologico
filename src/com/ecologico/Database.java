package com.ecologico;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cuida do banco de dados.
 *
 * Usamos SQLite: o banco inteiro é UM arquivo ("ecologico.db"), criado
 * sozinho na primeira vez que o programa roda. Não precisa instalar nada.
 */
public class Database {

    private static final String URL = "jdbc:sqlite:ecologico.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Descobre o id do último registro inserido nesta conexão. */
    public static int ultimoId(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    /** Cria as tabelas (se ainda não existirem) e, se o banco estiver vazio, coloca dados de exemplo. */
    public static void iniciar() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {

            // 1) Catálogo de alimentos. "nome_busca" guarda o nome sem acento/maiúscula, só para pesquisar.
            st.execute("CREATE TABLE IF NOT EXISTS produtos ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL UNIQUE, "
                    + "nome_busca TEXT NOT NULL, "
                    + "categoria TEXT)");

            // 2) Quem planta e vende
            st.execute("CREATE TABLE IF NOT EXISTS produtores ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL, "
                    + "email TEXT, "
                    + "telefone TEXT, "
                    + "cidade TEXT, "
                    + "bairro TEXT, "
                    + "latitude REAL, "
                    + "longitude REAL)");

            // 3) Quem compra
            st.execute("CREATE TABLE IF NOT EXISTS consumidores ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL, "
                    + "email TEXT, "
                    + "cidade TEXT, "
                    + "bairro TEXT)");

            // 4) O que cada produtor tem para vender agora (liga produtor <-> produto)
            st.execute("CREATE TABLE IF NOT EXISTS disponibilidade ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "produtor_id INTEGER NOT NULL, "
                    + "produto_id INTEGER NOT NULL, "
                    + "quantidade_disponivel REAL NOT NULL, "
                    + "unidade TEXT DEFAULT 'kg', "
                    + "preco REAL, "
                    + "FOREIGN KEY (produtor_id) REFERENCES produtores(id), "
                    + "FOREIGN KEY (produto_id) REFERENCES produtos(id))");

            // 5) Quanto foi vendido de cada produto em cada mês (base da estimativa de plantio)
            st.execute("CREATE TABLE IF NOT EXISTS historico_consumo ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "produto_id INTEGER NOT NULL, "
                    + "periodo TEXT NOT NULL, "
                    + "quantidade_vendida REAL NOT NULL, "
                    + "FOREIGN KEY (produto_id) REFERENCES produtos(id))");

            int totalProdutos = 0;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM produtos")) {
                if (rs.next()) totalProdutos = rs.getInt(1);
            }
            if (totalProdutos == 0) {
                inserirExemplos(con, st);
            }
        }
    }

    /** Dados de exemplo, para o protótipo já nascer com algo para testar. */
    private static void inserirExemplos(Connection con, Statement st) throws SQLException {
        String[][] produtos = {
            {"Maçã", "Fruta"}, {"Alface", "Verdura"}, {"Tomate", "Legume"},
            {"Banana", "Fruta"}, {"Cenoura", "Legume"}
        };
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO produtos (nome, nome_busca, categoria) VALUES (?,?,?)")) {
            for (String[] p : produtos) {
                ps.setString(1, p[0]);
                ps.setString(2, Texto.normalizar(p[0]));
                ps.setString(3, p[1]);
                ps.executeUpdate();
            }
        }

        // ids dos produtos: 1=Maçã, 2=Alface, 3=Tomate, 4=Banana, 5=Cenoura
        st.executeUpdate("INSERT INTO historico_consumo (produto_id, periodo, quantidade_vendida) VALUES "
                + "(1,'2026-01',320), (1,'2026-02',290), (1,'2026-03',410), "
                + "(2,'2026-01',150), (2,'2026-02',170), (2,'2026-03',140), "
                + "(3,'2026-01',260), (3,'2026-02',300), (3,'2026-03',280)");

        st.executeUpdate("INSERT INTO produtores (nome, email, telefone, cidade, bairro, latitude, longitude) VALUES "
                + "('Sítio Boa Vista', 'contato@sitioboavista.com', '(15) 99999-0001', 'Sorocaba', 'Éden', -23.4733, -47.4537), "
                + "('Fazenda Verde Vida', 'contato@verdevida.com', '(15) 99999-0002', 'Sorocaba', 'Campolim', -23.4900, -47.4650), "
                + "('Horta Comunitária Votorantim', 'contato@hortavotorantim.com', '(15) 99999-0003', 'Votorantim', 'Centro', -23.5467, -47.4387)");

        st.executeUpdate("INSERT INTO disponibilidade (produtor_id, produto_id, quantidade_disponivel, unidade, preco) VALUES "
                + "(1, 1, 120, 'kg', 4.50), "
                + "(2, 1, 80, 'kg', 4.20), "
                + "(2, 3, 200, 'kg', 3.80), "
                + "(3, 2, 60, 'kg', 3.00)");
    }

    /** Monta uma tabela HTML com TODO o conteúdo de uma tabela do banco (usado na página "Ver banco de dados"). */
    public static String tabelaComoHtml(String nomeDaTabela) throws SQLException {
        StringBuilder html = new StringBuilder();
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + nomeDaTabela)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colunas = meta.getColumnCount();

            html.append("<table class=\"tabela\"><thead><tr>");
            for (int i = 1; i <= colunas; i++) {
                html.append("<th>").append(meta.getColumnName(i)).append("</th>");
            }
            html.append("</tr></thead><tbody>");
            while (rs.next()) {
                html.append("<tr>");
                for (int i = 1; i <= colunas; i++) {
                    html.append("<td>").append(Texto.escapar(rs.getString(i))).append("</td>");
                }
                html.append("</tr>");
            }
            html.append("</tbody></table>");
        }
        return html.toString();
    }
}
