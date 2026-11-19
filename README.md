<!--
Markdown Cheatsheet
https://github.com/adam-p/markdown-here/wiki/markdown-cheatsheet
-->

## 🚀 MCP Server: Exemplo 🚀

Este projeto demonstra o uso do Protocolo de Contexto do Modelo ([MCP](https://modelcontextprotocol.io/docs/2026-07-28/getting-started/intro) - Model Context Protocol) usando o [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk).

Com este SDK deve-ser programar todos os detalhes, tanto do servidor quanto do cliente.

A camada de transporte administra a comunicação entre cliente e servidor, entre outras tarefas, podendo ser STDIO ou *Streamable* HTTP.

Neste caso, é demonstrado o uso de STDIO como modelo de comunicação entre o cliente e o servidor.

O MCP define três primitivas básicas: *tools* (ferramentas), *resources* (recursos) e *prompts*.

Os esquemas JSON, usados pelas ferramentas para indicar a estruturas de dados que serão usadas para torca de informações entre cliente e servidor estão localizados em **src/main/resources/json-schemas** do projeto e no diretório **json-schemas**, localizado dentro do arquivo *.jar*, gerado pelo *build* do servidor. 

Este projeto implementa *tools*, *prompts* e *resources*, a saber:
### 🧰 Tools 🧰
**Diretório Autorizado**: recupera o diretório autorizado passado ao servidor pela variável de ambiente `MCP_FILES_ROOT` ou pela propriedade `-D=mcp.files.root`. Esta ferramenta não possui argumentos.

**Busca de Arquivos**: a partir do diretório autorizado (este argumento é apresentado mais abaixo), a ferramenta fará buscas de acordo com os parâmetros forneceidos na sua execução seguindo o padrão Glob. Mais info [aqui](https://en.wikipedia.org/wiki/Glob_(programming) e [aqui](https://www.digitalocean.com/community/tools/glob?comments=true&glob=%2F%2A%2A%2F%2A.js&matches=false&tests=%2F%2F%20This%20will%20match%20as%20it%20ends%20with%20%27.js%27&tests=%2Fhello%2Fworld.js&tests=%2F%2F%20This%20won%27t%20match%21&tests=%2Ftest%2Fsome%2Fglobs).
Argumentos:
* **pattern**: padrão *glob* para valor de busca (obrigatório)
* **recursive**: pesquisar sub-diretórios? (opcional, false como padrão)
* **max_results**: total de resultados (1..100) (opcional, 20 como padrão)

**Contar Parágrafos**: recupera a quantidade de parágrafos de um documento do MS Word. Argumentos:
* **input_file**: caminho para o arquivo MS Word (obrigatório).

**Contar Palavras**: recupera a quantidade de palavras de um documento do MS Word. Argumentos:
* **input_file**: caminho para o arquivo MS Word (obrigatório).

**Metadados PDF**: recupera os metadados de um arquivo PDF. Argumentos:
* **input_file**: caminho para o arquivo PDF (obrigatório).

**Buscar & Substituir em PDF**: a partir do diretório autorizado, a ferramenta recupera o arquivo PDF informado pelo usuário e realiza busca de padrões em seu conteúdo, retornando a quantidade de ocorrênicas em cada página do arquivo. Argumentos:
* **file_input**: caminho para o arquivo PDF de entrada (obrigatório)
* **file_output**: caminho para o arquivo PDF de saída (opcional),
* **search**: texto a pesquisar, com ou sem expressões regulares (obrigatório)
* **replace**: texto a substituir (obrigatório),
* **use_regex**: se o argumento **search** contiver expressões regulares, este argumento deve ser true, caso contrário, false (opcional, false como padrão),
* **case_sensitive**: diferencia letras maiúsculas de minúsculas? (opcional, false como padrão).

**Buscar em PDF**: a partir do diretório autorizado, a ferramenta recupera o arquivo PDF informado pelo usuário e realiza busca de padrões em seu conteúdo, retornando a quantidade de ocorrênicas em cada página do arquivo. Argumentos:
* **input_file**: caminho para o arquivo PDF de entrada (obrigatório)
* **search**: texto a pesquisar, com ou sem expressões regulares (obrigatório)
* **use_regex**: se o argumento **targetText** contiver expressões regulares, este argumento deve ser true, caso contrário, false (opcional, false como padrão)
* **case_sensitive**: diferencia letras maiúsculas de minúsculas? (opcional, false como padrão).

**Simple Report Tool**: ferramenta de exemplo para um relatório simulado. Argumentos:
* **id**: identificador do relatório (obrigatório).

### 🪄 Prompts 🪄
**Analisador de PDF**: fornece um prompt para a análise de um documento PDF tendo em vista o contexto informado. Argumentos:
* **arquivo**: caminho para o arquivos PDF (obrigatório).
* **termo**: valor de busca (obrigatório).
* **contexto**: contexto no qual a análise será realizada (opcional, vazio por padrão).

**Saudação**: fornece um prompt para uma mensagem de saudação/cumprimento ao nome recebido.

### 📚 Resources 📚
**Sobre o servidor MCP**: retorna informações sobre o servidor MCP: nome, versão e descrição.

**Informações da raiz de arquivos**: retorna o estado do diretório autorizado: caminho, se é ou não diretório, se está ou não acessível para leitura.

---

### 📌 Executar a aplicação 📌
Este é um projeto único para duas aplicações: servidor e cliente. O servidor é executado a partir da classe `br.edu.ifpr.mcp_server.main.McpServer01SyncStdio`. Já o cliente é executado a partir da classe `br.edu.ifpr.mcp_server.main.McpClient01SyncStdio`.

Inicialmente, é preciso gerar a aplicação servidor e, em seguida, executar o cliente, tanto no [Eclispe IDE](https://www.eclipse.org/) quanto em linha de comando (CLI - *Command Line Interface*).

Além disto, é possível executar o servidor MCP Java em assistentes de IA locais, tais como [Claude Desktop](https://claude.com/product/overview), [Gemini CLI](https://geminicli.com/) e [Cursor CLI](https://cursor.com/cli).

Também é possível verificar e testar o servidor MCP Java com o [MCP Inspector](https://cursor.com/cli). A [Anthropic](https://cursor.com/cli) criou esta ferramenta para testar e depurar servidores MCP locais e remotos. Ela possui interface web, CLI e TUI (*Text User Interface*). Mais adiante, este documento mostra os passos necessários para testar o servidor MCP Java com o MCP Inspector rodando como um contêiner Docker no Docker Desktop, juntamente com os arquivos `Dockerfile` e `compose.yaml`, disponíveis na raiz do projeto.

#### ⚙️ Eclipse IDE ⚙️
1. Clicar com o botão direito do mouse no nome do projeto;
2. Selecionar **Run As** > **Maven build...**
3. Na janela **Edit configuration and launch**:
    * Na aba **Main** digitar **clean package -Pserver -DskipTests** no campo **Goals**
    * Na aba **JRE** digitar **--enable-native-access=ALL-UNNAMED** no campo **VM arguments**
4. Clicar no botão **Run**

Os passos anteriores devem ser executados somente na primeira vez. A partir da segunda vez clicar na "pontinha de flecha" ao lado do botão *play*, na barra de botões do Eclipse IDE.

Se o processo de construção da aplicação servidor ocorreu sem erros, ele produzirá o diretório (pasta) **server** na raiz do projeto. Este diretório contém a aplicação executável do servidor e o sub-diretório **libs** contendo as bibliotecas de código necessárias ao servidor.

**A aplicação servidor é executada pela aplicação cliente e não precisa de intervenção do programador.**

Para gerar a aplicação cliente, deve-se executar os quatro passos mostrados anteriormente, com a seguinte diferença, na aba **Main** digitar **clean package -Pclient -DskipTests**, e este procedimento, assim como o anterior, deve ser realizado apenas na primeira vez.

Se o processo de construção da aplicação cliente ocorreu sem erros, ele produzirá o diretório **dist-client** como sub-diretório de **target**. O diretório **dist-client** contém a aplicação cliente e o sub-diretório **libs** com as bibliotecas necessária à sua execução.

#### ⚙️ Linha de Comando (CLI) ⚙️
Abrir uma janela do terminal, Prompt de Comando ou PowerShell no Windows, por exemplo, mudar para o diretório raiz do projeto e gerar as aplicações:

```
# gerar a aplicação servidor
mvn clean package -Pserver -DskipTests
```
```
# gerar a aplicação cliente
mvn clean package -Pclient -DskipTests

# executar o cliente MCP Java, que executará o servidor MCP Java
# DIRETÓRIO_AUTORIZADO é passado como parâmetro ao cliente, que
# repassará ao servidor

java -jar mcp_server01_client.jar DIRETÓRIO_AUTORIZADO
```

ou

```
# executar o cliente MCP Java, que executará o servidor MCP Java
# DIRETÓRIO_AUTORIZADO é disponibilizado por meio da variável
# de ambiente MCP_FILES_ROOT e será repassado ao servidor
set MCP_FILES_ROOT=DIRETÓRIO_AUTORIZADO
java -jar mcp_server01_client.jar
set MCP_FILES_ROOT=
```

ou

```
# executar o cliente MCP Java, que executará o servidor MCP Java
# DIRETÓRIO_AUTORIZADO é disponibilizado por meio de um parâmetro
# à JVM (propriedade) e será repassado ao servidor
java -Dmcp.files.root=DIRETÓRIO_AUTORIZADO  -jar mcp_server01_client.jar
```

Independentemente do modo de execução do processo de construção (*build*), se realizado no Eclipse IDE ou em CLI, uma vez que ambas as aplicações tenham sido criadas, o programador deve abrir uma janela do terminal e mudar para o diretório **dist-client**. Estando neste diretório usar uma das opçãos acima, pressionar a tecla ENTER <img width="20" height="20" src="https://img.icons8.com/color/48/enter-key.png" alt="enter-key"/> e aguardar a execução do cliente que, por sua vez, executará o servidor e apresentará ao usuário um menu para interagir com a(s) ferramenta(s) disponibilizadas pelo servidor.

O argumento **DIRETÓRIO_AUTORIZADO** indica um caminho (diretório/pasta) local a partir do qual o servidor terá acesso para executar algumas de suas ferramentas (*tools*).

A segunda e terceira opções são mostradas na sequência para configuração do Claude Desktop.

---

### ⚙️ MCP Inspector no [Docker Desktop](https://www.docker.com/products/docker-desktop/?at_exp=DO112.A) ⚙️
Certificar-se de que o Docker Desktop esteja em execução antes de proceder com os passos à seguir:.

1. Editar o arquivo `compose.yaml` e localizar a entrada `source` para inserir um caminho válido no sistema de arquivos

2. Abrir uma janela do terminal, Prompt de Comando ou PowerShell no Windows, por exemplo
3. Mudar para o diretório raiz do projeto
4. Executar o comando `docker compose up -d --build mcp-inspector` (veja a seguir)
5. Aguardar até que a imagem tenha sido criada e executada no Docker Desktop
6. Na barra de endereços do navegador, digitar `http://127.0.0.1:6274` e pressionar ENTER <img width="20" height="20" src="https://img.icons8.com/color/48/enter-key.png" alt="enter-key"/>

O passo **4** é uma composição, ou seja, o comando `compose` processa o arquivo `compose.yaml`, que, por sua vez, processa o arquivo `Dockerfile` para criar, se necessário, a imagem Docker. Após a criação da imagem Docker, o `compose` continua sua execução e implanta um conjunto de serviços, o MCP Inspector + o MCP Java Server, no Docker Desktop. Ambos os arquivos encontram-se no diretório raiz do projeto.

Se os passos anteriores ocorreram com sucesso, o MCP Inspector exibirá o servidor MCP Java, ferramenta(s), recurso(s) e prompt(s) para testes.

O arquivo `compose.yaml` foi preparado para a construção de duas imagens:
1. A imagem contendo o MCP Inspector com o MCP Java Server: adicionar a opção `--build mcp-inspector` na linha de comando
2. A imagem do MCP Java Server para ser usada com um assistente de IA. Neste caso, quem executará o `compose` é a aplicação de assistente de IA (ver abaixo).

O diretório autorizado, aquele a partir do qual as operações com arquivos serão realizadas, **deve** ser configurado no arquivo `.env` (copie de `.env.example` e ajuste o caminho da sua máquina), no diretório raiz do projeto.

---

### 📌 Instalar o servidor MCP Java Local no [Claude Desktop](https://claude.com/) 📌 ###
- Localizar o diretório `C:\Users\<USUÁRIO_LOGADO>\AppData\Local\Packages\Claude_pzs8sxrjxfjjc\LocalCache\Roaming\Claude` ou `C:\Users\<USUÁRIO_LOGADO>\AppData\Local\Claude-Data\claude_pzs8sxrjxfjjc`
- Editar o arquivo `claude_desktop_config.json`
- Se não existir, criar a entrada `mcpServers` com um dos conteúdos abaixo

```
  "mcpServers": {
    "NOME_DO_SERVIDOR": {
      "command": "java",
      "args": [
        "-jar",
        "DRIVE:\\caminho\\absoluto\\para\\servidor.jar"
      ],
      "env": { "MCP_FILES_ROOT": "DIRETÓRIO_AUTORIZADO"}
    }
  }
```
ou

```
  "mcpServers": {
    "NOME_DO_SERVIDOR": {
      "command": "java",
      "args": [
         "-Dmcp.files.root=DIRETÓRIO_AUTORIZADO",
         "-jar",
         "DRIVE:\\caminho\\absoluto\\para\\servidor.jar"
      ]
    }
  }
```

Ainda, é possível usar o MCP Java Server diretamente no Docker com a seguinte configuração

```
    "NOME_DO_SERVIDOR": {
      "command": "docker",
      "args": [
        "compose",
        "-f",
        "DRIVE:\\caminho\\absoluto\\do\\projeto\\mcp-gastobot\\compose.yaml",
        "run",
        "--run",
        "-T",
        "--no-deps",
        "mcp-server-ai-host"
      ]
    }
```
- Salvar o arquivo e iniciar ou reiniciar o Claude Desktop (se o Claude Desktop estiver em execução deve ser completamente encerrado e não apenas sua janela fechada).


O diretório `Users` também pode ser escrito como `Usuários`.

`USUÁRIO_LOGADO` o nome do diretório é o mesmo nome usado para *login* no Windows.
 
O diretório `Claude_pzs8sxrjxfjjc` pode possuir um nome diferente do apresentado, mas o prefixo `Claude_` será o mesmo.

`NOME_DO_SERVIDOR` é o nome pelo qual o MCP Java Server será apresentado ao usuário, como, p.ex., `mcp_java_server`.

`DRIVE:` é a indicação do *drive* no sistema de arquivos do Windows.

`\\` caractere que separa nomes de caminhos no Windows (`/` para Linux, Unix e macOS).

O primeiro exemplo é genérico o suficiente e pode ser usado com servidores MCP implementados com qualquer tecnologia de desenvolvimento de software, desde que sejam capazes de ler o ambiente (variáveis de ambiente) em que são executadas..

O segundo exemplo é específico para a linguagem Java, pois usa parâmetros que são passados à Máquina Virtual Java (JVM - *Java Virtual Machine*) através de propriedades.

Para o primeiro e segundo exemplos, o Java (JDK - *Java Development Kit* ou JRE - *Java Runtime Environment*) **deve** estar instalado e adequadamente configurado.

O terceiro exemplo usará o `compose.yaml` do projeto para que o Claude Desktop inicie a execução do `compose`, que, por sua vez, criará uma imagem e a executará (tornando-se um contêiner) no Docker Desktop.

O diretório autorizado, aquele a partir do qual as operações com arquivos serão realizadas, **deve** ser configurado no arquivo `.env` (copie de `.env.example` e ajuste o caminho da sua máquina), no diretório raiz do projeto.

---

## 🗣️ **OBSERVAÇÃO IMPORTANTE** 🗣️
> Sempre que o artefato SERVIDOR é gerado, o artefato CLIENTE é apagado do diretório "target", devido ao objetivo (*goal*) `clean`, e deve ser gerado novamente (mesmo que não tenha havido mudança de código nele).

---

## 🕵️‍♂️ TESTES DE SOFTWARE 🕵️‍♂️
Algumas características e dados importantes acerca de testes de software.

* **Projeto orientado pela demanda**: ao invés de escrever o código e testá-lo depois, escreve-se um teste que requeira uma determinado comportamento. Isto conduz ao raciocínio sobre a interface, limites e perspectiva do consumidor (programador e **testers**, entre outros) antes que detalhes da implementação tornem-se caóticos;
* **Pequenos e seguros passos**: a carga cognitiva é minimizada ao dividir o problema em partes menores e gerenciáveis. Desta forma, pensa-se na implementação necessária ao teste e cria-se um mecanismo contínuo de **feedback**;
* **Documentação ativa**: o conjunto de testes atua como uma documentação atualizada de como a aplicação deve se comportar. Se um teste é difícil de ler ou escrever, provavelmente, há uma falha na implementação do código.

**Ciclo Red-Green-Refactor**

* **Red**: escrever um teste que falha (a funcionalidade ainda não existe), isto garante que o teste existe;
* **Green**: implementar minimamente a funcionalidade para que o teste passe sem se importar com a qualidade da implementação;
* **Refactor**: realizar refatorações e refinamentos sem quebrar os testes.-

O diretório `src/test/java` é o local onde se localizam as classes para testes, tanto de integração, classes com o sufixo **IT**, quanto unitários, classes com o sufixo **Test**.

#### 📌 Executar Testes Unitários 📌
As classes de teste possuem os mesmos nomes, com o acréscimo do sufixo `Test`, das classes que implementam as funcionalidades, cujos nomes possuem o sufixo `Service`, e estão localizadas em pacotes, cujos nomes são os mesmos dos pacotes nos quais as classes de código residem.

Seguir os passos acima para criar um *build*, se for a primeira execução. No campo `Goals` digitar `clean test`.

Para executar os testes de integração em linha de comando, digitar `mvn clean test`.

Para testar apenas uma das várias classes de testes disponíveis, o campo `Goals` deve informar `clean test -Dtest=NomeDaClasse`.


Para testar apenas um método de uma determinada classe, o campo `Goals` deve informar `clean test -Dtest=NomeDaClasse#NomeDoMétodo`.


#### 📌 Executar Testes de Integração (IT - *Integration Test*) 📌
As classes de teste possuem os mesmos nomes, com o acréscimo do sufixo `IT`, das classes que implementam as ferramentas, cujos nomes possuem o sufixo `Tool`, e estão localizadas em pacotes, cujos nomes são os mesmos dos pacotes nos quais as classes de código residem.

Seguir os passos acima para criar um *build*, se for a primeira execução. No campo `Goals` digitar `clean verify -Pserver`.

Para executar os testes de integração em linha de comando, digitar `mvn clean verify -Pserver`.

Para testar apenas uma das várias classes de testes disponíveis, o campo `Goals` deve informar `clean verify -Pserver -Dit.test=NomeDaClasse`.


Para testar apenas um método de uma determinada classe, o campo `Goals` deve informar `clean verify -Pserver -Dit.test=NomeDaClasse#NomeDoMétodo`.

---

#### 📘 REFERÊNCIAS 📘 ####
[What is the Model Context Protocol (MCP)? - Documentação Oficial](https://modelcontextprotocol.io/docs/2026-07-28/getting-started/intro)

[MCP Java SDK 1](https://github.com/modelcontextprotocol/java-sdk)

[MCP Java SDK 2](https://java.sdk.modelcontextprotocol.io/latest/)

[Building a JDBC-based MCP Server: A Developer Guide](https://www.cdata.com/kb/articles/jdbc-mcp-server-dev-guide.rst)

[Learn MCP Java Tutorial - Microsoft](https://github.com/microsoft/lets-learn-mcp-java/tree/main)

[Tutorial 1 no GitHub](https://github.com/eugenp/tutorials/tree/master/java-mcp)

[Tutorial 2 no GitHub](https://github.com/CDataSoftware/cdata-jdbc-mcp-server)

---

[Test Driven Development (Martin Fowler)](https://martinfowler.com/bliki/TestDrivenDevelopment.html)

[GeeksforGeeks TDD Guide](https://www.geeksforgeeks.org/software-testing/the-importance-of-test-driven-development-tdd/)