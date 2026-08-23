#!/usr/bin/env python3
"""Gera o gráfico de destaque da ficha da Play Store (1024x500).

Saída:
  - play-store/feature-graphic.png

O desenho reaproveita a paleta e a geometria do ícone definidas em
generate_launcher_icon.py (mantenha os dois em sincronia ao mudar a marca).

Uso:
  pip install Pillow
  python3 tools/generate_feature_graphic.py

A fonte padrão é a DejaVu Sans (presente na maioria dos Linux). Em outro
sistema, aponte para um .ttf em negrito com a variável de ambiente FONT_BOLD.
"""

import os

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

W, H = 1024, 500

# ---- Paleta (espelha generate_launcher_icon.py) ----
BG_PURPLE = (101, 80, 164, 255)      # Purple40 (primary do tema M3)
BODY_WHITE = (255, 255, 255, 255)
SCREEN_DARK = (43, 32, 64, 255)      # display da calculadora
SCREEN_ACCENT = (208, 188, 255, 255) # Purple80, brilho dos dígitos
KEY_LIGHT = (240, 238, 245, 255)     # teclas numéricas
KEY_ORANGE = (255, 165, 0, 255)      # teclas de operador (mesma cor do app)

# ---- Geometria do glifo, no mesmo viewport 108x108 do adaptive icon ----
BODY = dict(x=24, y=22, w=60, h=64, r=10)
SCREEN = dict(x=31, y=29, w=46, h=13, r=3)

GRID_X, GRID_Y, GRID_W, GRID_H = 31, 48, 46, 31
COLS, ROWS, GAP = 3, 3, 3
KEY_W = (GRID_W - (COLS - 1) * GAP) / COLS
KEY_H = (GRID_H - (ROWS - 1) * GAP) / ROWS
KEY_R = 2.4


def key_rect(col, row):
    x = GRID_X + col * (KEY_W + GAP)
    y = GRID_Y + row * (KEY_H + GAP)
    return dict(x=x, y=y, w=KEY_W, h=KEY_H, r=KEY_R)


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


def draw_glyph(draw, offset_x, offset_y, scale):
    """Desenha a calculadora do ícone (sem o fundo) na posição/escala dadas."""

    def rrect(g, color):
        draw.rounded_rectangle(
            [
                offset_x + g["x"] * scale,
                offset_y + g["y"] * scale,
                offset_x + (g["x"] + g["w"]) * scale,
                offset_y + (g["y"] + g["h"]) * scale,
            ],
            radius=g["r"] * scale,
            fill=color,
        )

    rrect(BODY, BODY_WHITE)
    rrect(SCREEN, SCREEN_DARK)
    draw.rectangle(
        [
            offset_x + (SCREEN["x"] + 4) * scale,
            offset_y + (SCREEN["y"] + SCREEN["h"] - 5) * scale,
            offset_x + (SCREEN["x"] + 24) * scale,
            offset_y + (SCREEN["y"] + SCREEN["h"] - 3) * scale,
        ],
        fill=SCREEN_ACCENT,
    )
    for row in range(ROWS):
        for col in range(COLS):
            color = KEY_ORANGE if col == COLS - 1 else KEY_LIGHT
            rrect(key_rect(col, row), color)


im = Image.new("RGB", (W, H), BG_PURPLE[:3])
draw = ImageDraw.Draw(im)

# Glifo à esquerda: o corpo (24..86 de 108) fica centralizado na vertical
glyph_scale = 5.2
glyph_h = (BODY["h"]) * glyph_scale
glyph_x = 80 - BODY["x"] * glyph_scale
glyph_y = (H - glyph_h) / 2 - BODY["y"] * glyph_scale
draw_glyph(draw, glyph_x, glyph_y, glyph_scale)

# Nome e tagline à direita, centralizados como bloco no espaço restante.
# O tamanho da fonte é reduzido até o texto caber com folga nas bordas
# (zona segura do gráfico de destaque).
title = "CalcDroid"
tagline = "Científica · Gráficos · Matrizes"

glyph_right = glyph_x + (BODY["x"] + BODY["w"]) * glyph_scale
area_left = glyph_right + 50
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

draw.text((title_x, title_y), title, font=title_font, fill=BODY_WHITE[:3])
draw.text((tag_x, tag_y), tagline, font=tag_font, fill=SCREEN_ACCENT[:3])

# Filete laranja discreto, ecoando as teclas de operador
rule_y = block_top + title_h + spacing + tag_h + rule_gap
rule_w = 190
rule_x = area_left + (area_w - rule_w) / 2
draw.rounded_rectangle(
    [rule_x, rule_y, rule_x + rule_w, rule_y + rule_h],
    radius=rule_h / 2,
    fill=KEY_ORANGE[:3],
)

out_dir = os.path.join(REPO, "play-store")
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "feature-graphic.png")
im.save(out)
print(f"Wrote {out} ({W}x{H}).")
