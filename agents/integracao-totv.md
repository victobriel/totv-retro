# Agente: Integração com o servidor e o app Totv

**Modelo padrão:** Sonnet (ver [models.md](models.md)).

## Por que é uma área

O Totv Retro conversa com o mundo de fora por duas portas, e ambas são contrato com outro sistema:
- **Intent de entrada:** `app.totv.retro.action.PLAY`, com extras `server`, `token`, `itemId`, `mediaId`, `stateId`,
  `title`, `platform`, `userName`, e retorno `playedMs`. Declarada em `app/src/main/AndroidManifest.xml`, lida em
  `app/src/main/java/app/totv/retro/play/PlayRequest.kt`.
- **API HTTP do servidor Totv:** `app/src/main/java/app/totv/retro/api/TotvApi.kt` e `api/Models.kt`, com
  autenticação pelo token de jogador (`X-Totv-Token`).
- **Cache de ROM:** `app/src/main/java/app/totv/retro/cache/RomCache.kt`, LRU de 1 GB, usado pela preparação.

O contrato completo está no `docs/games-api.md` do servidor Totv, que **não está neste repositório**. As provas
desta área são locais (testes com `FakeServer`); a conformidade real com o servidor só se confirma contra ele.

## Escopo (pode editar)

- `app/src/main/java/app/totv/retro/api/`
- `app/src/main/java/app/totv/retro/cache/`
- `app/src/main/java/app/totv/retro/play/PlayRequest.kt`
- `app/src/main/AndroidManifest.xml`: somente activity e intent-filter de `PLAY`, e permissões de rede
- A tabela "Como o Totv abre um jogo" do `README.md`, sempre junto com qualquer mudança de extra
- `app/src/test/java/app/totv/retro/TotvApiTest.kt`

## Não pode tocar

- `play/PlayActivity.kt` e `Prepare.kt`: consomem a API, mas são da emulação. Peça a mudança de sessão à emulação.
- Chave, senha ou token real em qualquer arquivo. Os testes usam token falso.
- Outras partes do `README.md`: são da documentação.
- `app/src/test/java/app/totv/retro/FakeServer.kt`: compartilhado; mudanças passam pelo agente de testes.
- Dependências em `app/build.gradle.kts`: são da área de build.

## Critério de pronto

1. `./gradlew :app:testDebugUnitTest --tests 'app.totv.retro.TotvApiTest'` passa, e `RomCache` (em `MiscTest`) segue
   passando se `cache/` mudou.
2. Se algum extra da intent ou rota da API mudou: a tabela do `README.md` e o `PlayRequest.kt` dizem a mesma coisa,
   e o aviso de mudança de contrato aparece no resumo do handoff.
3. Erro de rede e de autenticação continuam como `ApiException` com código (401, 403, 404 e 0 para falha de
   transporte), como usado por `TotvApiTest`.
4. Afirmar conformidade com `docs/games-api.md` só se o arquivo tiver sido lido; se não foi, diga que não foi
   verificado.

## Escalar para

- **Opus:** qualquer mudança de contrato (extra, rota, formato de resposta), uso ou exposição do token, ou decisão
  sobre dado do jogador. Contrato público é a decisão mais difícil de reverter.
- **Emulação:** se a preparação (`Prepare.kt`) precisar de nova chamada ou mudança de fluxo.
- **Build:** se for preciso mudar permissões de rede que afetam o APK ou o `minSdk`.
- **Documentação:** se o README tiver outro trecho que deixou de bater com o código.

## Modelo

Sonnet. O cliente e a intent têm muitos casos de borda e um contrato externo. Suba para Opus em mudança de contrato.
Desça para Haiku só para ajuste de nome de campo já definido, com teste existente cobrindo.
