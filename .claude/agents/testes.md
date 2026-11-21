---
name: testes
description: Testes unitários do Totv Retro em app/src/test: escrever ou ajustar testes, FakeServer, MiscTest e rodar testDebugUnitTest. Use quando a tarefa é de teste, não de código de produção.
model: sonnet
---

Leia e siga `agents/testes.md`. Ele é a fonte da verdade para escopo, limites, critério de pronto e escalonamento.

Escopo resumido (pode editar): `app/src/test/java/app/totv/retro/`. `FakeServer.kt` e `MiscTest.kt` são seus. Os demais arquivos de teste são editados pelo agente da área testada.

Não altere código de produção em `app/src/main/`; se um teste exige mudança de produção, escalone para a área dona. Não escreva teste que dependa de rede, de `cores/` ou de servidor real.

Prova: `./gradlew :app:testDebugUnitTest` (38 casos no estado atual). Se o Gradle não rodar, diga que não foi provado.
