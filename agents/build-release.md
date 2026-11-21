# Agente: Build e release

**Modelo padrão:** Sonnet (ver [models.md](models.md)).

## Por que é uma área

O APK sai de uma configuração Gradle pequena, mas com pontos sensíveis:
- `settings.gradle.kts` e `build.gradle.kts`: versões de plugin (AGP 8.7.3, Kotlin 2.0.21), repositórios (o
  LibretroDroid só existe no JitPack, e o `settings` proíbe repositório no projeto);
- `app/build.gradle.kts`: `compileSdk`/`targetSdk` 35, `minSdk` 23, ABIs `armeabi-v7a` e `arm64-v8a`, cores em
  `jniLibs.srcDir`, flag `-PretroExtraCores` (clownmdemu), assinatura de release lida de `keystore.properties`;
- `app/proguard-rules.pro`: mantém as classes do LibretroDroid, que o código nativo acessa por nome;
- `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `gradle.properties`: Gradle 8.11.1 com
  `validateDistributionUrl=true`;
- `.gitignore`: esconde `keystore.properties`, `*.jks`, `cores/` e `local.properties`.

Não há CI. Quem roda o build hoje é uma pessoa, com JDK 17, Android SDK 35 e internet.

## Escopo (pode editar)

- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `gradle/wrapper/`, `gradlew`, `gradlew.bat`
- `.gitignore`

## Não pode tocar

- `keystore.properties`, `*.jks`, `*.keystore` e qualquer senha de assinatura: não abra, não crie, não copie. O
  release usa a chave de debug quando o arquivo falta, e isso deve continuar assim.
- `tools/cores.lock` e os hashes: são da área de núcleos.
- `app/src/main/AndroidManifest.xml`: é da integração.
- `THIRD_PARTY.md`: edite só as linhas de versão de dependência que você mudou, junto com a mudança (regra 4).
- Código Kotlin de produção e de teste.

## Critério de pronto

1. `./gradlew :app:testDebugUnitTest` passa. Este é o mínimo para qualquer mudança aqui.
2. `./gradlew :app:assembleRelease` gera `app/build/outputs/apk/release/app-release.apk` sem erro. Sem `cores/`,
   o resultado não foi verificado: diga isso se não tiver sido gerado.
3. Mudança de versão (AGP, Kotlin, LibretroDroid, `compileSdk`, `minSdk`): confirme a versão nos dois lugares
   (`build.gradle.kts` e `THIRD_PARTY.md`), e diga que a verificação em aparelho não foi feita, se não foi.
4. Mudança de ProGuard: rode o release e diga que o APK não foi testado em execução; regra de keep errada só aparece
   em tempo de execução.

## Escalar para

- **Opus:** upgrade major de AGP, Kotlin ou LibretroDroid; qualquer mudança de assinatura ou de regra de release;
  mudança de `minSdk` (exclui aparelhos).
- **Núcleos:** se `cores/` ou `-PretroExtraCores` mudar o comportamento dos núcleos embutidos.
- **Integração:** se a permissão ou feature do manifest mudar o contrato com o app Totv.
- **Testes:** se a configuração de teste precisar de runner ou dependência nova.

## Modelo

Sonnet. Um erro de configuração quebra o APK inteiro e exige ler a saída do Gradle para decidir a correção.
Desça para Haiku para consultar versões, ler saída de lint ou de `dependencies` e relatar, sem editar.
