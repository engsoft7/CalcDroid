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

Os testes unitários (avaliador de expressões, teclado, matrizes e escala do
gráfico) rodam sem aparelho e também no CI a cada push:

```bash
./gradlew testDebugUnitTest
```

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
3. Preencha a ficha da loja com o material de
   [`play-store/listing/pt-BR/`](play-store/listing/pt-BR/) (nome, descrições,
   ícone 512×512 e gráfico de destaque 1024×500) e pelo menos 2 capturas de
   tela — detalhes em [`play-store/store-listing.md`](play-store/store-listing.md).
   Depois disso a ficha pode ser mantida pelo GitHub (ver
   [Ficha da loja automática](#ficha-da-loja-automática)).
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

Depois do primeiro envio manual, as próximas versões podem ir para a Play
sozinhas — ver [Envio automático para a Play Store](#envio-automático-para-a-play-store).

> **Conta pessoal criada após nov/2023?** A Play exige primeiro um período de
> teste fechado com testadores por 14 dias antes de liberar a publicação em
> produção. Contas antigas ou de organização não passam por isso.

## Release no GitHub (APK para download)

O workflow [`release.yml`](.github/workflows/release.yml) compila o APK e o
AAB de release e os anexa automaticamente na Release do GitHub sempre que uma
tag `v*` é criada:

```bash
git tag v1.9
git push origin v1.9
```

O APK aparece em **[Releases](https://github.com/engsoft7/CalcDroid/releases)**
como `CalcDroid-v1.9.apk`.

Para que o APK e o AAB saiam **assinados** (instaláveis e aceitos pela Play),
configure estes secrets no repositório
(*Settings → Secrets and variables → Actions*):

| Secret              | Conteúdo                                                   |
| ------------------- | ---------------------------------------------------------- |
| `KEYSTORE_BASE64`   | o `.jks` inteiro em base64 (`base64 -w0 calcdroid-upload.jks`) |
| `KEYSTORE_PASSWORD` | senha do keystore                                          |
| `KEY_ALIAS`         | alias da chave (ex.: `calcdroid-upload`)                   |
| `KEY_PASSWORD`      | senha da chave                                             |

O workflow confere a impressão digital da chave antes de publicar: um AAB
assinado com outra chave seria recusado pela Play.

Sem `KEYSTORE_BASE64`, o workflow cai para o keystore **criptografado**
(AES-256) em `.github/calcdroid-upload.jks.enc`, usando `KEYSTORE_PASSWORD`
como senha. Como o repositório é público, prefira o secret. Para trocar o
`.enc`, criptografe o novo `.jks` com:

```bash
openssl enc -aes-256-cbc -pbkdf2 -salt -in calcdroid-upload.jks \
  -out .github/calcdroid-upload.jks.enc -pass pass:SUA_SENHA
```

Sem os secrets, o workflow ainda roda, mas gera um APK sem assinatura.
Lembre de incrementar `versionCode`/`versionName` em `app/build.gradle.kts`
antes de criar a tag.

## Envio automático para a Play Store

O mesmo workflow [`release.yml`](.github/workflows/release.yml) envia o AAB
assinado para a Play Store pela Google Play Developer API, usando a action
[`r0adkll/upload-google-play`](https://github.com/r0adkll/upload-google-play).
Sem a configuração abaixo ele só publica no GitHub, como antes.

### Configuração (uma única vez)

1. **Ative a API.** No [Google Cloud Console](https://console.cloud.google.com/),
   crie (ou escolha) um projeto e ative a
   [Google Play Android Developer API](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com).
2. **Crie uma conta de serviço** em *IAM e administrador → Contas de serviço →
   Criar conta de serviço*, **sem** papéis no Google Cloud. Abra a conta,
   vá em *Chaves → Adicionar chave → Criar nova chave → JSON* e guarde o
   arquivo baixado.
3. **Dê acesso no Play Console.** Em *Usuários e permissões → Convidar novos
   usuários*, informe o e-mail da conta de serviço
   (`...@...iam.gserviceaccount.com`), adicione o app **CalcDroid** em
   *Permissões do app* e marque *Lançar apps para faixas de teste* (e
   *Lançar para produção...* se quiser enviar direto para produção).
4. **Salve a chave no GitHub** como secret do repositório
   (*Settings → Secrets and variables → Actions → New repository secret*):

   | Secret                       | Conteúdo                                   |
   | ---------------------------- | ------------------------------------------ |
   | `PLAY_SERVICE_ACCOUNT_JSON`  | o conteúdo inteiro do arquivo `.json`      |

   Os secrets do keystore (`KEYSTORE_*`, seção anterior) também são
   obrigatórios: a Play só aceita o AAB assinado com a chave de upload.

A Play exige que o app já exista no Play Console e que o primeiro AAB tenha
sido enviado à mão — o que já foi feito.

### Publicar uma versão

1. Incremente `versionCode` e ajuste `versionName` em `app/build.gradle.kts`
   (a Play recusa um `versionCode` repetido).
2. Escreva as novidades em
   [`play-store/whatsnew/whatsnew-pt-BR`](play-store/whatsnew/whatsnew-pt-BR)
   (até 500 caracteres). O nome do arquivo segue o idioma da ficha da loja;
   se o idioma padrão no Play Console for outro, renomeie (ex.:
   `whatsnew-en-US`).
3. Faça commit e crie a tag:

   ```bash
   git tag v1.18
   git push origin v1.18
   ```

O AAB vai para a faixa **teste interno** (`internal`). Para mudar o padrão,
crie a variável de repositório `PLAY_TRACK` (*Settings → Secrets and
variables → Actions → Variables*) com `internal`, `alpha`, `beta`,
`production` ou `none` (não enviar). Na execução manual (*Actions → Release →
Run workflow*) a faixa é escolhida na hora.

Do teste interno, a versão é promovida para produção no próprio Play Console
(*Teste interno → Promover versão*), depois de conferir no aparelho.

### Problemas comuns

- **`The caller does not have permission`** — a conta de serviço não foi
  convidada no Play Console ou não tem permissão de lançamento no app; pode
  levar alguns minutos para valer depois do convite.
- **`Version code N has already been used`** — faltou incrementar o
  `versionCode`.
- **`Only releases with status draft may be created on draft app`** — o app
  ainda não foi publicado em nenhuma faixa; crie a variável
  `PLAY_RELEASE_STATUS` com o valor `draft` e conclua o lançamento no Play
  Console.
- **`Changes cannot be sent for review automatically`** — há alterações
  pendentes feitas no Play Console (ficha, classificação etc.); envie-as para
  revisão por lá e rode o workflow de novo.

## Ficha da loja automática

O workflow [`store-listing.yml`](.github/workflows/store-listing.yml) mantém a
ficha da Play (nome, descrições, ícone, gráfico de destaque e capturas) igual
aos arquivos de [`play-store/listing/`](play-store/listing/), usando o script
[`tools/play_store_listing.py`](tools/play_store_listing.py) e o mesmo secret
`PLAY_SERVICE_ACCOUNT_JSON` do envio do AAB.

**Permissão extra:** no Play Console, a conta de serviço precisa também de
**Gerenciar presença na loja** (*Usuários e permissões → conta de serviço →
Permissões do app*).

Só o que existe na pasta é enviado: um arquivo ausente deixa o campo como está
na Play, e uma pasta de capturas vazia não apaga as capturas da loja. A pasta
`pt-BR` precisa ter o código do idioma da ficha no Play Console; se não bater
com nenhum idioma de lá, o script para com um erro em vez de criar uma
tradução nova.

### Primeira vez (para não sobrescrever nada sem querer)

1. *Actions → Ficha da Play Store → Run workflow → **baixar***. Baixe o
   artefato `ficha-atual-da-play` e compare com `play-store/listing/`; se a
   ficha da Play tiver algo que você quer manter, copie para os arquivos.
2. Rode de novo com **validar**: a Play confere tudo, mas nada é publicado. O
   log mostra o que mudaria.
3. Rode com **publicar**.

### Depois

- Edite os arquivos em `play-store/listing/` e faça merge no `master`. O
  workflow valida a mudança na Play; para publicar, rode **publicar**.
- Para publicar sozinho a cada merge, crie a variável de repositório
  `PLAY_LISTING_AUTO_PUBLISH` com o valor `true`.
- Em pull requests o workflow só confere tamanhos de texto e formatos de
  imagem, sem falar com a Play. Dá para rodar o mesmo localmente:

  ```bash
  python3 tools/play_store_listing.py check
  ```

Mudanças na ficha podem passar pela revisão da Play antes de aparecer na loja.

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
- `play-store/listing/pt-BR/images/icon.png` — ícone 512×512 da ficha da loja.

O gráfico de destaque da loja (1024×500,
`play-store/listing/pt-BR/images/featureGraphic.png`) sai de
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
