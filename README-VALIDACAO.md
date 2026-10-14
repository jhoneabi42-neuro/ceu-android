# Validação — CÉU Fase 1

## GERADO

- Data: 2026-10-14
- Escopo: Fase 1 fechada (texto, simulação, máquina de estados, confirmação de 15 s, negação por padrão, auditoria em memória, Compose)
- Módulos: `:core` (JVM) e `:app` (Android)
- Fora do escopo, de propósito: voz real, TTS, AccessibilityService, ações reais, rede, persistência, Keystore, executores reais

## COMPILADO

- `:core` compilado ao rodar `./gradlew :core:test` com Gradle 8.11.1 e JDK 17. Resultado: sucesso.
- `:app:assembleDebug` executado com Android SDK 35 (`compileSdk`/`targetSdk` 35, `minSdk` 31). Resultado: `BUILD SUCCESSFUL`.
- APK de depuração: `app/build/outputs/apk/debug/app-debug.apk`
- Assinatura: chave de debug do Android Gradle Plugin, não é pacote de loja.

## TESTADO

- Comando: `./gradlew :core:test`
- 21 testes, 0 falhas:
  - `StateMachineTest` (4)
  - `InterpreterTest` (3)
  - `ConfirmationManagerTest` (6)
  - `DenyAllAuthorizerTest` (1)
  - `AuditAgentTest` (1)
  - `CeuOrchestratorTest` (6)
- Coberto: transição ilegal registrada, token de 128 bits, SHA-256 de `v1|BACK`, validade exata de 15 s, uso único, cancelamento, troca de ação, negação por padrão, auditoria sem o texto do usuário, simulação que não executa ação real.
- Não há testes instrumentados (`androidTest`) nesta fase.
- Reexecução forçada nesta máquina em 2026-10-14, comando `./gradlew --no-daemon --max-workers=2 :core:cleanTest :core:test --rerun-tasks`. Saída: 21 PASSED, `BUILD SUCCESSFUL in 24s`, exit 0. O log também contém ruído de daemon (`Unexpected type tag 71`); o resultado dos testes não depende dele.

## VALIDADO EM DISPOSITIVO

- NÃO EXECUTADO.
- Nenhum aparelho ou emulador instalou o APK.
- A tela Compose não foi exercitada em dispositivo. A semântica (heading, rótulo, `liveRegion = Polite`) está no código, sem verificação de TalkBack.
