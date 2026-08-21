#!/usr/bin/env python3
"""Gera o gráfico de destaque da ficha da Play Store (1024x500).

Saída:
  - play-store/feature-graphic.png

Reaproveita a arte do ícone: o mesmo degradê laranja e o mesmo glifo
(tools/icon_glyph.png) usados por generate_launcher_icon.py. Ao mudar a
marca, altere lá e rode os dois scripts.

Uso:
  pip install Pillow
  python3 tools/generate_feature_graphic.py

A fonte padrão é a DejaVu Sans (presente na maioria dos Linux). Em outro
sistema, aponte para um .ttf em negrito com a variável de ambiente FONT_BOLD.
"""

import os

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GLYPH = os.path.join(REPO, "tools/icon_glyph.png")

W, H = 1024, 500

# ---- Paleta (espelha generate_launcher_icon.py) ----
BG_START = (255, 179, 0)     # #FFB300
BG_END = (255, 144, 0)       # #FF9000
TEXT_WHITE = (255, 255, 255)
TEXT_SOFT = (255, 243, 224)  # tagline, um branco quente sobre o laranja


def load_font(size):
    candidates = [
        os.environ.get("FONT_BOLD"),
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf",
        "/Library/Fonts/Arial Bold.ttf",
        "C:/Windows/Fonts/arialbd.ttf",
    ]
    for path in candidates:
        if path and os.path.exists(path):
            return ImageFont.truetype(path, size)
    raise SystemExit(
        "Nenhuma fonte em negrito encontrada; defina FONT_BOLD=/caminho/fonte.ttf"
    )


# ---- Fundo: degradê diagonal, igual ao do ícone ----
im = Image.new("RGB", (W, H))
px = im.load()
for y in range(H):
    for x in range(W):
        t = (x / (W - 1) + y / (H - 1)) / 2
        px[x, y] = tuple(round(BG_START[c] + (BG_END[c] - BG_START[c]) * t)
                         for c in range(3))
draw = ImageDraw.Draw(im)

# ---- Glifo à esquerda, centralizado na vertical ----
glyph = Image.open(GLYPH).convert("RGBA")
glyph_h = round(H * 0.66)
glyph_w = round(glyph.width * glyph_h / glyph.height)
glyph = glyph.resize((glyph_w, glyph_h), Image.LANCZOS)
glyph_x, glyph_y = 96, (H - glyph_h) // 2
im.paste(glyph, (glyph_x, glyph_y), glyph)

# ---- Nome e tagline à direita, centralizados como bloco no espaço restante ----
# O tamanho da fonte é reduzido até o texto caber com folga nas bordas
# (zona segura do gráfico de destaque).
title = "CalcDroid"
tagline = "Científica · Gráficos · Matrizes"

area_left = glyph_x + glyph_w + 60
area_right = W - 50
area_w = area_right - area_left

title_size = 104
while title_size > 40:
    title_font = load_font(title_size)
    tb = draw.textbbox((0, 0), title, font=title_font)
    if tb[2] - tb[0] <= area_w:
        break
    title_size -= 2

tag_size = 34
while tag_size > 16:
    tag_font = load_font(tag_size)
    gb = draw.textbbox((0, 0), tagline, font=tag_font)
    if gb[2] - gb[0] <= area_w:
        break
    tag_size -= 1

title_w, title_h = tb[2] - tb[0], tb[3] - tb[1]
tag_w, tag_h = gb[2] - gb[0], gb[3] - gb[1]
spacing = 26
rule_h, rule_gap = 8, 30
block_h = title_h + spacing + tag_h + rule_gap + rule_h
block_top = (H - block_h) / 2

title_x = area_left + (area_w - title_w) / 2 - tb[0]
tag_x = area_left + (area_w - tag_w) / 2 - gb[0]
title_y = block_top - tb[1]
tag_y = block_top + title_h + spacing - gb[1]

draw.text((title_x, title_y), title, font=title_font, fill=TEXT_WHITE)
draw.text((tag_x, tag_y), tagline, font=tag_font, fill=TEXT_SOFT)

# Filete branco discreto, fechando o bloco de texto
rule_y = block_top + title_h + spacing + tag_h + rule_gap
rule_w = 190
rule_x = area_left + (area_w - rule_w) / 2
draw.rounded_rectangle(
    [rule_x, rule_y, rule_x + rule_w, rule_y + rule_h],
    radius=rule_h / 2,
    fill=TEXT_WHITE,
)

out_dir = os.path.join(REPO, "play-store")
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "feature-graphic.png")
im.save(out)
print(f"Wrote {out} ({W}x{H}).")
