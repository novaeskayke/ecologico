package com.ecologico;

/** Um alimento do catálogo (ex: Maçã). */
public class Produto {
    public int id;
    public String nome;
    public String categoria;

    public Produto(int id, String nome, String categoria) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
    }
}
