# 🌱 Ecológico

> Plataforma que conecta produtores e consumidores de orgânicos: o produtor planeja o plantio pelo histórico de consumo e o consumidor encontra quem vende por perto.

![Java](https://img.shields.io/badge/Java-17+-orange)
![SQLite](https://img.shields.io/badge/SQLite-3-blue)
![Status](https://img.shields.io/badge/status-protótipo-green)

![Busca de alimentos](docs/img/busca.png)

## 📌 Sobre o projeto

Parte da produção de alimentos se perde por falta de planejamento e de canais de venda direta. O Ecológico ataca os dois problemas:

- o **produtor** recebe uma estimativa de quanto plantar, baseada no que a região realmente consome;
- o **consumidor** busca um alimento e vê quem tem disponível, do mais perto ao mais longe.

Projeto acadêmico desenvolvido pelo grupo **Nexus Urbano**, com a ideia validada pelos professores orientadores.

## ✨ Funcionalidades

**Produtor**

- Cadastro e painel em 3 passos
- Anúncio dos produtos disponíveis
- Registro de vendas por mês
- Estimativa de plantio com a conta explicada passo a passo

**Consumidor**

- Busca por alimento, ignorando acento e maiúscula
- Resultados com quantidade, preço e contato
- Ordenação por distância (quando a localização é permitida)

**Geral**

- Página "Ver banco de dados" para visualizar o que foi salvo

## 🖼️ Telas

| Painel do produtor             | Estimativa de plantio                  |
| ------------------------------ | -------------------------------------- |
| ![Painel](docs/img/painel.png) | ![Estimativa](docs/img/estimativa.png) |

## 🧮 Como a estimativa é calculada

1. Soma as vendas de cada mês do histórico
2. Divide pelo número de meses, obtendo a média
3. Aplica uma margem de segurança de 15% para cobrir perdas

Exemplo: `(320 + 290 + 410) ÷ 3 = 340` e `340 × 1,15 = 391 unidades`.

## 🛠️ Tecnologias

- Java 17+ (servidor HTTP nativo, sem frameworks)
- SQLite (banco em arquivo, criado automaticamente)
- HTML e CSS servidos pelo próprio backend

## 🚀 Como rodar

**Pré-requisito:** JDK 17 ou mais novo (`javac -version`).

```bash
git clone https://github.com/SEU-USUARIO/ecologico.git
cd ecologico

# baixa o driver do SQLite
mkdir -p lib
curl -L -o lib/sqlite-jdbc-3.41.2.2.jar \
  https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.41.2.2/sqlite-jdbc-3.41.2.2.jar

# compila e executa
bash rodar.sh        # Linux/Mac
# rodar.bat          # Windows (dois cliques)
```

Abra http://localhost:8080. O banco `ecologico.db` é criado na primeira execução, já com dados de exemplo.

## 📁 Estrutura

```
src/com/ecologico/
  Main.java       servidor e páginas (comece por aqui)
  Database.java   criação do banco e dados de exemplo
  *DAO.java       acesso ao banco
web/static/       estilos
docs/             landing page e imagens
```

## ⚠️ Limitações

- Sem login: qualquer pessoa acessa o painel de qualquer produtor
- Distância calculada em linha reta, não por rota
- Histórico de vendas compartilhado por produto (visão da região)

## 🔭 Próximos passos

- [ ] Login para produtores e consumidores
- [ ] Margem de segurança editável
- [ ] Distância por rotas
- [ ] Testes com dados reais

## 👥 Equipe

Grupo **Nexus Urbano**: Kayke Novaes, Gabrielly dos Santos, Lygia Soares, Murilo do Amaral, Kaique Viera e Gabriel Cavalcanti.

Orientadores: Brenda Medina e Daniel Ohata.
