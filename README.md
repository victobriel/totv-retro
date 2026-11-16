# Totv Retro

Emulador de consoles clássicos para **Android TV / Fire TV**, feito para ser aberto pelo app **Totv**.
Licença **GPL-3.0-or-later** (veja `LICENSE`). É um projeto independente: não contém nem importa código do Totv;
a comunicação é só pela API HTTP de jogos do servidor Totv e por uma *intent* explícita.

- Pacote: `app.totv.retro` · minSdk 23 · targetSdk 35 · ABIs `armeabi-v7a` e `arm64-v8a`
- Emulação: [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) **0.14.0** + núcleos libretro embutidos
- Plataformas: NES, Game Boy / Color, Game Boy Advance, Master System / Game Gear, PC Engine.
  SNES e Mega Drive ficam atrás de uma flag de build (veja abaixo).

Sem um jogo, o ícone do Totv Retro só mostra a tela "Abra um jogo pelo app Totv".

## Como o Totv abre um jogo

Intent explícita, pacote `app.totv.retro`, ação `app.totv.retro.action.PLAY`, extras:

| Extra | Tipo | |
|---|---|---|
| `server` | String | URL base do servidor Totv |
| `token` | String | token de jogador (escopo de um jogo, 6 h) |
| `itemId` | Long | item (jogo) |
| `mediaId` | Long, opcional | versão/dump (padrão: a primeira) |
| `stateId` | Long, opcional | estado para retomar |
| `title`, `platform`, `userName` | String | |

Ao fechar devolve `RESULT_OK` com o extra `playedMs` (Long). A API usada está descrita em `docs/games-api.md` do
Totv (plataformas, ROM/BIOS, saves, sessões).

O que o app faz: consulta plataformas e o item, baixa BIOS (se listadas) e a ROM (cache em `cache/roms`, limite de
1 GB, o mais antigo sai primeiro; `?extract=1` para zips), escolhe o núcleo da plataforma (preferência do servidor
em `cores.android`, filtrada pelo que está embutido; no GBA: gpSP no armv7 e mGBA no arm64), carrega a SRAM e o
estado (só se o núcleo do estado for o mesmo), registra a sessão (`start`, `heartbeat` a cada 30 s, `stop`),
salva estado automático + miniatura ao sair/ir para a Home e a cada 5 min, e envia a SRAM ao sair e a cada 60 s
se mudou.

## Controles

- **Gamepad:** direcional / analógico esquerdo, A/B/X/Y (A à direita, como no SNES), L/R, Start, Select.
  **Select + Start** abre o menu (o botão Guide/Mode também).
- **Controle remoto (Fire TV):** direcionais, OK = A, Play/Pause = Start, Menu = Select, **Voltar** = menu.
  Um gamepad é recomendado (o app mostra um aviso quando não há nenhum).
- **Menu (Voltar fecha a camada mais interna):** Continuar, Salvar estado (slots 1–9 com miniatura), Carregar
  estado, Reiniciar, Avanço rápido, Proporção (original/esticar), Filtro (nítido/suave), Controles, Sair.

## Compilar

Requisitos: JDK 17, Android SDK (plataforma 35), acesso à internet.

```bash
export JAVA_HOME=/caminho/do/jdk17 ANDROID_HOME=$HOME/Android/Sdk
tools/fetch-cores.sh            # baixa os núcleos (confere SHA256 de tools/cores.lock) para cores/base/
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease  # app/build/outputs/apk/release/app-release.apk
```

**Assinatura:** `keystore.properties` na raiz (ignorado pelo git) com `storeFile`, `storePassword`, `keyAlias`,
`keyPassword`. Sem ele, o release é assinado com a chave de debug. Crie uma chave própria com `keytool`.

**SNES e Mega Drive (desligados por padrão):**

```bash
tools/fetch-cores.sh --extra
./gradlew :app:assembleRelease -PretroExtraCores=snes,md    # ou "all"
```

Também vale um arquivo de decisão (`-PretroBenchDecision=/caminho`, linhas `snes=include` / `md=include`).
Em tempo de execução, o app usa o que estiver embutido; plataformas sem núcleo mostram um erro claro.

## Testes

`app/src/test`: cliente da API (um servidor falso em um *interceptor* do OkHttp, sem sockets), seleção de núcleo,
cache de ROMs, mapeamento de controles.

## Estrutura

```
app/src/main/java/app/totv/retro/
  api/    cliente HTTP da API de jogos (modelos + TotvApi)
  core/   seleção de núcleo (CoreSelector) e registro dos embutidos
  cache/  cache LRU de ROMs
  play/   PlayActivity (emulação, saves, sessão), preparação (rede/disco), parse da intent
  input/  gamepad/controle remoto -> joypad libretro
  ui/     tokens visuais próprios, componentes e menu de jogo
tools/    fetch-cores.sh + cores.lock (hashes dos núcleos)
```

Veja `THIRD_PARTY.md` para licenças, origem e hashes dos núcleos embutidos.
