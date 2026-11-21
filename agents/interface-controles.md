# Agente: Interface TV e controles

**Modelo padrão:** Sonnet, com Haiku para recurso e texto (ver [models.md](models.md)).

## Por que é uma área

Tudo que o jogador vê fora do jogo e tudo que chega do controle:
- `app/src/main/java/app/totv/retro/ui/`: `Tokens.kt` (cores, espaçamento de 8 pt, canvas de 960×540 dp),
  `Ui.kt` (componentes sobre `View`s), `MenuOverlay.kt` (menu do jogo), `Texts.kt` (textos da UI);
- `app/src/main/java/app/totv/retro/input/PadMapper.kt`: converte evento de tecla e analógico em joypad libretro.
  O mapeamento de A/B/X/Y é a parte mais fácil de errar: o Android e o RetroPad trocam A e B;
- `app/src/main/java/app/totv/retro/InfoActivity.kt`: tela "Abra um jogo pelo app Totv";
- `app/src/main/res/`: `strings.xml`, `themes.xml`, `drawable/` (banner e ícone).

O app não usa Compose; a interface é feita com `View`s e uma paleta própria, por decisão documentada em `Tokens.kt`.

## Escopo (pode editar)

- `app/src/main/java/app/totv/retro/ui/`
- `app/src/main/java/app/totv/retro/input/`
- `app/src/main/java/app/totv/retro/InfoActivity.kt`
- `app/src/main/res/`

## Não pode tocar

- `play/PlayActivity.kt`: é a emulação. O menu é chamado de lá; a mudança de fluxo (o que acontece ao escolher
  "Sair", por exemplo) é da emulação, a mudança de aparência é desta área.
- `app/src/test/`: testes de `PadMapper` estão em `MiscTest.kt`, que é do agente de testes.
- Paleta de cor fora de `Tokens.kt`: não crie cor literal em componente novo.
- Nomes de string usados pela documentação sem alinhar com `README.md`.

## Critério de pronto

1. `./gradlew :app:assembleDebug` compila sem erro novo.
2. `./gradlew :app:testDebugUnitTest` continua passando (inclui `padMapperSwapsAbxyAndMapsRemote` e
   `analogToDpadWithDeadzone`).
3. Mudança de layout ou de controle: diga que a verificação foi em TV ou emulador, ou que não foi feita. Este
   agente não roda o app; a prova visual é manual.
4. Mudança em `PadMapper`: o teste de A/B/X/Y ainda descreve o comportamento pretendido (A à direita, como SNES).

## Escalar para

- **Emulação:** se a mudança de menu exigir novo estado ou novo comando em `PlayActivity`.
- **Testes:** se precisar de teste novo em `MiscTest.kt` ou `FakeServer.kt`.
- **Documentação:** se os controles descritos no `README.md` mudarem (a seção "Controles").
- **Opus:** se a mudança de navegação do menu alterar como o jogador sai do jogo, ou se o mapeamento de controle
  mudar para uma convenção diferente da atual (é decisão de produto).

## Modelo

Sonnet para componente e layout, pois o efeito só aparece na TV. Desça para Haiku para `strings.xml`, recurso
gráfico ou valor de token que já existe. Não desça para Haiku quando a tarefa mexer em foco ou navegação do controle.
