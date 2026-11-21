# Agente: Testes unitários

**Modelo padrão:** Sonnet (ver [models.md](models.md)).

## Por que é uma área

Todos os testes ficam em `app/src/test/java/app/totv/retro/` (38 casos em 4 arquivos, mais o helper `FakeServer.kt`). Eles rodam no JVM, sem
dispositivo e sem rede:
- `TotvApiTest.kt` (15 casos) e `FakeServer.kt`: o cliente da API contra um servidor falso, implementado como
  interceptor do OkHttp, sem sockets;
- `CoreSelectorTest.kt`: seleção de núcleo por plataforma e ABI;
- `RewindTest.kt`: buffer de rebobinar e padrão por plataforma;
- `MiscTest.kt`: cache LRU de ROM, nome de arquivo, mapeamento de controle (`PadMapper`) e analógico.

A área de testes existe para garantir que a convenção (sem rede real, sem sockets, teste com nome de regra) se
mantenha ao longo das outras áreas.

## Escopo (pode editar)

- `app/src/test/java/app/totv/retro/`, inteiro, com estas regras:
  - `FakeServer.kt` e `MiscTest.kt` são deste agente e não podem ser alterados por outras áreas;
  - os demais arquivos são editados pelo agente da área testada (emulação, integração, núcleos), e este agente
    só revisa se o agente da área pediu;
  - este agente pode criar arquivo de teste novo para uma regra que ainda não tem teste, desde que a regra esteja
    em produção e o agente dono da área concorde na entrega.

## Não pode tocar

- Código de produção em `app/src/main/`. Se um teste exige que a produção mude para ser possível (ex.: classe
  sem injeção), escalone para a área dona e não altere a produção.
- Dependências de teste em `app/build.gradle.kts` (junit, kotlinx-coroutines-test): são da área de build.
- Testes que dependam de rede, de `cores/` ou de servidor real.

## Critério de pronto

1. `./gradlew :app:testDebugUnitTest` passa, com o total de casos igual ou maior que antes (38 no estado atual).
2. Cada teste novo falha se a regra for quebrada. Demonstre isso com uma mudança temporária na produção que
   reverta a regra, e desfaça depois. Se não for possível, diga.
3. Nenhum teste novo abre socket, lê `cores/` ou depende do relógio do sistema sem controle.
4. Se o Gradle não rodar no ambiente (sem JDK 17 ou SDK), diga que a prova não foi feita. Não afirme que passou.

## Escalar para

- **Área dona da produção:** quando a regra testada não existe ou não é injetável.
- **Build:** se a configuração de teste (`testImplementation`, runner) precisar mudar.
- **Opus:** se os testes que tocam `android.*` (ex.: `PadMapper` usa `android.view.KeyEvent`) não rodarem no JVM,
  porque a correção pode exigir Robolectric. Isso é decisão de dependência.

## Modelo

Sonnet para escrever teste que prova comportamento, pois teste errado dá falsa segurança. Desça para Haiku para
uma tarefa só de execução: rodar `testDebugUnitTest` e resumir as falhas em lista, sem escrever código.
