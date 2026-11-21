# Agente: Emulação e sessão de jogo

**Modelo padrão:** Sonnet (ver [models.md](models.md)).

## Por que é uma área

`app/src/main/java/app/totv/retro/play/` concentra o que acontece depois que o jogo abre:
- `PlayActivity.kt` (642 linhas): emulação via LibretroDroid, menu, saves, sessão (`start`/`heartbeat`/`stop`),
  miniatura e SRAM periódicos;
- `Prepare.kt` (181 linhas): prepara rede e disco antes de carregar (ROM, BIOS, estado, discos);
- `RewindBuffer.kt`: buffer de rebobinar com limite de 32 MB e ligado por padrão só em plataformas de 8/16 bits;
- `VideoFilter.kt`: enum dos filtros de vídeo (nítido, suave, CRT, LCD, pixel art) e o padrão por plataforma.

Essa área tem o maior risco de perda de progresso do jogador, por isso o escalonamento para Opus está na tabela.

## Escopo (pode editar)

- `app/src/main/java/app/totv/retro/play/PlayActivity.kt`
- `app/src/main/java/app/totv/retro/play/Prepare.kt`
- `app/src/main/java/app/totv/retro/play/RewindBuffer.kt`
- `app/src/main/java/app/totv/retro/play/VideoFilter.kt`
- `app/src/test/java/app/totv/retro/RewindTest.kt`

## Não pode tocar

- `play/PlayRequest.kt`: é o contrato da intent, pertence à integração.
- `api/`, `cache/`: cliente HTTP e cache, pertencem à integração. Chame a API pela interface existente; não
  altere `TotvApi` aqui.
- `core/`: seleção de núcleo, pertence aos núcleos.
- `ui/`, `input/`: `MenuOverlay` e `PadMapper` pertencem à interface. A `PlayActivity` pode chamar o menu, mas
  mudança visual no menu é da interface.
- Qualquer chave ou segredo.

## Critério de pronto

1. `./gradlew :app:testDebugUnitTest` passa (inclui `RewindTest` e o cache de ROM em `MiscTest`).
2. Mudança em saves, SRAM, estado ou rebobinar: demonstre por teste ou explique por que nenhum dado é perdido
   (ex.: a ordem de salvar antes de sair). Se for só teste de unidade, diga que não foi testado no aparelho.
3. Mudança no fluxo de jogo (sair, ir para a Home, sessão): diga que a prova em Fire TV não foi feita, se não
   foi. Essa prova é manual e este agente não a executa.

## Escalar para

- **Integração:** se precisar de campo novo da API, de mudança na intent ou de novo tipo de extra.
- **Núcleos:** se a troca de núcleo ou o estado de outro núcleo afetar a escolha.
- **Interface:** se a mudança for no menu (`MenuOverlay.kt`) ou em componente visual.
- **Opus:** se a mudança tocar a ordem de escrita de save/SRAM ou redesenhar o ciclo de vida da `PlayActivity`.

## Modelo

Sonnet. Implementação e refatoração dentro de uma classe grande com efeitos de ciclo de vida. Desça para Haiku só
para texto, log ou constante sem efeito em estado.
