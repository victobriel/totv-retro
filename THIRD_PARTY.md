# Componentes de terceiros

O Totv Retro é distribuído sob a **GPL-3.0-or-later** (veja `LICENSE`). Este arquivo lista tudo o que é
incorporado ao APK, de onde veio e como obter o código-fonte correspondente.

## Biblioteca de emulação

| Componente | Versão | Licença | Origem / fonte |
|---|---|---|---|
| LibretroDroid (Swordfish90) | **0.14.0** (tag `0.14.0`, commit `8835c3098514390a271e36983957f7bb5f40abf1`) | GPL-3.0 | https://github.com/Swordfish90/LibretroDroid (artefato `com.github.Swordfish90:LibretroDroid:0.14.0` via JitPack) |

A versão fica fixada em `app/build.gradle.kts`. Para atualizar, troque a versão, rode os testes e confira no
dispositivo (a API muda entre versões).

## Núcleos libretro embutidos no APK

Os núcleos são bibliotecas nativas (`lib<núcleo>.so`) carregadas em tempo de execução pelo LibretroDroid via
`dlopen`. Foram baixados do *buildbot* oficial do libretro (builds "nightly/latest" para Android) em
**2026-11-16**, inspecionados (ELF de 32 bits ARM EABI5 / ELF de 64 bits AArch64, um único arquivo por zip) e
fixados por SHA256 em `tools/cores.lock`; `tools/fetch-cores.sh` baixa e confere os hashes.

URL de cada arquivo: `https://buildbot.libretro.com/nightly/android/latest/<abi>/<núcleo>_libretro_android.so.zip`

Núcleos de licença não comercial foram deliberadamente **excluídos**.

| Núcleo | Plataforma | Licença | ABI | Data do build (zip) | SHA256 do .zip | SHA256 do .so |
|---|---|---|---|---|---|---|
| fceumm | NES / Famicom | GPL-2.0 | arm64-v8a | 2026-11-15 08:38 | `c5eff4d274ad6b2190014c559243f842bcbb0662f5c76335388cfb07646b7833` | `17d13e67597983d09fe5fca42491cdbde74557c22ea9d90437c2f3ca64e439b0` |
| fceumm | NES / Famicom | GPL-2.0 | armeabi-v7a | 2026-11-15 08:38 | `9f48f13314a426af16808a4dcd3fe3dbc1961853f81f7f55dd50d3f23b4f048b` | `42cbeb83d99e22e98d888cf837cf92dfdc51cbe85fe3163d59dc671298f01f86` |
| gambatte | Game Boy / Color | GPL-2.0 | arm64-v8a | 2026-11-15 05:33 | `c43f2fcd829aff665dee090a95c0383c0743f1c97b9cd4186f20c75924628f54` | `940085c7a138b0ff5161c56e1e92bdab06ba9d12d29430c9b406628ed2b8aa26` |
| gambatte | Game Boy / Color | GPL-2.0 | armeabi-v7a | 2026-11-15 05:33 | `68c4c18179e30f2ed90de9be79bc721d80d1477c62c88384dbcbb49c49b597f6` | `8c1babd7a734fa7ff85684928e2d57d20826cc2326aeb5b293382166f1148722` |
| gpsp | GBA (armeabi-v7a) | GPL-2.0 | armeabi-v7a | 2026-11-15 05:10 | `edcd0c27745428e4486aabcc747334243177f31575bb78b9e926f387ca3c84e4` | `1909ad5f2b053dba120d77fae58352f1ecdb066dfdcdd20d4f565bb1fd52d376` |
| mgba | GBA (arm64-v8a) | MPL-2.0 | arm64-v8a | 2026-11-16 04:22 | `df1dd7fe5eb28cdb071bddb45fe4f3f0127512440a41402a5e380007d693099e` | `b5a6ed40ede735ea7f1d56916f4ceed8c2aabe4d481fe3383ae2b5e5485b6545` |
| smsplus | Master System / Game Gear | GPL-2.0-or-later | arm64-v8a | 2026-11-15 05:17 | `a8acb3dfbb51b9b9f4868a9342cbdbdf925ecb34acedded5c20322a4fc458d42` | `c31b99329f2f52dfa4cc02ed4094d81195e64cff2e0a3c0f07b00c1b3d2d6f63` |
| smsplus | Master System / Game Gear | GPL-2.0-or-later | armeabi-v7a | 2026-11-15 05:17 | `2932d50f407569638f3c14d125e95a00c7a63a4554c52a627861a0c0592d3d83` | `30ec76e5f895982b555b6351235d116af6d2beb81bed416726d844cd7dc285e8` |
| mednafen_pce_fast | PC Engine / TurboGrafx-16 | GPL-2.0 | arm64-v8a | 2026-11-15 05:50 | `d3374d3e04caa4af21680c449817239ab99fdd59d52526e3a99613b17fe17c0e` | `57c3d7ea453a892c36c482cd5d3881d94da342708d214633d05e3fba0902f9e1` |
| mednafen_pce_fast | PC Engine / TurboGrafx-16 | GPL-2.0 | armeabi-v7a | 2026-11-15 05:50 | `0d214dc747811debed2d32808dcf48a695d826327823cbf1ac595d95d54e797c` | `bdfe0fee9b5698dd80b1e94255970f25a32146807a9a0d8bf582876b84b2f87d` |
| mednafen_supafaust | SNES (opcional) | GPL-2.0-or-later | arm64-v8a | 2026-11-15 05:48 | `0d218dc0cb1856fe2fa8812f9ba75aed660e4fc48196324bd0ee3dd00d2e0596` | `22f06ed62692c4170cfac1c09173e5866f92feabd21bee1b159f481dc37603cb` |
| mednafen_supafaust | SNES (opcional) | GPL-2.0-or-later | armeabi-v7a | 2026-11-15 05:47 | `72a26fd6e16bad5ca9afac4553fee4b5e7faf6532acbda008416609a16897305` | `b64c61f07ee1fd5b387576263c3813a8d3f331c92b433f61ce257f62a4b566c3` |
| clownmdemu | Mega Drive (opcional) | AGPL-3.0 | arm64-v8a | 2026-11-16 02:59 | `ee7027bd09fee7cad77b372c4fefab1021384f5ba3ff03f76cc47d157dc5ef27` | `53a03a845ce0dec1d20d7872d75e345253a0745cb191539d9dcfabd2b45eb9fb` |
| clownmdemu | Mega Drive (opcional) | AGPL-3.0 | armeabi-v7a | 2026-11-16 02:58 | `68539e1af0a385397ec9a69f4babe2b8c94da1ef151edf7eb7490c0b9f7119d8` | `d1e1e8f69920f623e097ae6646b4144956faaa6c5aed07599b957cda716142e7` |
| blastem | Mega Drive (opcional) | GPL-3.0 | arm64-v8a | 2026-11-16 04:27 | `bb9fbd7b0b262c3d47923507f7980c94fb47f2fed8ab4247d356108d0bd2f916` | `ee07b7646428fa9b9c4a8402e47cd1507234d2e93764906b8e428ee771447f8b` |
| blastem | Mega Drive (opcional) | GPL-3.0 | armeabi-v7a | 2026-11-16 04:18 | `d805391613b3570c665e483147880e55862f30ce9fc77360b2c5c6fccf1e32f6` | `007af4a7971891162eabbeaedd63290750c718330c4a7bf430d3c4f76799b543` |
As linhas de `mednafen_supafaust`, `clownmdemu` e `blastem` são **opcionais**: só entram no APK com a flag de build
descrita no README (`-PretroExtraCores=snes,md`) e `tools/fetch-cores.sh --extra`. Por padrão ficam de fora.

### Código-fonte correspondente dos núcleos

O buildbot compila o ramo principal de cada repositório na data indicada; ele não publica o hash do commit. O
código-fonte correspondente é o do ramo principal do repositório abaixo na data do build, e pode ser obtido com
`git clone <repo>` seguido de `git checkout $(git rev-list -1 --before="<data do build>" HEAD)`.

| Núcleo | Repositório | Arquivo de licença |
|---|---|---|
| fceumm | https://github.com/libretro/libretro-fceumm | `Copying` (GPL-2.0) |
| gambatte | https://github.com/libretro/gambatte-libretro | `COPYING` (GPL-2.0) |
| gpsp | https://github.com/libretro/gpsp | `COPYING` (GPL-2.0) |
| mgba | https://github.com/libretro/mgba | `LICENSE` (MPL-2.0) |
| smsplus (SMS Plus GX) | https://github.com/libretro/smsplus-gx | README: "GPLv2 or later" |
| mednafen_pce_fast (Beetle PCE Fast) | https://github.com/libretro/beetle-pce-fast-libretro | `COPYING` (GPL-2.0) |
| mednafen_supafaust (opcional) | https://github.com/libretro/supafaust | GPL-2.0+ |
| clownmdemu (opcional) | https://github.com/libretro/ClownMDEmu-libretro | AGPL-3.0 |
| blastem (opcional) | https://github.com/libretro/blastem | GPL-3.0 |

As licenças acima vêm do campo `license` dos arquivos `.info` do libretro-super
(https://github.com/libretro/libretro-super/tree/master/dist/info) e dos arquivos de licença dos repositórios.

**Nota de compatibilidade (a verificar pelo mantenedor):** vários núcleos são GPL-2.0 (alguns "somente 2.0"), o
aplicativo é GPL-3.0. Os núcleos são programas separados, carregados por uma interface binária (API libretro) e
distribuídos junto com o aplicativo como agregação; o texto das licenças acompanha o código-fonte. Se algum núcleo
for "GPL-2.0-only" e isso for um problema para o seu caso de uso, remova-o e use a alternativa compatível.

## Bibliotecas Java/Kotlin (dependências do Gradle)

| Biblioteca | Versão | Licença |
|---|---|---|
| OkHttp / Okio (Square) | 4.12.0 | Apache-2.0 |
| kotlinx.serialization-json | 1.7.3 | Apache-2.0 |
| kotlinx.coroutines | 1.9.0 | Apache-2.0 |
| Kotlin stdlib (JetBrains) | 2.0.21 | Apache-2.0 |
| AndroidX core-ktx / appcompat / lifecycle | 1.15.0 / 1.7.0 / 2.8.7 | Apache-2.0 |

(Apache-2.0 é compatível com a GPL-3.0.) Testes: JUnit 4.13.2 (EPL-1.0), não vai no APK.

## Marca e visual

O visual (cores, espaçamento, regras de foco) segue o design system do Totv e foi **reimplementado** com
constantes próprias em `app/src/main/java/app/totv/retro/ui/Tokens.kt`. Nenhum código do Totv é usado aqui; o Totv
e o Totv Retro só conversam pela API HTTP documentada (`docs/games-api.md` do servidor Totv) e por uma *intent*
explícita.
