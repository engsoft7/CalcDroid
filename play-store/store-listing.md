# Ficha da Play Store — textos e assets

Material pronto para copiar e colar no Play Console
(*Presença na loja → Ficha principal da loja*).

## Nome do app (até 30 caracteres)

```
CalcDroid
```

## Descrição curta (até 80 caracteres)

```
Calculadora científica com gráficos de funções e matrizes. Grátis e offline.
```

## Descrição completa (até 4000 caracteres)

```
O CalcDroid é uma calculadora completa, leve e sem anúncios. Tudo funciona
offline: o app não pede nenhuma permissão e não acessa a internet.

QUATRO MODOS EM UM SÓ APP

• Básica — as quatro operações e porcentagem, com display que mostra a
expressão inteira enquanto você digita.

• Científica — seno, cosseno e tangente (em graus), logaritmos (log e ln),
raiz quadrada, potências, fatorial e as constantes π e e. Escreva expressões
completas, como 2sin(30) + √16^2, com multiplicação implícita e parênteses.

• Gráfico — digite f(x) e veja a curva na hora, com eixos, grade e ajuste
automático da escala vertical. Ótimo para estudar funções.

• Matrizes — soma, subtração e multiplicação de matrizes 2×2 e 3×3, além de
determinante, inversa e transposta.

FEITO PARA O DIA A DIA

• Interface limpa em Material Design, com botões grandes e resposta imediata.
• O estado é preservado ao girar a tela: nada se perde no meio da conta.
• Continue calculando em cima do resultado anterior, como numa calculadora
de verdade.
• App pequeno, rápido e de código aberto (licença MIT).

Sem cadastro, sem anúncios, sem coleta de dados. Só uma boa calculadora.
```

## Assets gráficos

| Asset                        | Arquivo                                | Como gerar                                  |
| ---------------------------- | -------------------------------------- | ------------------------------------------- |
| Ícone do app (512×512)       | `play-store/ic_launcher-playstore.png` | `python3 tools/generate_launcher_icon.py`   |
| Gráfico de destaque (1024×500) | `play-store/feature-graphic.png`     | `python3 tools/generate_feature_graphic.py` |
| Capturas de tela (mín. 2)    | *(pendente)*                           | Ver abaixo                                  |

### Capturas de tela

A Play exige pelo menos 2 capturas de telefone (proporção 16:9 a 9:16, lado
menor ≥ 320 px; o ideal é capturar direto do aparelho ou emulador, ex.:
1080×2400). Sugestão de conjunto, uma por modo:

1. Modo básico com uma conta no display (ex.: `1234+567`).
2. Modo científico com uma expressão completa (ex.: `2sin(30)+√(16)^2`).
3. Modo gráfico plotando `sin(x)`.
4. Modo matrizes exibindo o resultado de `A×B` (3×3).

Salve como `play-store/screenshots/01-basica.png` etc.

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
