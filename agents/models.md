# Modelos por agente

Escolha o modelo mais barato que dá conta da tarefa. Custo sobe de Haiku para Sonnet e de Sonnet para Opus; sobe
o modelo só quando a tarefa pede julgamento que o modelo menor não entrega.

## Tabela

| Área | Agente | Modelo padrão | Motivo da escolha | Sobe para Opus quando | Desce para Haiku quando |
|---|---|---|---|---|---|
| Emulação e sessão | emulacao-sessao | Sonnet | `PlayActivity.kt` tem 642 linhas e mistura ciclo de vida, saves, sessão e rebobinar. Exige raciocínio sobre estado e corrida, mas é implementação dentro de uma área | Mudança em saves/SRAM/estado que possa apagar progresso; redesenho do ciclo de vida da `PlayActivity` | Ajuste de texto, log ou constante sem efeito em estado |
| Núcleos | nucleos | Sonnet | `CoreSelector.kt` tem regras de preferência e fallback por plataforma e ABI, cobertas por `CoreSelectorTest`. Trocar núcleo é implementação com regra clara | Novo núcleo, troca de núcleo principal, licença diferente de GPL, ou política de hash | Só conferir se `cores.lock` e a tabela do `THIRD_PARTY.md` batem (leitura e comparação) |
| Integração Totv | integracao-totv | Sonnet | Cliente HTTP, parse de JSON e intent com muitos casos de borda (`TotvApiTest` tem 15 casos). Precisa seguir um contrato que não está no repositório | Mudança de contrato (extra da intent, rota da API, uso do token) ou decisão sobre dado de jogador | Ajuste de modelo JSON ou nome de campo já definido, com teste existente |
| Interface e controles | interface-controles | Sonnet | Componentes próprios sobre `View`s clássicas, sem Compose (`Ui.kt`, `MenuOverlay.kt`), e mapeamento de controle. Ajuste de layout exige checar comportamento na TV | Mudança de navegação do menu que afete o fluxo de jogo | Strings, recursos (`strings.xml`), ajuste de token de cor ou espaçamento |
| Testes | testes | Sonnet | Escrever teste que prova comportamento exige entender a regra testada e o `FakeServer`. Erro aqui dá falsa segurança | Teste que exige mudar produção para ser possível (decisão de desenho) | Rodar `testDebugUnitTest` e resumir falhas em lista; nenhuma escrita de teste |
| Build e release | build-release | Sonnet | Mudança em Gradle, AGP e ProGuard exige ler erro de configuração e decidir a correção. Erro quebra o APK inteiro | Upgrade major de AGP, Kotlin ou LibretroDroid; qualquer coisa de assinatura ou regra de release | Consultar versões, ler saída de lint ou de `dependencies` e relatar |
| Documentação | docs | Haiku | Texto com risco baixo e reversível. O trabalho é editar prosa e alinhar números com o código (versões, extensões, plataformas), o que um modelo pequeno faz bem | Reescrita de estrutura que exija decidir o que o produto promete | Não rebaixa; se a tarefa é auditar se o README bate com o código, sobe para Sonnet |
| Orquestração | orquestrador | Sonnet | Roteia e decide ordem, paralelismo e escalonamento de agentes, e julga se a saída de um worker fecha a prova. Essas escolhas são julgamento sobre o caso, não consulta à tabela: um erro de roteamento custa uma execução inteira de outro agente. Haiku ficaria só na consulta, e a decisão é justamente o que Sonnet cobre | Nunca por padrão. Opus entra como revisor da pergunta específica (gatilho de contrato, saves, licença ou upgrade major), não para coordenar | Não desce: o orquestrador não é o caminho de tarefa de um arquivo; com uma área só, o pedido pode ir direto ao dono |

## Por que não Opus em área nenhuma

Opus não é dono de área. Ele entra como **revisor de decisão** quando um agente com escopo fechado encontra uma
pergunta de arquitetura, risco de dados ou política de licença/supply chain. Assim a chamada cara acontece uma
vez, sobre uma pergunta bem delimitada, e não em toda tarefa de rotina.

## Regra de roteamento

1. **Comece no modelo padrão da área.**
2. **Desça para Haiku** quando a tarefa for mecânica: edição de texto, recurso, leitura de saída, comparação de
   hash ou versão, busca de arquivo. Pergunta de confirmação: "dá para fazer sem decidir nada?" Se sim, Haiku.
3. **Suba para Opus** quando qualquer item abaixo for verdadeiro:
   - a mudança cruza duas áreas ou altera um contrato público (intent `app.totv.retro.action.PLAY`, forma da API);
   - a mudança pode apagar progresso (saves, SRAM, estados) ou expor o token do jogador;
   - a mudança altera supply chain ou licença (núcleo novo, hash, versão de dependência nativa, GPL/AGPL);
   - é upgrade major de AGP, Kotlin ou LibretroDroid.
4. **Suba de Haiku para Sonnet** quando a tarefa exigir julgamento sobre comportamento (a mudança depende de
   entender o código), mesmo que pareça pequena.
5. **Escalone de Sonnet para Opus** depois de duas tentativas que não fecham a prova da área, ou quando a
   prova falha por motivo que o agente não entende. Antes de subir, registre o que foi tentado e a saída da prova.
6. **Não suba por precaução.** Tarefa de um arquivo, sem efeito em estado, dados ou contrato, fica no padrão.

## Como o coordenador usa esta tabela

- Ao rotear, leia o agente da área e use o modelo padrão.
- Se o agente retornar com "escalonar" ou com a prova falhando duas vezes, faça a revisão com Opus sobre a
  pergunta específica, não sobre a tarefa inteira.
- Registre no handoff o modelo usado e o motivo, para que a próxima tarefa parecida use a mesma escolha.
