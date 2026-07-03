# CalcDroid

Calculadora simples para Android, construída com Kotlin e Jetpack Compose (Material 3).

- **applicationId**: `com.mayconlimasan.calcdroid`
- **minSdk**: 24 · **targetSdk / compileSdk**: 35

## Build de desenvolvimento

```bash
./gradlew assembleDebug
```

O APK de debug fica em `app/build/outputs/apk/debug/`.

## Release para a Play Store

### 1. Configurar a assinatura (uma única vez)

O build de release é assinado com um keystore de upload que **não** fica no
repositório. Coloque estes dois arquivos na raiz do projeto:

1. `calcdroid-upload.jks` — o keystore de upload.
2. `keystore.properties` — as credenciais (use `keystore.properties.example`
   como modelo).

Se ainda não tiver um keystore, gere um com:

```bash
keytool -genkeypair -v -keystore calcdroid-upload.jks -keyalg RSA \
  -keysize 2048 -validity 10000 -alias calcdroid-upload
```

Guarde o keystore e as senhas em local seguro (gerenciador de senhas). Com o
Play App Signing ativado (padrão para apps novos), essa chave é apenas a chave
de upload — se ela for perdida, dá para solicitar a troca no Play Console, mas
é melhor não precisar.

Sem `keystore.properties`, o build de release ainda funciona, porém gera um
artefato **não assinado** (útil para CI).

### 2. Gerar o Android App Bundle

```bash
./gradlew bundleRelease
```

O AAB assinado fica em `app/build/outputs/bundle/release/app-release.aab`.
É esse arquivo que se envia no Play Console.

### 3. Subir na Play Store

1. No [Play Console](https://play.google.com/console), crie o app (nome:
   **CalcDroid**) e aceite o Play App Signing.
2. Envie o `app-release.aab` em *Produção* (ou *Teste interno* primeiro).
3. Na ficha da loja, use `play-store/ic_launcher-playstore.png` (512×512) como
   ícone do app.
4. A cada nova versão, incremente `versionCode` (e ajuste `versionName`) em
   `app/build.gradle.kts` antes de gerar o AAB.
