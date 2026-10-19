package com.ecologico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ConsumidorDAO {

    public int cadastrar(Consumidor c) throws SQLException {
        String sql = "INSERT INTO consumidores (nome, email, cidade, bairro) VALUES (?,?,?,?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.nome);
            ps.setString(2, c.email);
            ps.setString(3, c.cidade);
            ps.setString(4, c.bairro);
            ps.executeUpdate();
            return Database.ultimoId(con);
        }
    }
}
