#!/usr/bin/env python3
"""Gera todos os assets do ícone do CalcDroid a partir de uma única definição.

Saídas:
  - app/src/main/res/drawable/ic_launcher_background.xml  (vetor, fundo)
  - app/src/main/res/drawable/ic_launcher_foreground.xml  (vetor, calculadora)
  - app/src/main/res/mipmap-*/ic_launcher{,_round}.png    (fallback legado)
  - play-store/ic_launcher-playstore.png                  (512x512, ficha da loja)

Uso:
  pip install Pillow
  python3 tools/generate_launcher_icon.py
"""

import os

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "app/src/main/res")

# ---- Paleta (segue o tema Material do app + cores dos botões da calculadora) ----
BG_PURPLE = (101, 80, 164, 255)      # Purple40 (primary do tema M3)
BODY_WHITE = (255, 255, 255, 255)
SCREEN_DARK = (43, 32, 64, 255)      # display da calculadora
SCREEN_ACCENT = (208, 188, 255, 255) # Purple80, brilho dos dígitos
KEY_LIGHT = (240, 238, 245, 255)     # teclas numéricas
KEY_ORANGE = (255, 165, 0, 255)      # teclas de operador (mesma cor do app)

# ---- Geometria, no viewport 108x108 do adaptive icon ----
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


# ---------------------------------------------------------------------------
# pathData de vetor Android para um retângulo de cantos arredondados
# ---------------------------------------------------------------------------
def n(v):
    v = round(v, 2)
    return str(int(v)) if v == int(v) else str(v)


def rounded_rect_path(x, y, w, h, r):
    return (
        f"M{n(x + r)},{n(y)} "
        f"H{n(x + w - r)} "
        f"A{n(r)},{n(r)} 0 0 1 {n(x + w)},{n(y + r)} "
        f"V{n(y + h - r)} "
        f"A{n(r)},{n(r)} 0 0 1 {n(x + w - r)},{n(y + h)} "
        f"H{n(x + r)} "
        f"A{n(r)},{n(r)} 0 0 1 {n(x)},{n(y + h - r)} "
        f"V{n(y + r)} "
        f"A{n(r)},{n(r)} 0 0 1 {n(x + r)},{n(y)} "
        f"Z"
    )


def fmt(c):
    r, g, b, a = c
    return f"#{a:02X}{r:02X}{g:02X}{b:02X}"


def vector_header(w=108, h=108):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{w}dp"\n'
        f'    android:height="{h}dp"\n'
        f'    android:viewportWidth="{w}"\n'
        f'    android:viewportHeight="{h}">\n'
    )


def path_tag(d, color, indent="    "):
    return f'{indent}<path\n{indent}    android:fillColor="{fmt(color)}"\n{indent}    android:pathData="{d}" />\n'


# ---------------------------------------------------------------------------
# Drawable de fundo: cor sólida roxa da marca
# ---------------------------------------------------------------------------
bg_xml = vector_header()
bg_xml += path_tag("M0,0h108v108h-108z", BG_PURPLE)
bg_xml += "</vector>\n"

with open(os.path.join(RES, "drawable/ic_launcher_background.xml"), "w") as f:
    f.write(bg_xml)

# ---------------------------------------------------------------------------
# Drawable de frente: glifo de calculadora (corpo + display + grade de teclas)
# ---------------------------------------------------------------------------
fg_xml = vector_header()
fg_xml += path_tag(rounded_rect_path(**BODY), BODY_WHITE)
fg_xml += path_tag(rounded_rect_path(**SCREEN), SCREEN_DARK)
fg_xml += path_tag(
    f"M{SCREEN['x'] + 4},{SCREEN['y'] + SCREEN['h'] - 4}h20",
    SCREEN_ACCENT,
)

for row in range(ROWS):
    for col in range(COLS):
        color = KEY_ORANGE if col == COLS - 1 else KEY_LIGHT
        fg_xml += path_tag(rounded_rect_path(**key_rect(col, row)), color)

fg_xml += "</vector>\n"

with open(os.path.join(RES, "drawable/ic_launcher_foreground.xml"), "w") as f:
    f.write(fg_xml)

print("Wrote vector background/foreground drawables.")


# ---------------------------------------------------------------------------
# Renderização raster (mipmaps legados + ícone da ficha da Play Store)
# ---------------------------------------------------------------------------
def draw_rounded_rect(draw, x, y, w, h, r, color, scale):
    draw.rounded_rectangle(
        [x * scale, y * scale, (x + w) * scale, (y + h) * scale],
        radius=r * scale,
        fill=color,
    )


def render_icon(size, legacy_mask=None):
    """Renderiza o design 108x108 em `size`x`size` px.
    legacy_mask: None (quadrado cheio, Play Store), 'square' (ícone legado
    quadrado arredondado) ou 'round' (ícone legado circular).
    """
    scale = size / 108.0
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    draw.rectangle([0, 0, size, size], fill=BG_PURPLE)
    draw_rounded_rect(draw, **BODY, color=BODY_WHITE, scale=scale)
    draw_rounded_rect(draw, **SCREEN, color=SCREEN_DARK, scale=scale)
    draw.rectangle(
        [
            (SCREEN["x"] + 4) * scale,
            (SCREEN["y"] + SCREEN["h"] - 5) * scale,
            (SCREEN["x"] + 24) * scale,
            (SCREEN["y"] + SCREEN["h"] - 3) * scale,
        ],
        fill=SCREEN_ACCENT,
    )
    for row in range(ROWS):
        for col in range(COLS):
            color = KEY_ORANGE if col == COLS - 1 else KEY_LIGHT
            draw_rounded_rect(draw, **key_rect(col, row), color=color, scale=scale)

    if legacy_mask is None:
        return im

    mask = Image.new("L", (size, size), 0)
    mdraw = ImageDraw.Draw(mask)
    if legacy_mask == "round":
        mdraw.ellipse([0, 0, size, size], fill=255)
    else:
        mdraw.rounded_rectangle([0, 0, size, size], radius=size * 0.19, fill=255)

    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out


DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

for folder, size in DENSITIES.items():
    d = os.path.join(RES, folder)
    os.makedirs(d, exist_ok=True)
    render_icon(size, legacy_mask="square").save(os.path.join(d, "ic_launcher.png"))
    render_icon(size, legacy_mask="round").save(os.path.join(d, "ic_launcher_round.png"))

print("Wrote legacy mipmap PNGs for all densities.")

# ---------------------------------------------------------------------------
# Ícone da ficha da Play Store: 512x512, quadrado cheio, sem transparência
# ---------------------------------------------------------------------------
store_dir = os.path.join(REPO, "play-store")
os.makedirs(store_dir, exist_ok=True)
store_icon = render_icon(512, legacy_mask=None).convert("RGB")
store_icon.save(os.path.join(store_dir, "ic_launcher-playstore.png"))
print("Wrote Play Store 512x512 listing icon.")
