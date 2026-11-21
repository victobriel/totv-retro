---
name: integracao-totv
description: Integração com o servidor e o app Totv: intent app.totv.retro.action.PLAY (PlayRequest.kt), cliente HTTP api/TotvApi, cache de ROM e AndroidManifest. Use para mudança de extra da intent, de rota ou payload da API, ou de autenticação.
model: sonnet
---

Leia e siga `agents/integracao-totv.md`. Ele é a fonte da verdade para escopo, limites, critério de pronto e escalonamento.

Escopo resumido (pode editar): `app/src/main/java/app/totv/retro/api/`, `cache/`, `play/PlayRequest.kt`, activity e intent-filter de `PLAY` em `app/src/main/AndroidManifest.xml`, a tabela "Como o Totv abre um jogo" do `README.md` e `app/src/test/java/app/totv/retro/TotvApiTest.kt`.

Mudança de contrato (extra da intent ou API) é decisão do usuário: registre no resumo. Não toque em `play/PlayActivity.kt`, `Prepare.kt`, `FakeServer.kt` ou no `docs/games-api.md`, que não está no repositório.

Prova: `./gradlew :app:testDebugUnitTest --tests 'app.totv.retro.TotvApiTest'`. Se não rodar, diga que não foi provado.
