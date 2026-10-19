package com.ecologico;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Lê os dados que o navegador envia (na URL ou em formulários). */
public class Params {

    /** Transforma "a=1&b=2" em um mapa {a:1, b:2}, já decodificando acentos. */
    public static Map<String, String> deQuery(String query) {
        Map<String, String> mapa = new HashMap<>();
        if (query == null || query.isEmpty()) return mapa;
        for (String par : query.split("&")) {
            int pos = par.indexOf('=');
            if (pos < 0) continue;
            String chave = URLDecoder.decode(par.substring(0, pos), StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(par.substring(pos + 1), StandardCharsets.UTF_8);
            mapa.put(chave, valor);
        }
        return mapa;
    }

    /** Lê o corpo de um formulário enviado por POST. */
    public static Map<String, String> deCorpo(HttpExchange ex) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ex.getRequestBody().transferTo(buffer);
        return deQuery(buffer.toString(StandardCharsets.UTF_8));
    }

    public static Double paraDouble(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Double.parseDouble(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
