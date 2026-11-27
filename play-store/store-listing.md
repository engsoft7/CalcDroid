# Ficha da Play Store

Os textos e as imagens da ficha ficam em [`listing/pt-BR/`](listing/pt-BR/)
(português) e [`listing/en-US/`](listing/en-US/) (inglês, criada como tradução
na primeira publicação) e
são enviados para a Play pelo workflow *Ficha da Play Store* (ver README,
seção "Ficha da loja automática"). Para mudar a ficha, edite esses arquivos.

| Campo                              | Arquivo                                        | Limite / formato          |
| ---------------------------------- | ---------------------------------------------- | ------------------------- |
| Nome do app                        | `listing/pt-BR/title.txt`                      | até 30 caracteres         |
| Descrição curta                    | `listing/pt-BR/short_description.txt`          | até 80 caracteres         |
| Descrição completa                 | `listing/pt-BR/full_description.txt`           | até 4000 caracteres       |
| Ícone                              | `listing/pt-BR/images/icon.png`                | PNG 512×512               |
| Gráfico de destaque                | `listing/pt-BR/images/featureGraphic.png`      | 1024×500, sem transparência |
| Capturas de tela                   | `listing/pt-BR/images/phoneScreenshots/*.png`  | 2 a 8, ver abaixo         |

Na descrição completa, cada quebra de linha aparece na loja: escreva cada
parágrafo numa linha só, com uma linha em branco entre eles.

O ícone e o gráfico de destaque são gerados por
`python3 tools/generate_launcher_icon.py` e
`python3 tools/generate_feature_graphic.py`.

### Capturas de tela

As capturas em `listing/*/images/phoneScreenshots/` (1080×2160) foram
renderizadas a partir do próprio app, em cada idioma. São enviadas em ordem alfabética e substituem as que estão na
loja quando a ficha é publicada. Para trocar, salve novas imagens ali (lado
menor ≥ 320 px, lado maior no máximo o dobro do menor, sem transparência).

## Demais campos do Play Console

- **Categoria**: Ferramentas
- **Tags**: calculadora, científica, gráficos, matrizes
- **Política de privacidade**: `https://engsoft7.github.io/CalcDroid/privacy-policy.html`
  (requer o GitHub Pages ativado — ver README)
- **Segurança dos dados**: declarar que o app **não coleta nem compartilha
  nenhum dado** e não usa serviços de terceiros (é a verdade: sem permissões
  e sem internet).
- **Classificação de conteúdo (IARC)**: questionário de utilitário, sem
  conteúdo sensível — resulta em classificação livre.
- **Público-alvo**: 13+ (evita as exigências extras da política de famílias).
