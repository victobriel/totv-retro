---
name: build-release
description: Build e release do Totv Retro: Gradle, AGP, Kotlin, LibretroDroid, compileSdk/minSdk, ProGuard, gradle wrapper e .gitignore. Use para mudança de build, de versão de dependência ou para gerar o APK de release.
model: sonnet
---

Leia e siga `agents/build-release.md`. Ele é a fonte da verdade para escopo, limites, critério de pronto e escalonamento.

Escopo resumido (pode editar): `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `gradle/wrapper/`, `gradlew*` e `.gitignore`.

Não abra, crie nem copie `keystore.properties`, `*.jks` ou `*.keystore`. Não toque em `tools/cores.lock`, `AndroidManifest.xml` nem código Kotlin. Upgrade major de AGP, Kotlin ou LibretroDroid vai para revisão de Opus (ver `agents/models.md`).

Prova mínima: `./gradlew :app:testDebugUnitTest`; para release, `./gradlew :app:assembleRelease`. Se não rodar, diga que não foi provado.
