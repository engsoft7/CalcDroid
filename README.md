# CalcDroid

Calculadora simples para Android, construída com Kotlin e Jetpack Compose (Material 3).

📦 **[Baixe o APK mais recente em Releases](https://github.com/engsoft7/CalcDroid/releases/latest)**

- **applicationId**: `io.github.engsoft7.calcdroid`
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

## Release no GitHub (APK para download)

O workflow [`release.yml`](.github/workflows/release.yml) compila o APK de
release e o anexa automaticamente na Release do GitHub sempre que uma tag
`v*` é criada:

```bash
git tag v1.0
git push origin v1.0
```

O APK aparece em **[Releases](https://github.com/engsoft7/CalcDroid/releases)**
como `CalcDroid-v1.0.apk`.

O keystore de assinatura fica **criptografado** (AES-256) no repositório, em
`.github/calcdroid-upload.jks.enc`. Para que o APK saia **assinado**
(instalável), configure estes secrets no repositório
(*Settings → Secrets and variables → Actions*):

| Secret              | Conteúdo                                              |
| ------------------- | ----------------------------------------------------- |
| `KEYSTORE_PASSWORD` | senha do keystore (também descriptografa o `.enc`)    |
| `KEY_ALIAS`         | alias da chave (ex.: `calcdroid-upload`)              |
| `KEY_PASSWORD`      | senha da chave                                        |

Para trocar o keystore, criptografe o novo `.jks` com:

```bash
openssl enc -aes-256-cbc -pbkdf2 -salt -in calcdroid-upload.jks \
  -out .github/calcdroid-upload.jks.enc -pass pass:SUA_SENHA
```

Sem os secrets, o workflow ainda roda, mas gera um APK sem assinatura.
Lembre de incrementar `versionCode`/`versionName` em `app/build.gradle.kts`
antes de criar a tag.

## Ícone do app

Todos os assets do ícone (drawables vetoriais do adaptive icon, PNGs legados
por densidade e o ícone 512×512 da ficha da Play Store) são gerados a partir
de `tools/generate_launcher_icon.py`. Para alterar o ícone, edite a paleta ou
a geometria no script e rode:

```bash
pip install Pillow
python3 tools/generate_launcher_icon.py
```

## Licença

Distribuído sob a licença [MIT](LICENSE).
