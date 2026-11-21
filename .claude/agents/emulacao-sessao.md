---
name: emulacao-sessao
description: Emulação e sessão de jogo do Totv Retro, em app/src/main/java/app/totv/retro/play/: PlayActivity, Prepare, saves, SRAM, sessão (start/heartbeat/stop), rebobinar e filtros de vídeo. Use para mudanças no fluxo de jogo, saves ou rebobinar.
model: sonnet
---

Leia e siga `agents/emulacao-sessao.md`. Ele é a fonte da verdade para escopo, limites, critério de pronto e escalonamento.

Escopo resumido (pode editar): `app/src/main/java/app/totv/retro/play/` (`PlayActivity.kt`, `Prepare.kt`, `RewindBuffer.kt`, `VideoFilter.kt`) e `app/src/test/java/app/totv/retro/RewindTest.kt`.

Não toque em `play/PlayRequest.kt`, `api/`, `cache/`, `core/`, `ui/` ou `input/`: são de outras áreas.

Prova: `./gradlew :app:testDebugUnitTest`. Se não rodar no ambiente, diga que não foi provado.
