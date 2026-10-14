# CÉU — Fase 1

Núcleo Kotlin testável e interface Android simulada. Esta fase **não executa ações reais** do sistema.

## O que entra

- Entrada só por texto (`VoiceInput` = fila de texto)
- Resposta só na tela (`VoiceOutput` = transcrição na interface)
- Máquina de estados explícita: transição fora da lista é rejeitada e registrada
- Interpretador determinístico (sem fuzzy match, locale `ROOT`)
- Confirmação vinculada à ação: token de 128 bits, SHA-256 da forma canônica `v1|NOME`, validade de 15 s no relógio monotônico, uso único, invalidação ao cancelar ou trocar/reemitir a ação
- `DenyAllAuthorizer` nega toda ação (`DENIED_BY_DEFAULT`)
- Auditoria só em memória, sem o texto digitado
- Tela Jetpack Compose com rótulo, botões Enviar / Confirmar / Cancelar, região ao vivo `Polite` e título com semântica de heading

## O que fica de fora

Voz real, microfone, TTS, `AccessibilityService`, abrir Configurações, voltar de verdade, rede, Room, DataStore, arquivos, Keystore e qualquer executor real.

Mesmo o caminho interno em que um autorizador de teste devolveria `Allowed` apenas escreve que a ação foi **simulada**. O app de produção usa somente `DenyAllAuthorizer`.

## Módulos

| Módulo | Tipo |
| --- | --- |
| `:core` | JVM puro, sem Android |
| `:app` | Android, Jetpack Compose |

## Versões

JDK 17, Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21, Compose Compiler 2.0.21, Compose BOM 2024.12.01, minSdk 31, compileSdk/targetSdk 35, Coroutines 1.9.0, JUnit Jupiter 5.11.3. Catálogo em `gradle/libs.versions.toml`.

## Comandos

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew :core:test
./gradlew :app:assembleDebug
```

O wrapper desta árvore já foi gerado para o Gradle 8.11.1. `assembleDebug` precisa de Android SDK 35 (`ANDROID_HOME`). O manifesto remove `INTERNET` e `ACCESS_NETWORK_STATE` e define `android:allowBackup="false"`.

## Comandos da gramática

`ajuda`, `status`, `eco <texto>`, `simular voltar`, `simular inicio`, `simular configuracoes`, `simular notificacoes`, `simular recentes`.

Acentos e maiúsculas são normalizados. Qualquer outra frase é rejeitada. `eco` mostra o texto na tela e **não** o copia para a auditoria.

## Limitações

Fase 1 é um núcleo fechado de laboratório. Confirmar uma ação só registra a negação. Nada é aberto, nada é persistido, nada sai do aparelho.
