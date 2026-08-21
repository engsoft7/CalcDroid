# CalcDroid

Calculadora simples para Android, construída com Kotlin e Jetpack Compose (Material 3).

📦 **[Baixe o APK mais recente em Releases](https://github.com/engsoft7/CalcDroid/releases/latest)**

- **applicationId**: `io.github.engsoft7.calcdroid`
- **minSdk**: 24 · **targetSdk / compileSdk**: 37

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
3. Preencha a ficha da loja com o material pronto em
   [`play-store/store-listing.md`](play-store/store-listing.md) (nome,
   descrições, categoria) e os assets:
   - Ícone 512×512: `play-store/ic_launcher-playstore.png`
   - Gráfico de destaque 1024×500: `play-store/feature-graphic.png`
   - Capturas de tela: pelo menos 2, capturadas de um aparelho ou emulador
     (sugestões no mesmo arquivo).
4. Informe a URL da política de privacidade (obrigatória para todo app):
   `https://engsoft7.github.io/CalcDroid/privacy-policy.html`. A página fica
   em [`docs/privacy-policy.html`](docs/privacy-policy.html); para publicá-la,
   ative o GitHub Pages em *Settings → Pages → Deploy from a branch*,
   escolhendo o branch `master` e a pasta `/docs`.
5. Preencha *Segurança dos dados* declarando que o app não coleta nem
   compartilha nenhum dado (não pede permissões e não acessa a internet) e
   responda o questionário de classificação de conteúdo.
6. A cada nova versão, incremente `versionCode` (e ajuste `versionName`) em
   `app/build.gradle.kts` antes de gerar o AAB.

> **Conta pessoal criada após nov/2023?** A Play exige primeiro um período de
> teste fechado com testadores por 14 dias antes de liberar a publicação em
> produção. Contas antigas ou de organização não passam por isso.

## Release no GitHub (APK para download)

O workflow [`release.yml`](.github/workflows/release.yml) compila o APK de
release e o anexa automaticamente na Release do GitHub sempre que uma tag
`v*` é criada:

```bash
git tag v1.9
git push origin v1.9
```

O APK aparece em **[Releases](https://github.com/engsoft7/CalcDroid/releases)**
como `CalcDroid-v1.9.apk`.

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

## Ícone do app e artes da loja

A arte de origem é `tools/icon_glyph.png`: só a calculadora, em RGBA com
fundo transparente. O degradê laranja não fica embutido nesse PNG — ele é
desenhado pelos scripts, para que o adaptive icon use as duas camadas
separadas, como o Android espera.

`tools/generate_launcher_icon.py` gera, a partir dela:

- `drawable/ic_launcher_background.xml` — vetor com o degradê do fundo;
- `drawable/ic_launcher_monochrome.xml` — vetor do ícone temático (Android 13+);
- `mipmap-*/ic_launcher_foreground.png` — camada de frente, com o glifo
  dimensionado para caber na zona segura de 66dp do canvas de 108dp (fora
  dela o launcher recorta o desenho);
- `mipmap-*/ic_launcher{,_round}.png` — ícones legados (pré-API 26), já
  recortados em quadrado arredondado e em círculo;
- `play-store/ic_launcher-playstore.png` — ícone 512×512 da ficha da loja.

O gráfico de destaque da loja (1024×500) sai de
`tools/generate_feature_graphic.py`, que reaproveita o mesmo glifo e o mesmo
degradê. Para alterar as artes, troque `tools/icon_glyph.png` ou ajuste a
paleta/geometria nos scripts e rode:

```bash
pip install Pillow
python3 tools/generate_launcher_icon.py
python3 tools/generate_feature_graphic.py
```

## Licença

Distribuído sob a licença [MIT](LICENSE).
