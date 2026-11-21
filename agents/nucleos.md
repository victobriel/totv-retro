# Agente: Núcleos libretro

**Modelo padrão:** Sonnet (ver [models.md](models.md)).

## Por que é uma área

Os núcleos são bibliotecas nativas que rodam o jogo, e a escolha de qual núcleo usar por plataforma é uma regra
de negócio com testes próprios:
- `app/src/main/java/app/totv/retro/core/CoreRegistry.kt`: lista os núcleos embutidos no APK e a ABI do aparelho (`abi()`);
- `app/src/main/java/app/totv/retro/core/CoreSelector.kt`: preferência do servidor, fallback embutido, núcleo do
  estado e regra específica de GBA (gpSP no armv7, mGBA no arm64);
- `tools/fetch-cores.sh` e `tools/cores.lock`: baixam os `.so` do buildbot do libretro e conferem SHA256. Este é o
  ponto de supply chain do projeto: o `.so` executa código nativo no aparelho.

Os núcleos entram no APK por `app/build.gradle.kts` (`jniLibs.srcDir`), mas essa parte é da área de build.

## Escopo (pode editar)

- `app/src/main/java/app/totv/retro/core/CoreRegistry.kt`
- `app/src/main/java/app/totv/retro/core/CoreSelector.kt`
- `tools/fetch-cores.sh`
- `tools/cores.lock`, somente com aprovação explícita do usuário (regra 4 do README)
- `THIRD_PARTY.md`: seção de núcleos e linhas da tabela, sempre junto com `tools/cores.lock`
- `app/src/test/java/app/totv/retro/CoreSelectorTest.kt`

## Não pode tocar

- `app/build.gradle.kts`: `jniLibs.srcDir` e a versão do LibretroDroid são da área de build.
- `LICENSE`.
- `cores/`: é gerado, não versionado. Não faça `git add` nele.
- `play/PlayActivity.kt`: é a emulação; aqui a seleção só é usada, não alterada.
- Hash ou versão em `cores.lock` alterado sem aprovação: se o hash de um núcleo mudar no buildbot, pare e
  escalone (ver abaixo). Nunca atualize o hash para "fazer o script passar".

## Critério de pronto

1. `./gradlew :app:testDebugUnitTest --tests 'app.totv.retro.CoreSelectorTest'` passa.
2. Se `CoreRegistry` ou `CoreSelector` mudou: cada plataforma listada no README (NES, GB/GBC, GBA, MS/GG, PCE,
   SNES, MD, PS1) continua com núcleo, ou a remoção está explicada.
3. Se `cores.lock` mudou: cada linha tem um par `abi core` em `tools/fetch-cores.sh` e o mesmo hash aparece no
   `THIRD_PARTY.md`. Rode `tools/fetch-cores.sh` quando houver internet; sem internet, diga que o download não foi
   feito e confira só a consistência entre os três arquivos.
4. Núcleo novo ou com licença diferente de GPL: não entra sem revisão do Opus e aprovação do usuário.

## Escalar para

- **Opus:** núcleo novo, troca do núcleo principal de uma plataforma, licença não GPL (ex.: AGPL do clownmdemu, que
  fica fora do APK por padrão), ou mudança na política de hash.
- **Build e release:** `app/build.gradle.kts`, o `srcDir` de `cores/` e a flag `-PretroExtraCores`.
- **Documentação:** se a lista de plataformas no README mudar.
- **Emulação:** se a seleção exigir mudança na forma como a sessão carrega o núcleo.

## Modelo

Sonnet. Regra de seleção com casos de borda e cobertura de testes; erro aqui escolhe o núcleo errado para um jogo.
Desça para Haiku só para a checagem de consistência entre `cores.lock` e `THIRD_PARTY.md`, sem editar.
