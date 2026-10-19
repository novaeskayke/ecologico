package com.ecologico;

/** Uma linha do resultado da busca: um produtor que tem o produto procurado. */
public class ResultadoBusca {
    public String produtorNome;
    public String cidade;
    public String bairro;
    public String telefone;
    public double quantidadeDisponivel;
    public String unidade;
    public Double preco;        // pode ser null
    public Double distanciaKm;  // null quando não dá para calcular
}
