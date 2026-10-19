package com.ecologico;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * COMO ESTE PROGRAMA FUNCIONA (resumo):
 *
 *  1. O main() cria o banco SQLite e liga um servidor web na porta 8080.
 *  2. Cada "createContext" liga um endereço (ex: /produtor/painel) a um método Java.
 *  3. Cada método lê o que o navegador mandou, fala com o banco (via DAOs)
 *     e devolve uma página HTML pronta.
 *
 *  Fluxo de uma página:  navegador -> endereço -> método abaixo -> DAO -> banco -> HTML -> navegador
 */
public class Main {

    // Os "DAOs" são as classes que conversam com o banco de dados
    static final ProdutoDAO produtoDAO = new ProdutoDAO();
    static final ProdutorDAO produtorDAO = new ProdutorDAO();
    static final ConsumidorDAO consumidorDAO = new ConsumidorDAO();
    static final HistoricoDAO historicoDAO = new HistoricoDAO();

    // Margem de segurança da estimativa: planta 15% a mais que a média, para cobrir perdas
    static final double MARGEM_SEGURANCA = 0.15;

    public static void main(String[] args) throws IOException {
        try {
            Database.iniciar();
            System.out.println("Banco SQLite pronto (arquivo ecologico.db).");
        } catch (Exception e) {
            System.out.println("Erro ao preparar o banco: " + e.getMessage());
            System.out.println("Confira se o arquivo .jar do SQLite esta na pasta lib/");
            return;
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress(8080), 0);

        // endereço  ->  método que responde
        servidor.createContext("/static/", Main::arquivoEstatico);
        servidor.createContext("/", Main::home);
        servidor.createContext("/produtor", Main::produtorInicio);
        servidor.createContext("/produtor/cadastro", Main::produtorCadastro);
        servidor.createContext("/produtor/painel", Main::produtorPainel);
        servidor.createContext("/produtor/disponibilidade", Main::produtorDisponibilidade);
        servidor.createContext("/produtor/historico", Main::produtorHistorico);
        servidor.createContext("/produtor/estimativa", Main::produtorEstimativa);
        servidor.createContext("/consumidor/cadastro", Main::consumidorCadastro);
        servidor.createContext("/consumidor/busca", Main::consumidorBusca);
        servidor.createContext("/banco", Main::banco);

        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Pronto! Abra no navegador: http://localhost:8080");
    }

    // ============================================================
    //  FERRAMENTAS PEQUENAS (usadas por todas as páginas)
    // ============================================================

    static void responder(HttpExchange ex, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Manda o navegador para outra página. */
    static void redirecionar(HttpExchange ex, String local) throws IOException {
        ex.getResponseHeaders().set("Location", local);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    static void erro(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        responder(ex, 500, Html.pagina("Erro",
                "<h1>Ops, algo deu errado</h1><p>" + esc(msg) + "</p>"
                + "<a class=\"botao botao-linha\" href=\"/\">Voltar ao início</a>"));
    }

    static void naoEncontrado(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(404, -1);
        ex.close();
    }

    static void arquivoEstatico(HttpExchange ex) throws IOException {
        File arquivo = new File("web" + ex.getRequestURI().getPath());
        if (!arquivo.exists() || arquivo.isDirectory()) {
            naoEncontrado(ex);
            return;
        }
        byte[] bytes = Files.readAllBytes(arquivo.toPath());
        ex.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Protege o HTML de textos digitados pelo usuário. */
    static String esc(String texto) {
        return Texto.escapar(texto);
    }

    /** Escreve números sem casas inúteis: 340.0 vira "340" e 12.5 continua "12,5". */
    static String num(double valor) {
        double arredondado = Math.round(valor * 10) / 10.0;
        if (arredondado == Math.rint(arredondado)) return String.valueOf((long) arredondado);
        return String.format("%.1f", arredondado);
    }

    static String montarOpcoesDeProdutos(List<Produto> produtos) {
        StringBuilder opcoes = new StringBuilder();
        for (Produto p : produtos) {
            opcoes.append("<option value=\"").append(p.id).append("\">").append(esc(p.nome)).append("</option>");
        }
        return opcoes.toString();
    }

    static String cartaoPasso(String numero, String titulo, String texto) {
        return "<div class=\"cartao\"><span class=\"passo-num\">" + numero + "</span>"
             + "<h3>" + titulo + "</h3><p>" + texto + "</p></div>";
    }

    // ============================================================
    //  PÁGINA INICIAL
    // ============================================================

    static void home(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) {
            naoEncontrado(ex);
            return;
        }
        String conteudo =
            "<section class=\"hero-simples\">" +
            "<h1>Do campo à mesa, sem desperdício</h1>" +
            "<p>O Ecológico conecta quem planta com quem compra. Veja como funciona, em 3 passos:</p>" +
            "</section>" +
            "<div class=\"grade-painel\">" +
            cartaoPasso("1", "O produtor anuncia", "Quem planta cadastra o que tem disponível para vender.") +
            cartaoPasso("2", "O sistema estima", "Com o histórico de consumo, ele sugere quanto vale a pena plantar.") +
            cartaoPasso("3", "O consumidor encontra", "Quem compra busca um alimento e vê quem vende por perto.") +
            "</div>" +
            "<p class=\"dica centro\">Escolha seu perfil para começar:</p>" +
            "<div class=\"acoes\">" +
            "<a class=\"botao\" href=\"/produtor\">Sou produtor</a>" +
            "<a class=\"botao botao-linha\" href=\"/consumidor/busca\">Sou consumidor</a>" +
            "</div>";
        responder(ex, 200, Html.pagina("Inicio", conteudo));
    }

    // ============================================================
    //  ÁREA DO PRODUTOR
    // ============================================================

    /** Tela "Quem é você?": escolher um produtor existente ou criar um novo. */
    static void produtorInicio(HttpExchange ex) throws IOException {
        String caminho = ex.getRequestURI().getPath();
        if (!caminho.equals("/produtor") && !caminho.equals("/produtor/")) {
            naoEncontrado(ex);
            return;
        }
        try {
            StringBuilder html = new StringBuilder();
            html.append("<h1>Área do produtor</h1>");
            html.append("<p>Escolha o seu perfil para entrar, ou crie um cadastro novo.</p>");
            html.append("<div class=\"grade-painel\">");
            for (Produtor p : produtorDAO.listarTodos()) {
                html.append("<div class=\"cartao\"><h3>").append(esc(p.nome)).append("</h3>");
                html.append("<p class=\"dica\">").append(esc(p.cidade));
                if (p.bairro != null && !p.bairro.isBlank()) html.append(" · ").append(esc(p.bairro));
                html.append("</p><a class=\"botao\" href=\"/produtor/painel?id=").append(p.id).append("\">Entrar</a></div>");
            }
            html.append("</div>");
            html.append("<p><a class=\"botao botao-linha\" href=\"/produtor/cadastro\">+ Cadastrar novo produtor</a></p>");
            responder(ex, 200, Html.pagina("Area do produtor", html.toString()));
        } catch (Exception e) {
            erro(ex, e);
        }
    }

    static void produtorCadastro(HttpExchange ex) throws IOException {
        try {
            // GET = o navegador está pedindo para VER o formulário
            if (ex.getRequestMethod().equals("GET")) {
                String conteudo =
                    "<h1>Cadastro de produtor</h1>" +
                    "<p>Preencha seus dados. Só nome e cidade são obrigatórios.</p>" +
                    "<form method=\"POST\" action=\"/produtor/cadastro\" class=\"formulario\">" +
                    "<label>Nome <input name=\"nome\" required></label>" +
                    "<label>E-mail <input name=\"email\" type=\"email\"></label>" +
                    "<label>Telefone <input name=\"telefone\"></label>" +
                    "<label>Cidade <input name=\"cidade\" required></label>" +
                    "<label>Bairro <input name=\"bairro\"></label>" +
                    "<label>Latitude (opcional) <input name=\"latitude\" placeholder=\"ex: -23.527\"></label>" +
                    "<label>Longitude (opcional) <input name=\"longitude\" placeholder=\"ex: -47.478\"></label>" +
                    "<button type=\"submit\" class=\"botao\">Cadastrar</button>" +
                    "</form>" +
                    "<p class=\"dica\">Dica: latitude e longitude servem para mostrar a distância até o consumidor. " +
                    "No Google Maps, clique com o botão direito no seu local e copie os dois números.</p>";
                responder(ex, 200, Html.pagina("Cadastro de produtor", conteudo));
                return;
            }

            // POST = o navegador está ENVIANDO o formulário preenchido
            Map<String, String> dados = Params.deCorpo(ex);
            Produtor p = new Produtor();
            p.nome = dados.get("nome");
            p.email = dados.get("email");
            p.telefone = dados.get("telefone");
            p.cidade = dados.get("cidade");
            p.bairro = dados.get("bairro");
            p.latitude = Params.paraDouble(dados.get("latitude"));
            p.longitude = Params.paraDouble(dados.get("longitude"));
            int id = produtorDAO.cadastrar(p);          // salva no banco
            redirecionar(ex, "/produtor/painel?id=" + id); // e leva para o painel do novo produtor
        } catch (Exception e) {
            erro(ex, e);
        }
    }

    /** Painel do produtor, organizado em 3 passos. */
    static void produtorPainel(HttpExchange ex) throws IOException {
        try {
            Map<String, String> query = Params.deQuery(ex.getRequestURI().getQuery());
            int id = Integer.parseInt(query.getOrDefault("id", "0"));
            Produtor p = produtorDAO.buscarPorId(id);
            if (p == null) {
                responder(ex, 404, Html.pagina("Nao encontrado",
                        "<p>Produtor não encontrado.</p><a class=\"botao\" href=\"/produtor\">Voltar</a>"));
                return;
            }

            String opcoes = montarOpcoesDeProdutos(produtoDAO.listarTodos());

            StringBuilder listaOfertas = new StringBuilder();
            List<String> ofertas = produtorDAO.listarOfertasDoProdutor(id);
            if (ofertas.isEmpty()) {
                listaOfertas.append("<li>Nada anunciado ainda.</li>");
            } else {
                for (String o : ofertas) listaOfertas.append("<li>").append(esc(o)).append("</li>");
            }

            StringBuilder html = new StringBuilder();
            html.append("<h1>Olá, ").append(esc(p.nome)).append("!</h1>");

            // mensagem de confirmação depois de uma ação
            String ok = query.get("ok");
            if ("anuncio".equals(ok)) {
                html.append("<div class=\"aviso\">Produto anunciado! Ele já aparece na busca dos consumidores.</div>");
            } else if ("historico".equals(ok)) {
                html.append("<div class=\"aviso\">Venda registrada! Ela já entra no cálculo da estimativa.</div>");
            }

            html.append("<p>Siga os 3 passos abaixo:</p>");
            html.append("<div class=\"grade-painel\">");

            // PASSO 1
            html.append("<section class=\"cartao\"><span class=\"passo-num\">1</span>");
            html.append("<h2>Anuncie o que você tem</h2>");
            html.append("<p class=\"dica\">Os consumidores vão encontrar você pela busca.</p>");
            html.append("<form method=\"POST\" action=\"/produtor/disponibilidade\" class=\"formulario\">");
            html.append("<input type=\"hidden\" name=\"produtorId\" value=\"").append(id).append("\">");
            html.append("<label>Produto <select name=\"produtoId\">").append(opcoes).append("</select></label>");
            html.append("<label>Quantidade disponível <input name=\"quantidade\" type=\"number\" step=\"any\" min=\"0\" required></label>");
            html.append("<label>Unidade <input name=\"unidade\" value=\"kg\"></label>");
            html.append("<label>Preço por unidade (opcional) <input name=\"preco\" type=\"number\" step=\"any\" min=\"0\"></label>");
            html.append("<button type=\"submit\" class=\"botao\">Anunciar</button></form>");
            html.append("<h3>Meus anúncios</h3><ul>").append(listaOfertas).append("</ul></section>");

            // PASSO 2
            html.append("<section class=\"cartao\"><span class=\"passo-num\">2</span>");
            html.append("<h2>Registre uma venda</h2>");
            html.append("<p class=\"dica\">Quanto mais histórico, melhor a estimativa.</p>");
            html.append("<form method=\"POST\" action=\"/produtor/historico\" class=\"formulario\">");
            html.append("<input type=\"hidden\" name=\"produtorId\" value=\"").append(id).append("\">");
            html.append("<label>Produto <select name=\"produtoId\">").append(opcoes).append("</select></label>");
            html.append("<label>Mês <input name=\"periodo\" type=\"month\" required></label>");
            html.append("<label>Quantidade vendida <input name=\"quantidade\" type=\"number\" step=\"any\" min=\"0\" required></label>");
            html.append("<button type=\"submit\" class=\"botao\">Registrar</button></form></section>");

            // PASSO 3
            html.append("<section class=\"cartao\"><span class=\"passo-num\">3</span>");
            html.append("<h2>Veja quanto plantar</h2>");
            html.append("<p class=\"dica\">O sistema usa o histórico de vendas para sugerir uma quantidade.</p>");
            html.append("<form method=\"GET\" action=\"/produtor/estimativa\" class=\"formulario\">");
            html.append("<input type=\"hidden\" name=\"produtorId\" value=\"").append(id).append("\">");
            html.append("<label>Produto <select name=\"produtoId\">").append(opcoes).append("</select></label>");
            html.append("<button type=\"submit\" class=\"botao\">Ver estimativa</button></form></section>");

            html.append("</div>");

            responder(ex, 200, Html.pagina("Painel do produtor", html.toString()));
        } catch (Exception e) {
            erro(ex, e);
        }
    }

    static void produtorDisponibilidade(HttpExchange ex) throws IOException {
        try {
            Map<String, String> dados = Params.deCorpo(ex);
            int produtorId = Integer.parseInt(dados.get("produtorId"));
            int produtoId = Integer.parseInt(dados.get("produtoId"));
            double quantidade = Double.parseDouble(dados.get("quantidade").replace(",", "."));
            String unidade = dados.getOrDefault("unidade", "kg");
            Double preco = Params.paraDouble(dados.get("preco"));
            produtorDAO.adicionarDisponibilidade(produtorId, produtoId, quantidade, unidade, preco);
            redirecionar(ex, "/produtor/painel?id=" + produtorId + "&ok=anuncio");
        } catch (Exception e) {
            erro(ex, e);
        }
    }

    static void produtorHistorico(HttpExchange ex) throws IOException {
        try {
            Map<String, String> dados = Params.deCorpo(ex);
            int produtorId = Integer.parseInt(dados.get("produtorId"));
            int produtoId = Integer.parseInt(dados.get("produtoId"));
            String periodo = dados.get("periodo");
            double quantidade = Double.parseDouble(dados.get("quantidade").replace(",", "."));
            historicoDAO.registrar(produtoId, periodo, quantidade);
            redirecionar(ex, "/produtor/painel?id=" + produtorId + "&ok=historico");
        } catch (Exception e) {
            erro(ex, e);
        }
    }

    /**
     * A ESTIMATIVA DE PLANTIO, em 3 contas simples:
     *   1) soma tudo que foi vendido nos meses do histórico
     *   2) divide pelo número de meses  -> média
     *   3) soma uma margem de segurança (15%) para cobrir perdas
     */
    static void produtorEstimativa(HttpExchange ex) throws IOException {
        try {
            Map<String, String> query = Params.deQuery(ex.getRequestURI().getQuery());
            int produtoId = Integer.parseInt(query.get("produtoId"));
            int produtorId = Integer.parseInt(query.getOrDefault("produtorId", "0"));

            String nomeProduto = produtoDAO.nomePorId(produtoId);
            List<HistoricoDAO.RegistroHistorico> historico = historicoDAO.listarPorProduto(produtoId);
            String voltar = "<p><a class=\"botao botao-linha\" href=\"/produtor/painel?id=" + produtorId
                          + "\">Voltar ao painel</a></p>";

            StringBuilder html = new StringBuilder();
            html.append("<h1>Quanto plantar de ").append(esc(nomeProduto)).append("?</h1>");

            if (historico.isEmpty()) {
                html.append("<p>Ainda não há vendas registradas deste produto. ");
                html.append("Registre algumas vendas no painel (passo 2) e volte aqui.</p>").append(voltar);
                responder(ex, 200, Html.pagina("Estimativa de plantio", html.toString()));
                return;
            }

            // --- as contas ---
            double soma = 0;
            double maximo = 0;
            StringBuilder parcelas = new StringBuilder();
            StringBuilder barras = new StringBuilder();
            for (HistoricoDAO.RegistroHistorico r : historico) {
                soma += r.quantidade;
                if (r.quantidade > maximo) maximo = r.quantidade;
                if (parcelas.length() > 0) parcelas.append(" + ");
                parcelas.append(num(r.quantidade));
            }
            double media = soma / historico.size();
            double estimativa = media * (1 + MARGEM_SEGURANCA);

            // --- gráfico de barras simples ---
            for (HistoricoDAO.RegistroHistorico r : historico) {
                int largura = maximo > 0 ? (int) Math.round(r.quantidade / maximo * 100) : 0;
                barras.append("<div class=\"barra-linha\"><span class=\"barra-rotulo\">").append(esc(r.periodo))
                      .append("</span><div class=\"barra\"><div class=\"barra-fill\" style=\"width:")
                      .append(largura).append("%\"></div></div><span class=\"barra-valor\">")
                      .append(num(r.quantidade)).append("</span></div>");
            }

            html.append("<h2>O que foi vendido em cada mês</h2>");
            html.append("<div class=\"cartao\">").append(barras).append("</div>");

            html.append("<h2>Como chegamos no resultado</h2>");
            html.append("<div class=\"passo-calculo\"><span class=\"passo-num\">1</span><p>Somamos as vendas de cada mês: ")
                .append("<strong>").append(parcelas).append(" = ").append(num(soma)).append("</strong></p></div>");
            html.append("<div class=\"passo-calculo\"><span class=\"passo-num\">2</span><p>Dividimos pelo número de meses (")
                .append(historico.size()).append(") para achar a média: <strong>")
                .append(num(soma)).append(" ÷ ").append(historico.size()).append(" = ").append(num(media))
                .append("</strong></p></div>");
            html.append("<div class=\"passo-calculo\"><span class=\"passo-num\">3</span><p>Somamos uma margem de segurança de ")
                .append(Math.round(MARGEM_SEGURANCA * 100)).append("% para cobrir perdas: <strong>")
                .append(num(media)).append(" × ").append(String.format("%.2f", 1 + MARGEM_SEGURANCA))
                .append(" = ").append(num(estimativa)).append("</strong></p></div>");

            html.append("<div class=\"destaque-estimativa\"><p>Sugestão para o próximo plantio</p>");
            html.append("<p class=\"numero-grande\">").append(num(estimativa)).append(" unidades</p></div>");
            html.append(voltar);

            responder(ex, 200, Html.pagina("Estimativa de plantio", html.toString()));
        } catch (Exception e) {
            erro(ex, e);
        }
    }

    // ============================================================
    //  ÁREA DO CONSUMIDOR
    // ============================================================

    static void consumidorCadastro(HttpExchange ex) throws IOException {
        try {
            if (ex.getRequestMethod().equals("GET")) {
                String conteudo =
                    "<h1>Cadastro de consumidor</h1>" +
                    "<p>O cadastro é opcional: você pode buscar alimentos sem se cadastrar.</p>" +
                    "<form method=\"POST\" action=\"/consumidor/cadastro\" class=\"formulario\">" +
                    "<label>Nome <input name=\"nome\" required></label>" +
                    "<label>E-mail <input name=\"email\" type=\"email\"></label>" +
                    "<label>Cidade <input name=\"cidade\"></label>" +
                    "<label>Bairro <input name=\"bairro\"></label>" +
                    "<button type=\"submit\" class=\"botao\">Cadastrar</button>" +
                    "</form>";
                responder(ex, 200, Html.pagina("Cadastro de consumidor", conteudo));
                return;
            }

            Map<String, String> dados = Params.deCorpo(ex);
            Consumidor c = new Consumidor();
            c.nome = dados.get("nome");
            c.email = dados.get("email");
            c.cidade = dados.get("cidade");
            c.bairro = dados.get("bairro");
            consumidorDAO.cadastrar(c);
            redirecionar(ex, "/consumidor/busca");
        } catch (Exception e) {
            erro(ex, e);
        }
    }

    /** Busca de alimentos: digite "Maçã" e veja quem vende, do mais perto ao mais longe. */
    static void consumidorBusca(HttpExchange ex) throws IOException {
        try {
            Map<String, String> query = Params.deQuery(ex.getRequestURI().getQuery());
            String produto = query.get("produto");
            Double lat = Params.paraDouble(query.get("lat"));
            Double lon = Params.paraDouble(query.get("lon"));
            boolean temBusca = produto != null && !produto.isBlank();

            // Botões de atalho com o que está disponível agora
            StringBuilder atalhos = new StringBuilder();
            for (String nome : produtoDAO.nomesComOferta()) {
                atalhos.append("<button type=\"button\" class=\"chip\" data-produto=\"")
                       .append(esc(nome)).append("\">").append(esc(nome)).append("</button>");
            }

            // Resultados da busca
            StringBuilder resultadosHtml = new StringBuilder();
            if (temBusca) {
                List<ResultadoBusca> resultados = produtorDAO.buscarPorProduto(produto, lat, lon);
                resultadosHtml.append("<h2>Resultados para \"").append(esc(produto)).append("\"</h2>");
                if (resultados.isEmpty()) {
                    resultadosHtml.append("<p>Ninguém tem esse produto disponível no momento. Tente outro nome ou um dos atalhos acima.</p>");
                } else {
                    resultadosHtml.append("<ul class=\"lista-resultados\">");
                    for (ResultadoBusca r : resultados) {
                        resultadosHtml.append("<li><strong>").append(esc(r.produtorNome)).append("</strong>");
                        if (r.distanciaKm != null) {
                            resultadosHtml.append("<span class=\"distancia\">a ")
                                          .append(String.format("%.1f", r.distanciaKm)).append(" km de você</span>");
                        }
                        resultadosHtml.append("<br>");
                        if (r.cidade != null) resultadosHtml.append(esc(r.cidade));
                        if (r.bairro != null && !r.bairro.isBlank()) resultadosHtml.append(" · ").append(esc(r.bairro));
                        resultadosHtml.append("<br>Disponível: ").append(String.format("%.1f", r.quantidadeDisponivel))
                                      .append(" ").append(esc(r.unidade));
                        if (r.preco != null) resultadosHtml.append(" · R$ ").append(String.format("%.2f", r.preco));
                        if (r.telefone != null && !r.telefone.isBlank()) {
                            resultadosHtml.append("<br>Contato: ").append(esc(r.telefone));
                        }
                        resultadosHtml.append("</li>");
                    }
                    resultadosHtml.append("</ul>");
                }
                // Explicação didática de como a busca funciona
                resultadosHtml.append("<details class=\"explicacao\"><summary>Como a busca funciona?</summary>");
                resultadosHtml.append("<p>O sistema pergunta ao banco de dados quem tem o produto em estoque. ");
                resultadosHtml.append("Depois, se você permitiu a localização, calcula a distância até cada produtor e ordena do mais perto ao mais longe.</p>");
                resultadosHtml.append("<pre class=\"codigo\">SELECT produtor, cidade, quantidade, preco\n");
                resultadosHtml.append("FROM disponibilidade\n");
                resultadosHtml.append("JOIN produtores ... JOIN produtos ...\n");
                resultadosHtml.append("WHERE nome_busca LIKE '%maca%'\n");
                resultadosHtml.append("  AND quantidade_disponivel &gt; 0</pre></details>");
            }

            StringBuilder html = new StringBuilder();
            html.append("<h1>O que você quer comprar hoje?</h1>");
            html.append("<form method=\"GET\" action=\"/consumidor/busca\" class=\"formulario formulario-busca\" id=\"form-busca\">");
            html.append("<input type=\"hidden\" name=\"lat\" id=\"campo-lat\" value=\"").append(lat != null ? lat : "").append("\">");
            html.append("<input type=\"hidden\" name=\"lon\" id=\"campo-lon\" value=\"").append(lon != null ? lon : "").append("\">");
            html.append("<div class=\"linha-busca\">");
            html.append("<input name=\"produto\" id=\"campo-produto\" placeholder=\"Digite um alimento. Ex: Maçã\" value=\"")
                .append(esc(produto)).append("\" required>");
            html.append("<button type=\"submit\" class=\"botao\">Buscar</button></div></form>");
            html.append("<p class=\"dica\">Ou escolha um que está disponível agora:</p>");
            html.append("<div class=\"chips\">").append(atalhos).append("</div>");
            html.append("<p class=\"dica\" id=\"status-localizacao\">Procurando sua localização para ordenar por distância...</p>");
            html.append("<div class=\"resultados\">").append(resultadosHtml).append("</div>");
            html.append("<p class=\"dica\"><a href=\"/consumidor/cadastro\">Quer criar um cadastro? (opcional)</a></p>");

            // Um pouco de JavaScript: pega a localização e faz os botões de atalho funcionarem
            html.append("<script>");
            html.append("var jaTinhaLocal = ").append(lat != null && lon != null ? "true" : "false").append(";");
            html.append("var form = document.getElementById('form-busca');");
            html.append("var status = document.getElementById('status-localizacao');");
            html.append("document.querySelectorAll('.chip').forEach(function(b) {");
            html.append("  b.addEventListener('click', function() {");
            html.append("    document.getElementById('campo-produto').value = b.getAttribute('data-produto');");
            html.append("    form.submit();");
            html.append("  });");
            html.append("});");
            html.append("if (navigator.geolocation) {");
            html.append("  navigator.geolocation.getCurrentPosition(function(pos) {");
            html.append("    document.getElementById('campo-lat').value = pos.coords.latitude;");
            html.append("    document.getElementById('campo-lon').value = pos.coords.longitude;");
            html.append("    status.textContent = 'Localização ativada: os resultados saem do mais perto ao mais longe.';");
            html.append("    if (!jaTinhaLocal && document.getElementById('campo-produto').value) { form.submit(); }");
            html.append("  }, function() {");
            html.append("    status.textContent = 'Sem acesso à localização: os resultados aparecem sem distância.';");
            html.append("  });");
            html.append("} else {");
            html.append("  status.textContent = '';");
            html.append("}");
            html.append("</script>");

            responder(ex, 200, Html.pagina("Buscar alimentos", html.toString()));
        } catch (Exception e) {
            erro(ex, e);
        }
    }

    // ============================================================
    //  VER O BANCO DE DADOS (para entender o que fica guardado)
    // ============================================================

    static void banco(HttpExchange ex) throws IOException {
        try {
            String[][] tabelas = {
                {"produtos", "Catálogo de alimentos que existem no sistema."},
                {"produtores", "Quem planta e vende."},
                {"consumidores", "Quem compra (cadastro opcional)."},
                {"disponibilidade", "O que cada produtor tem para vender agora. Liga um produtor a um produto."},
                {"historico_consumo", "Quanto foi vendido de cada produto em cada mês. É a base da estimativa de plantio."}
            };

            StringBuilder html = new StringBuilder();
            html.append("<h1>Ver banco de dados</h1>");
            html.append("<p>Tudo o que o sistema guarda fica em um único arquivo, o <strong>ecologico.db</strong> (SQLite), ");
            html.append("criado sozinho na primeira execução. Cadastre algo no site e recarregue esta página para ver os dados entrarem.</p>");
            for (String[] t : tabelas) {
                html.append("<h2>").append(t[0]).append("</h2>");
                html.append("<p class=\"dica\">").append(t[1]).append("</p>");
                html.append("<div class=\"rolagem\">").append(Database.tabelaComoHtml(t[0])).append("</div>");
            }
            responder(ex, 200, Html.pagina("Banco de dados", html.toString()));
        } catch (Exception e) {
            erro(ex, e);
        }
    }
}
