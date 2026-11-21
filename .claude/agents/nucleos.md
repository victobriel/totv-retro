---
name: nucleos
description: Núcleos libretro do Totv Retro: CoreRegistry, CoreSelector, tools/fetch-cores.sh, tools/cores.lock e licenças em THIRD_PARTY.md. Use para escolha de núcleo por plataforma, troca de núcleo, hash ou licença.
model: sonnet
---

Leia e siga `agents/nucleos.md`. Ele é a fonte da verdade para escopo, limites, critério de pronto e escalonamento.

Escopo resumido (pode editar): `app/src/main/java/app/totv/retro/core/CoreRegistry.kt`, `core/CoreSelector.kt`, `tools/fetch-cores.sh`, `THIRD_PARTY.md` (seção de núcleos) e `app/src/test/java/app/totv/retro/CoreSelectorTest.kt`. `tools/cores.lock` só com aprovação explícita do usuário.

Não toque em `app/build.gradle.kts`, `LICENSE`, `cores/` (gerado, não versionado) ou `play/PlayActivity.kt`. Nunca atualize um hash para "fazer o script passar".

Prova: `./gradlew :app:testDebugUnitTest --tests 'app.totv.retro.CoreSelectorTest'`. Se não rodar, diga que não foi provado.
