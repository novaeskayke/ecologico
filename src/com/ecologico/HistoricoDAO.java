package com.ecologico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Histórico de quanto foi vendido de cada produto por mês. */
public class HistoricoDAO {

    /** Uma linha do histórico: "em tal mês, foram vendidas tantas unidades". */
    public static class RegistroHistorico {
        public String periodo;
        public double quantidade;
    }

    public List<RegistroHistorico> listarPorProduto(int produtoId) throws SQLException {
        String sql = "SELECT periodo, quantidade_vendida FROM historico_consumo "
                   + "WHERE produto_id = ? ORDER BY periodo";
        List<RegistroHistorico> lista = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RegistroHistorico r = new RegistroHistorico();
                    r.periodo = rs.getString("periodo");
                    r.quantidade = rs.getDouble("quantidade_vendida");
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    public void registrar(int produtoId, String periodo, double quantidade) throws SQLException {
        String sql = "INSERT INTO historico_consumo (produto_id, periodo, quantidade_vendida) VALUES (?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, produtoId);
            ps.setString(2, periodo);
            ps.setDouble(3, quantidade);
            ps.executeUpdate();
        }
    }
}
