# Sistema de agentes — Totv Retro

Definição e regras para dividir o trabalho neste repositório entre agentes especializados. **Não há runtime
aqui**: cada arquivo descreve um agente (escopo, limites, critério de pronto, escalonamento e modelo). Quem
orquestra (o coordenador) lê esta pasta, roteia a tarefa pela área dona dos caminhos afetados e passa o arquivo
do agente como instrução.

Não há CI, `AGENTS.md` nem `CLAUDE.md` no repositório; este sistema não substitui nenhum deles.

## Áreas e agentes

| Área | Agente | Modelo padrão | Caminhos principais |
|---|---|---|---|
| Emulação e sessão de jogo | [emulacao-sessao.md](emulacao-sessao.md) | Sonnet | `app/src/main/java/app/totv/retro/play/` (exceto `PlayRequest.kt`) |
| Núcleos libretro | [nucleos.md](nucleos.md) | Sonnet | `app/src/main/java/app/totv/retro/core/`, `tools/` |
| Integração com o servidor Totv | [integracao-totv.md](integracao-totv.md) | Sonnet | `app/src/main/java/app/totv/retro/api/`, `cache/`, `play/PlayRequest.kt`, intent em `AndroidManifest.xml` |
| Interface TV e controles | [interface-controles.md](interface-controles.md) | Sonnet | `app/src/main/java/app/totv/retro/ui/`, `input/`, `InfoActivity.kt`, `app/src/main/res/` |
| Testes unitários | [testes.md](testes.md) | Sonnet | `app/src/test/` |
| Build e release | [build-release.md](build-release.md) | Sonnet | `build.gradle.kts`, `settings.gradle.kts`, `gradle*`, `gradlew*`, `app/build.gradle.kts`, `app/proguard-rules.pro` |
| Documentação | [docs.md](docs.md) | Haiku | `README.md`, `THIRD_PARTY.md` |
| Orquestração (ponto de entrada, não edita código) | [orquestrador.md](orquestrador.md) | Sonnet | `agents/` (só leitura para roteamento) |

O **orquestrador** é o ponto de entrada: recebe o pedido, roteia pela tabela acima, despacha os agentes das áreas
afetadas e consolida o resultado.

A justificativa de cada área (arquivos que a mostram) está em cada arquivo de agente. Os modelos e a regra de
roteamento estão em [models.md](models.md).

## Regras que valem para todos os agentes

1. **Edite só o seu escopo.** Se a mudança precisa tocar outra área, pare essa parte e escalone (tabela abaixo).
   Teste de uma área é editado pelo agente dessa área: `RewindTest.kt` (emulação), `TotvApiTest.kt`
   (integração), `CoreSelectorTest.kt` (núcleos). `MiscTest.kt` mistura áreas (`RomCache`, `PadMapper`) e
   `FakeServer.kt` é compartilhado: quem edita esses dois é o agente de testes.
2. **Não faça push, não abra PR, não crie tag nem release** sem pedido explícito do usuário.
3. **Segredos ficam fora do versionado.** Não abra, copie nem gere `keystore.properties`, `*.jks` ou
   `*.keystore`. A chave de release fica fora do repositório.
4. **Versões e hashes fixados não mudam sozinhos.** Isso vale para `tools/cores.lock`, a versão do LibretroDroid
   em `app/build.gradle.kts` e o `gradle-wrapper.properties`. Mudança nesses pontos exige aprovação explícita
   do usuário e vai na mesma alteração que `THIRD_PARTY.md`.
5. **Prova antes de "pronto".** Rode o comando de prova do seu agente. Se não der para rodar (por exemplo, sem JDK 17
   ou Android SDK no ambiente), diga isso e verifique por leitura, sem afirmar que passou.
6. **Entrega:** arquivos alterados, o que foi provado (com o comando e o resultado), o que não foi provado e o
   que ficou para outra área.

## Escalonamento entre áreas

| Quando | Para quem | Motivo |
|---|---|---|
| Mudança de extra da intent `app.totv.retro.action.PLAY` ou de payload da API | Integração com o servidor Totv | É contrato público com o app Totv; `README.md` precisa acompanhar |
| Novo núcleo, troca de núcleo por plataforma, ou núcleo com licença diferente de GPL | Núcleos libretro (+ Opus para revisar licença) | Muda o APK e a conformidade de licença |
| Mudança em `saves`, SRAM, estados ou rebobinar | Emulação e sessão (+ Opus para revisar risco de perda de dados) | Erro aqui apaga progresso do jogador |
| Nova cor, espaçamento ou componente visual | Interface TV e controles | Os tokens são de `ui/Tokens.kt` |
| Teste precisa de classe de produção que não existe ou não é injetável | Área dona da classe (o agente de testes não altera produção) | Escopo |
| Versão de AGP, Kotlin, LibretroDroid ou compileSdk/minSdk | Build e release (+ Opus para revisar) | Afeta o APK inteiro |
| Comportamento descrito no README que não bate com o código | Documentação pergunta à área dona do código | Doc não decide comportamento |

## Lacunas conhecidas

- **Sem CI no repositório.** Não há `.github/workflows`. As provas são manuais, pelos comandos de cada agente.
- **`docs/games-api.md` não está no repositório.** README e código citam esse arquivo do servidor Totv. O contrato
  da API e da intent não pode ser verificado só aqui; a integração depende de um arquivo externo.
- **Sem lint/format próprio.** O único lint é o do AGP, com `abortOnError = false` (`app/build.gradle.kts`).
- **`cores/` não está versionado.** É gerado por `tools/fetch-cores.sh` (exige internet). Não verifiquei se os
  testes unitários precisam dele.
- **Testes que tocam `android.*`** (ex.: `MiscTest.padMapperSwapsAbxyAndMapsRemote`, `PadMapper` usa
  `android.view.KeyEvent`) rodam no JVM. Não verifiquei se passam sem Robolectric ou stubs.
