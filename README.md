# GastoBot

Controle de entradas e saídas em Java, com servidor e cliente MCP no mesmo projeto Maven. Usa Controller → Service → Repository, DTOs imutáveis com records e SQLite via JDBC. Sem Spring, Lombok ou geração de código da aplicação.

## Estado atual

- Registro de entradas e saídas no SQLite.
- Consulta por UUID e usuário proprietário.
- Consulta por uma data ou período inclusivo, com páginas de 10 lançamentos.
- Relatório com totais e quantidades de entradas e saídas do dia, semana e mês calendário.
- Cliente MCP Java de terminal que descobre ferramentas e aceita argumentos JSON genéricos.
- Bot Telegram com `/entrada`, `/saida`, `/registrar`, `/consultar` e `/relatorio`, executando ferramentas pelo MCP.
- Servidor MCP via STDIO, configurável para cliente Java, Claude Desktop e MCP Inspector.

## Arquitetura

Os pacotes ficam em `src/main/java/br/edu/ifpr/gastobot`:

| Pacote | Responsabilidade |
| --- | --- |
| `client` | Cliente MCP Java de terminal |
| `client.mcp` | Conexão STDIO reutilizada pelo terminal e Telegram |
| `client.telegram` | API HTTP, long polling, interpretação de mensagens e adaptação para MCP |
| `server` e `server.mcp` | Inicialização STDIO e adaptação das ferramentas |
| `controller` | Entrada e resposta dos casos de uso |
| `dto` | Entradas e respostas imutáveis de registro, consulta paginada e relatório |
| `model` | Lançamento, enums e invariantes do domínio |
| `service` | Coordenação dos casos de uso |
| `repository` e `repository.sqlite` | Contrato de persistência e implementação JDBC |
| `config` e `exception` | Configuração do banco e falhas de persistência |

O servidor monta as dependências por construtores. O service depende de `LancamentoRepository`; o controller não conhece MCP nem SQL. As ferramentas convertem parâmetros em DTOs e respostas em JSON. Conexões, statements e resultados são fechados com try-with-resources. Consultas usam parâmetros preparados.

O bot não acessa SQLite nem chama diretamente os services do servidor: `Telegram → TelegramApiClient → TelegramUpdateHandler → TelegramCommandService → McpGateway → servidor MCP → Controller → Service → Repository`. A interface `McpGateway` permite testar a adaptação dos comandos sem conexão com o Telegram.

## Pré-requisitos e builds

Instale JDK 25 e Apache Maven. Confira `java -version`, `javac -version` e `mvn -version`. Execute os comandos na raiz do projeto. Não há Maven Wrapper.

```sh
# Testes unitários, sem iniciar servidor
mvn test

# Gerar o servidor (o perfil pula unitários por padrão)
mvn -Pserver package

# Unitários + integração SQLite + chamadas MCP ao JAR real
mvn -Pserver "-Dskip.unit.tests=false" verify

# Apenas integração
mvn -Pserver verify

# Gerar o cliente
mvn -Pclient package
```

Encerre o bot com Ctrl+C antes de atualizar os artefatos; no Windows, processos em execução mantêm os JARs bloqueados. Gere servidor e cliente separadamente, sem `clean` entre eles. O servidor é gerado em `server/mcp_server01_server.jar`, acompanhado de `server/libs/`. O cliente é gerado em `target/dist-client/mcp_server01_client.jar`, acompanhado de `target/dist-client/libs/`. Os nomes dos artefatos foram mantidos para continuidade com o projeto original. Distribua cada JAR junto à sua pasta `libs`.

Os testes unitários usam repository e gateway simulados. Os testes `*IT` usam arquivos SQLite temporários; os testes MCP iniciam o JAR como subprocesso e executam ferramentas pelo protocolo STDIO. São verificados centavos exatos, persistência após reabertura e reinício, isolamento por usuário, duplicidade, dados inválidos e falhas de leitura e gravação. Os testes Telegram verificam interpretação de mensagens, identidade e isolamento do usuário, filtro de chats privados, offset do polling, erros HTTP, sanitização do token, formatação e fuso horário. A integração HTTP usa servidor local falso; a integração financeira usa o servidor MCP real. Os testes não acessam o Telegram nem exigem token. Os testes não alteram o banco padrão. Relatórios: `target/surefire-reports` e `target/failsafe-reports`.

O relatório é testado nos limites do dia, semana e mês, em mudanças de mês/ano e em fevereiro de ano bissexto. Os testes verificam quantidades por tipo, ausência de registros, soma acima do limite de um inteiro de 64 bits sem perda de precisão e inclusão de todos os registros, independentemente da paginação. Também verificam que o Telegram não exibe os IDs no registro ou na consulta.

## Banco de dados

Precedência da configuração:

1. Propriedade JVM `gastobot.db.path`.
2. Variável de ambiente `GASTOBOT_DB_PATH`.
3. Valor `GASTOBOT_DB_PATH` no `.env`.
4. Padrão `data/gastobot.db`, relativo ao diretório de execução.

O caminho é normalizado para absoluto. Diretórios e tabela são criados automaticamente. Para compartilhar dados entre cliente Java, Claude e Inspector, configure o mesmo caminho absoluto em todos eles. O Java carrega o `.env` do diretório de execução automaticamente. Execute os comandos na raiz do projeto para usar o `.env` e os caminhos padrão desse diretório.

O valor é validado com `BigDecimal` e armazenado como `INTEGER` em centavos, sem arredondamento. A coluna restringe o tipo e exige valor positivo. O banco guarda UUID, usuário, tipo, forma de pagamento, descrição e data ISO. Cada operação abre sua própria conexão, com espera de até cinco segundos se o SQLite estiver ocupado. A inicialização usa `CREATE TABLE IF NOT EXISTS`; migrações de versões futuras ainda não foram implementadas.

## Cliente Java

Após gerar os dois artefatos, execute na raiz:

```sh
java -jar target/dist-client/mcp_server01_client.jar
```

Para configurar os caminhos explicitamente:

```sh
java "-Dgastobot.db.path=E:/mcp-gastobot/data/gastobot.db" "-Dgastobot.server.jar=E:/mcp-gastobot/server/mcp_server01_server.jar" -jar target/dist-client/mcp_server01_client.jar
```

O cliente inicia o servidor com o Java da própria JVM. No CMD, PowerShell ou console do Eclipse, digite `/saida` e responda às perguntas sobre valor, pagamento e descrição. Em seguida, use `/relatorio` para conferir o total ou `/consultar` para buscar lançamentos por data ou período. Também estão disponíveis `/entrada`, `/ajuda` e `/cancelar`. Esse modo funciona sem token e sem acesso ao Telegram.

Os comandos guiados do terminal usam o proprietário `terminal:local` e o horário de São Paulo. Cada usuário do Telegram tem seu próprio histórico no mesmo banco. Para testar qualquer ferramenta MCP diretamente, digite `/ferramentas`, escolha o nome e informe os argumentos JSON em uma linha. Use `sair` para encerrar.

## Bot Telegram

Com `--telegram`, o mesmo JAR mantém o terminal interativo e atende o Telegram em segundo plano. Se o token estiver ausente, inválido ou o bot estiver indisponível, você pode continuar a demonstração no próprio terminal. Digite `sair` ou feche a entrada do console para encerrar os dois canais. Para executar apenas o bot, inclusive com entrada redirecionada, use `--somente-telegram`.

No Windows, para abrir o GastoBot em uma janela própria, execute este comando **no CMD, na raiz do projeto**:

```cmd
start "GastoBot" cmd /k java -jar target\dist-client\mcp_server01_client.jar --telegram
```

### Configuração pelo BotFather

1. No Telegram, abra o bot oficial `@BotFather` e use `/newbot` para criar o seu bot, caso ainda não tenha criado.
2. Guarde o token fornecido pelo BotFather no ambiente local. Não o envie em mensagens, não o inclua no código e não faça commit dele.
3. Configure `TELEGRAM_BOT_TOKEN` no `.env` da raiz do projeto ou no ambiente do terminal. Não é necessário ID de servidor, grupo ou chat: o chat e o usuário são obtidos da mensagem recebida.
4. Inicie a aplicação, abra o chat privado do seu bot pelo nome de usuário escolhido no BotFather e envie `/start`.

| Variável | Obrigatória | Uso |
| --- | --- | --- |
| `TELEGRAM_BOT_TOKEN` | Sim, no modo Telegram | Token fornecido pelo BotFather |
| `GASTOBOT_TIME_ZONE` | Não | Fuso da data padrão; padrão `America/Sao_Paulo` |
| `GASTOBOT_DB_PATH` | Não | Mesmo caminho do banco usado pelos demais clientes |

Copie `.env.example` para `.env` na raiz e preencha `TELEGRAM_BOT_TOKEN` com o token do BotFather. Se o `.env` já existir, basta ajustar essa chave. Em seguida, execute na raiz:

```sh
java -jar target/dist-client/mcp_server01_client.jar --telegram
```

O token é carregado automaticamente do `.env`; não é necessário imprimi-lo ou exportá-lo no terminal.

O carregador aceita linhas `CHAVE=valor`, valores entre aspas simples ou duplas e comentários em linhas iniciadas por `#`. Não executa comandos nem expande variáveis. Variáveis do ambiente têm precedência sobre o `.env`, inclusive se estiverem vazias. Se definiu um token antigo no PowerShell, remova essa variável com `Remove-Item Env:TELEGRAM_BOT_TOKEN -ErrorAction SilentlyContinue` para usar o arquivo. O `.env` continua ignorado pelo Git.

Como alternativa ao arquivo, no PowerShell, para informar o token sem deixá-lo no histórico:

```powershell
$telegramToken = Read-Host 'Token do BotFather' -AsSecureString
$env:TELEGRAM_BOT_TOKEN = [System.Net.NetworkCredential]::new('', $telegramToken).Password
$env:GASTOBOT_DB_PATH = 'E:/mcp-gastobot/data/gastobot.db'
java -jar target/dist-client/mcp_server01_client.jar --telegram
```

Para escolher explicitamente o banco:

```sh
java "-Dgastobot.db.path=E:/mcp-gastobot/data/gastobot.db" -jar target/dist-client/mcp_server01_client.jar --telegram
```

Se configurou variáveis no terminal em vez do arquivo, use esse mesmo terminal. O cliente inicia o servidor MCP automaticamente. Encerre com Ctrl+C.

O comando `java` deve apontar para um JDK 25 ou superior. Confira com `java -version`. No PowerShell, se o JDK estiver configurado em `JAVA_HOME`, você também pode executá-lo diretamente:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -jar target/dist-client/mcp_server01_client.jar --telegram
```

### Comandos no chat privado

Envie apenas `/saida` ou `/entrada` para iniciar o cadastro guiado. O bot pergunta, em mensagens separadas, o valor, a forma de pagamento e a descrição. Depois da descrição, salva com a data de hoje. Envie `-` ou `pular` para concluir sem descrição. Valores ou formas inválidos mantêm a pergunta atual. `/registrar` pergunta primeiro o tipo; `/consultar` pergunta a data ou período. `/cancelar` abandona os dados ainda não enviados ao servidor. Cada usuário e chat têm seu próprio estado, mantido em memória até concluir, cancelar, reiniciar com um novo comando ou encerrar o processo.

O cadastro só é enviado ao servidor MCP ao concluir as perguntas. A ajuda aparece em `/start`, `/ajuda` ou `/help`; mensagens sem uma operação em andamento recebem uma orientação curta, sem registros ou valores fictícios. Os comandos completos em uma única mensagem também continuam disponíveis:

```text
/start
/ajuda
/saida 35,90 pix Almoço
/entrada 1500 transferencia Salário
/registrar saida 35.90 pix Almoço
/saida 35,90 credito data=2026-11-21 Almoço
/consultar 22/11/2026
/consultar 01/11/2026 22/11/2026
/consultar 2026-11-01 2026-11-22 pagina=2
/relatorio
```

`/saida` e `/entrada` são atalhos de `/registrar tipo valor forma descrição`. Valor e forma são obrigatórios. Formas aceitas: `pix`, `dinheiro`, `debito`, `credito`, `transferencia` e `boleto`; também são aceitos os nomes do enum, como `CARTAO_CREDITO`. Tipo e forma são normalizados, inclusive acentos. Não use separador de milhar no valor: `1.234,56`, notação científica e mais de duas casas decimais não são aceitos pelo comando Telegram.

A descrição é opcional e aceita até 500 caracteres. Para informar a data, use `data=AAAA-MM-DD` logo depois da forma de pagamento e antes da descrição. Sem data, usa o dia atual no fuso configurado. A validação de valor positivo e limite monetário continua no servidor MCP.

`/consultar` sem argumentos pergunta a data ou período. Responda com uma data (`22/11/2026` ou `2026-11-22`) ou duas datas separadas por espaço (`01/11/2026 22/11/2026`). Também aceita `a`, `ate` ou `até` entre as datas. As datas inicial e final são incluídas; fim anterior ao início e datas inexistentes são rejeitados. São consultadas as datas dos lançamentos, não a data em que a mensagem foi enviada.

A resposta lista até 10 registros, em ordem crescente por data e ID, com data, tipo, valor, forma e descrição resumida. Havendo mais resultados, inclui o comando da próxima página. Use `pagina=N` ao final do comando ou da resposta à pergunta do bot. Apenas descrições longas são abreviadas; os lançamentos não são descartados para caber na mensagem. Consultas sem registros retornam uma mensagem clara e não são erro.

O ID não aparece nas mensagens de registro, consulta individual ou consulta por período do Telegram. Os trechos de exibição estão comentados em `TelegramCommandService`, para permitir reativação. O UUID continua armazenado no SQLite e presente nos DTOs e nas respostas estruturadas MCP, preservando a identificação interna e a ferramenta `buscar_lancamento`. A consulta individual `/consultar UUID` permanece apenas como compatibilidade para quem já possui um identificador; o uso normal é por data ou período.

### Relatório do dia, semana e mês

Envie `/relatorio`, sem parâmetros. O bot retorna os três resumos do usuário que enviou a mensagem, sem listar lançamentos, descrições ou IDs:

- **Dia:** a data atual no fuso `GASTOBOT_TIME_ZONE`.
- **Semana:** segunda-feira a domingo da semana que contém essa data.
- **Mês:** primeiro ao último dia do mês calendário atual.

Cada resumo informa o intervalo, o total de entradas, o total de saídas, a quantidade de entradas, a quantidade de saídas e a quantidade total de lançamentos. Um período sem registros apresenta `R$ 0,00` e quantidade zero. Quantidade é contagem de lançamentos; valores monetários não são usados como contagem.

As datas inicial e final são incluídas. Semana e mês são intervalos completos do calendário, não os últimos 7 ou 30 dias: lançamentos cadastrados com datas futuras dentro desses intervalos também entram no resumo. O relatório usa a data do lançamento. Não calcula saldo e não traz registros individuais.

Todos os registros do intervalo são considerados, mesmo quando existem mais de 10. A soma é feita com `BigDecimal` sobre os centavos inteiros, sem `double` e sem usar `SUM(INTEGER)` do SQLite, para evitar arredondamento e overflow ao somar valores elevados. O resultado mostra valores com duas casas decimais.

`/start`, `/ajuda`, `/help` e texto desconhecido fora de uma conversa exibem instruções sem criar registros.

O bot processa apenas mensagens de texto de usuários em chats privados. Grupos, canais, mensagens editadas e mensagens de bots não executam operações financeiras. As respostas são mensagens comuns no chat privado, sem modo HTML ou Markdown; não são mensagens efêmeras.

O identificador do usuário vem de `message.from.id`, nunca do texto digitado, e é enviado ao MCP como `telegram:ID`. O prefixo separa usuários do Telegram de identificadores de outras interfaces. Os lançamentos são pessoais. Dados anteriores com IDs sem esse prefixo continuam no banco, mas não são automaticamente associados a uma conta Telegram.

### Comunicação e limites operacionais

A aplicação usa long polling (`getUpdates`) com espera de 25 segundos, sem servidor HTTP público ou webhook. Mantenha somente uma instância em execução para o mesmo token e não use simultaneamente um webhook. Token inválido (401) ou conflito de consumo (409) encerram o bot com mensagem sanitizada. Falhas transitórias recebem uma pausa antes de nova consulta.

O offset é mantido em memória e avança antes da entrega da resposta. Uma falha ao enviar a mensagem não repete a operação MCP na mesma execução. O Telegram confirma atualizações quando a próxima consulta envia um offset maior. Se o processo cair entre o cadastro e essa confirmação, uma atualização pode ser reapresentada no reinício; esta versão não oferece deduplicação persistente ou garantia de execução única. Uma mensagem repetida manualmente também cria outro registro.

Erros HTTP são sanitizados para não expor o token, que faz parte da URL da API. O bot não altera webhooks nem configura comandos remotamente na inicialização. Se desejar um menu de comandos, configure `/start`, `/ajuda`, `/entrada`, `/saida`, `/registrar`, `/consultar`, `/relatorio` e `/cancelar` com `/setcommands` no BotFather.

Referência do protocolo: [Telegram Bot API](https://core.telegram.org/bots/api). Os testes usam um servidor HTTP local falso e o servidor MCP real, sem acessar Telegram ou exigir token. A validação real de recebimento e entrega das mensagens deve ser feita com o seu bot e token.

## registrar_lancamento

Cada chamada válida cria um registro novo. Repetir a chamada cria outro lançamento; não há deduplicação automática.

| Parâmetro | Tipo MCP | Obrigatório | Regra |
| --- | --- | --- | --- |
| `usuarioId` | string | Sim | Não vazio; espaços das extremidades são removidos |
| `tipo` | string | Sim | `ENTRADA` ou `SAIDA` |
| `valor` | string | Sim | Decimal positivo em reais, com ponto, como `35.90` |
| `formaPagamento` | string | Sim | `PIX`, `DINHEIRO`, `CARTAO_DEBITO`, `CARTAO_CREDITO`, `TRANSFERENCIA` ou `BOLETO` |
| `descricao` | string | Não | Se omitida, vira texto vazio; espaços das extremidades são removidos |
| `data` | string | Sim | Data válida no formato `AAAA-MM-DD` |

O valor é texto no protocolo para preservar a precisão decimal. Não envie número JSON nem use vírgula. Frações de centavo e valores acima de `92233720368547758.07` são rejeitados. Zeros decimais excedentes são aceitos quando não exigem arredondamento. Saídas também usam valor positivo; o tipo indicará seu efeito no saldo futuro. Enums usam maiúsculas.

Exemplo de argumentos:

```json
{"usuarioId":"123","tipo":"SAIDA","valor":"35.90","formaPagamento":"PIX","descricao":"Almoço","data":"2026-11-21"}
```

Sucesso MCP: `isError=false`, com conteúdo textual JSON contendo os campos normalizados mais `id` (UUID). O valor retornado é texto com duas casas decimais. O bot Telegram formata essa resposta sem exibir o ID.

## buscar_lancamento

| Parâmetro | Tipo MCP | Obrigatório | Regra |
| --- | --- | --- | --- |
| `id` | string | Sim | UUID retornado pelo registro |
| `usuarioId` | string | Sim | Identificador do proprietário |

```json
{"id":"UUID_RETORNADO_NO_REGISTRO","usuarioId":"123"}
```

Sucesso: `isError=false`, com o mesmo formato de resposta do registro. Um ID inexistente ou pertencente a outro usuário produz o mesmo erro `NAO_ENCONTRADO`.

O filtro por usuário **não autentica o chamador MCP**: o servidor atual é local e confia no `usuarioId` recebido. O bot já obtém esse identificador da mensagem recebida pela API do Telegram. O cliente genérico e outros clientes MCP continuam em contexto local de confiança; esta versão não oferece autenticação para expor o servidor MCP publicamente.

## listar_lancamentos

Consulta os lançamentos do usuário em um intervalo inclusivo. Para consultar um único dia, informe a mesma data em `dataInicio` e `dataFim`.

| Parâmetro | Tipo MCP | Obrigatório | Regra |
| --- | --- | --- | --- |
| `usuarioId` | string | Sim | Identificador do proprietário, não vazio |
| `dataInicio` | string | Sim | Data ISO `AAAA-MM-DD` |
| `dataFim` | string | Sim | Data ISO, igual ou posterior ao início |
| `pagina` | integer | Não | Padrão 1; positiva, com deslocamento suportado por inteiro de 32 bits |

```json
{"usuarioId":"telegram:123","dataInicio":"2026-11-01","dataFim":"2026-11-22","pagina":1}
```

Sucesso: `isError=false`, com conteúdo textual JSON contendo `dataInicio`, `dataFim`, `pagina`, `temProximaPagina` e `lancamentos`. A lista contém até 10 objetos com o mesmo formato de `LancamentoResponse`; os valores monetários permanecem strings com duas casas decimais. O serviço busca um registro adicional para detectar a próxima página, sem expô-lo na página atual. Não retorna dados de outro usuário. Uma lista vazia, inclusive uma página além do fim, é sucesso com `temProximaPagina=false`.

A consulta usa SQL parametrizado e ordena por `data ASC, id ASC`. Um índice em `(usuario_id, data, id)` é criado automaticamente para os bancos existentes. Parâmetros inválidos geram `DADOS_INVALIDOS`; falhas de leitura geram `ERRO_PERSISTENCIA`.

## relatorio_financeiro

Retorna resumos financeiros do usuário para o dia, a semana e o mês da data de referência. O Telegram envia a data atual no fuso configurado; outros clientes MCP devem informá-la explicitamente.

| Parâmetro | Tipo MCP | Obrigatório | Regra |
| --- | --- | --- | --- |
| `usuarioId` | string | Sim | Identificador do proprietário, não vazio |
| `dataReferencia` | string | Sim | Data ISO `AAAA-MM-DD`, usada para calcular os três intervalos |

```json
{"usuarioId":"telegram:123","dataReferencia":"2026-11-22"}
```

Sucesso: `isError=false`, com conteúdo textual JSON contendo `dataReferencia` e `periodos`, sempre na ordem `DIA`, `SEMANA`, `MES`. Cada período contém:

| Campo | Conteúdo |
| --- | --- |
| `periodo` | `DIA`, `SEMANA` ou `MES` |
| `dataInicio`, `dataFim` | Limites inclusivos, em ISO |
| `totalEntradas`, `totalSaidas` | Totais exatos, strings com duas casas decimais |
| `quantidadeEntradas`, `quantidadeSaidas` | Número de lançamentos de cada tipo |
| `quantidade` | Soma das duas quantidades |

Não há paginação nem lista de lançamentos nessa resposta. Registros de outros usuários e de datas fora de cada intervalo não entram no cálculo. Períodos vazios retornam totais `"0.00"` e contagens zero. Usuário/data inválidos ou parâmetros extras geram `DADOS_INVALIDOS`; erro de acesso ao banco gera `ERRO_PERSISTENCIA`.

## Erros das ferramentas MCP

Erros de execução usam `isError=true`, com conteúdo textual JSON contendo `codigo` e `mensagem`:

| Código | Situação |
| --- | --- |
| `DADOS_INVALIDOS` | Campo ausente, extra, com tipo incorreto, enum inválido, UUID/data inválidos ou valor inválido |
| `NAO_ENCONTRADO` | Nenhum lançamento com o par ID/usuário informado |
| `ERRO_PERSISTENCIA` | Falha de acesso, leitura ou escrita no SQLite |

Detalhes de persistência vão para stderr e não são devolvidos na resposta. Falhas ao preparar ou abrir o banco durante a inicialização impedem que o servidor inicie. Logs usam stderr; stdout fica reservado ao protocolo MCP. Clientes também podem rejeitar argumentos incompatíveis com o schema antes de executar a ferramenta.

## Claude Desktop

Instale o [Claude Desktop](https://claude.ai/download) e o JDK 25, e gere o servidor. Em Settings → Developer → Edit Config, adicione a entrada abaixo ao objeto `mcpServers` existente, ajustando os caminhos absolutos. No Windows, o arquivo fica em `%APPDATA%/Claude/claude_desktop_config.json`.

```json
{
  "mcpServers": {
    "gastobot": {
      "command": "C:/CAMINHO/DO/JDK-25/bin/java.exe",
      "args": [
        "-Dgastobot.db.path=E:/mcp-gastobot/data/gastobot.db",
        "-jar",
        "E:/mcp-gastobot/server/mcp_server01_server.jar"
      ]
    }
  }
}
```

Encerre completamente e reabra o Claude Desktop. Peça para listar ferramentas, registrar um lançamento para um usuário de teste e consultar o UUID retornado. Este servidor Java não precisa de Node.js. Referência: [guia oficial de servidores locais MCP](https://modelcontextprotocol.io/docs/develop/connect-local-servers).

## MCP Inspector

Instale uma versão do Node.js compatível com o [MCP Inspector](https://modelcontextprotocol.io/docs/tools/inspector). Com JDK 25 no PATH e servidor compilado:

```sh
npx @modelcontextprotocol/inspector java "-Dgastobot.db.path=E:/mcp-gastobot/data/gastobot.db" -jar E:/mcp-gastobot/server/mcp_server01_server.jar
```

Abra a URL informada, conecte usando STDIO e use a área de ferramentas para listar e executar `registrar_lancamento`, `buscar_lancamento`, `listar_lancamentos` e `relatorio_financeiro`. Também é possível iniciar o Inspector sem argumentos e preencher comando `java` e os argumentos acima na interface. Claude Desktop e Inspector têm instruções de configuração; a validação automatizada usa o cliente do SDK MCP Java.

## Docker opcional

Após compilar o servidor:

```sh
docker compose build
docker compose run --rm -T mcp-server
```

O comando inicia um servidor STDIO e aguarda um cliente MCP; não é um terminal de comandos de gastos e não expõe porta HTTP. O volume nomeado `gastobot-data` persiste `/data/gastobot.db`. Para outro cliente MCP, configure-o para iniciar `docker compose run --rm -T mcp-server` na raiz do projeto. Docker é opcional e separado do fluxo Maven/Java.

## Dependências

SDK MCP Java, Jackson para JSON, [Xerial SQLite JDBC](https://github.com/xerial/sqlite-jdbc) para persistência, SLF4J/Logback para logs e JUnit/Mockito para testes. As versões estão fixadas no `pom.xml`. A integração Telegram usa apenas o cliente HTTP do JDK.
