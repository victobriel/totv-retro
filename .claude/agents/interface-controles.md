---
name: interface-controles
description: Interface TV e controles do Totv Retro: ui/ (Tokens, Ui, MenuOverlay, Texts), input/PadMapper, InfoActivity e res/. Use para tela, menu, cor, layout, strings ou mapeamento de controle.
model: sonnet
---

Leia e siga `agents/interface-controles.md`. Ele é a fonte da verdade para escopo, limites, critério de pronto e escalonamento.

Escopo resumido (pode editar): `app/src/main/java/app/totv/retro/ui/`, `input/`, `InfoActivity.kt` e `app/src/main/res/`.

Não toque em `play/PlayActivity.kt` (fluxo de jogo é da emulação) nem em `app/src/test/` (testes de `PadMapper` estão em `MiscTest.kt`, do agente de testes). Cor nova vai em `ui/Tokens.kt`.

Prova: `./gradlew :app:assembleDebug` e `./gradlew :app:testDebugUnitTest`. Verificação visual em TV não é feita por você; diga isso.
