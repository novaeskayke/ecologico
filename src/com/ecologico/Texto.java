package com.ecologico;

import java.text.Normalizer;

/** Pequenas funções para mexer com texto. */
public class Texto {

    /**
     * Deixa o texto "pronto para busca": tudo minúsculo e sem acentos.
     * Assim "Maçã", "maçã", "MACA" e "maca" viram todos "maca".
     */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().trim();
    }

    /** Protege o HTML: troca caracteres especiais para que o texto digitado não quebre a página. */
    public static String escapar(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }
}
