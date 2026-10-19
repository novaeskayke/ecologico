package com.ecologico;

/** Monta a "moldura" que se repete em todas as páginas (topo com menu + rodapé). */
public class Html {

    public static String pagina(String tituloAba, String conteudo) {
        String layout = """
            <!DOCTYPE html>
            <html lang="pt-br">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>%s - Ecologico</title>
            <link rel="preconnect" href="https://fonts.googleapis.com">
            <link href="https://fonts.googleapis.com/css2?family=Fraunces:ital,wght@0,600;1,500&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
            <link rel="stylesheet" href="/static/style.css">
            </head>
            <body>
              <header class="topo">
                <a href="/" class="logo">Ecologico</a>
                <nav>
                  <a href="/produtor">Sou produtor</a>
                  <a href="/consumidor/busca">Sou consumidor</a>
                  <a href="/banco">Ver banco de dados</a>
                </nav>
              </header>
              <main class="conteudo">
            %s
              </main>
              <footer class="rodape">Protótipo acadêmico · Grupo Nexus Urbano</footer>
            </body>
            </html>
            """;
        return layout.formatted(tituloAba, conteudo);
    }
}
