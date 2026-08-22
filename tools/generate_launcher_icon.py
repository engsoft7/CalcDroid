#!/usr/bin/env python3
"""Gera todos os assets do ícone do CalcDroid a partir de uma única fonte.

Fonte: tools/icon_glyph.png — a calculadora recortada (RGBA, fundo
transparente). O degradê laranja é desenhado aqui, e não embutido no PNG,
para que o adaptive icon use as duas camadas como o Android espera.

Saídas:
  - app/src/main/res/drawable/ic_launcher_background.xml  (vetor, degradê)
  - app/src/main/res/drawable/ic_launcher_monochrome.xml  (vetor, ícone temático)
  - app/src/main/res/mipmap-*/ic_launcher_foreground.png  (camada de frente)
  - app/src/main/res/mipmap-*/ic_launcher.png             (legado, quadrado)
  - app/src/main/res/mipmap-*/ic_launcher_round.png       (legado, redondo)
  - play-store/ic_launcher-playstore.png                  (512x512, ficha da loja)

Uso:
  pip install Pillow
  python3 tools/generate_launcher_icon.py
"""

import os

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "app/src/main/res")
GLYPH = os.path.join(REPO, "tools/icon_glyph.png")

# ---- Degradê do fundo (medido da arte original: diagonal 45°) ----
BG_START = (255, 179, 0)   # #FFB300, canto superior esquerdo
BG_END = (255, 144, 0)     # #FF9000, canto inferior direito

# ---- Geometria da calculadora, medida da arte no espaço 108x108 ----
# Usada só para o vetor monocromático; as camadas coloridas vêm do PNG.
BODY_W, BODY_H = 67.2, 77.2
BODY_R = 7.6
DISPLAY = dict(x=6.8, y=6.8, w=53.8, h=18.2, r=3.0)     # relativo ao corpo
KEY_W = KEY_H = 10.8
KEY_R = 2.1
KEY_COLS = (6.8, 21.0, 35.6, 49.8)                      # relativo ao corpo
KEY_ROWS = (29.6, 43.0, 56.6)

# Altura do glifo dentro do canvas 108dp do adaptive icon. O raio máximo do
# glifo é 48.45dp na arte original; em 54dp de altura ele cai para 33.7dp, ou
# seja, dentro da zona segura de 66dp (raio 33) que toda máscara de launcher
# preserva — com folga de 2.3dp até o círculo de 36dp, que absorve o parallax.
FOREGROUND_GLYPH_H = 54.0

# Ícones legados: o desenho ocupa a maior parte do bitmap, como manda a
# convenção pré-API 26 (o launcher não aplica máscara nenhuma).
LEGACY_GLYPH_FRAC = 0.62
LEGACY_CORNER_FRAC = 0.20

# Ficha da Play Store: arte cheia, na proporção original.
STORE_GLYPH_FRAC = 0.715

DENSITIES = {           # pasta: (canvas 108dp do adaptive, bitmap legado)
    "mipmap-mdpi": (108, 48),
    "mipmap-hdpi": (162, 72),
    "mipmap-xhdpi": (216, 96),
    "mipmap-xxhdpi": (324, 144),
    "mipmap-xxxhdpi": (432, 192),
}


def gradient(size):
    """Degradê linear na diagonal, igual ao medido na arte original."""
    im = Image.new("RGB", (size, size))
    px = im.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * (size - 1)) if size > 1 else 0.0
            px[x, y] = tuple(
                round(BG_START[c] + (BG_END[c] - BG_START[c]) * t) for c in range(3)
            )
    return im


def scaled_glyph(height_px):
    g = Image.open(GLYPH).convert("RGBA")
    w = max(1, round(g.width * height_px / g.height))
    return g.resize((w, max(1, round(height_px))), Image.LANCZOS)


def centered(canvas, glyph):
    canvas.paste(glyph, ((canvas.width - glyph.width) // 2,
                         (canvas.height - glyph.height) // 2), glyph)
    return canvas


def masked(im, shape):
    size = im.width
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    if shape == "round":
        d.ellipse([0, 0, size - 1, size - 1], fill=255)
    else:
        d.rounded_rectangle([0, 0, size - 1, size - 1],
                            radius=size * LEGACY_CORNER_FRAC, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(im.convert("RGBA"), (0, 0), mask)
    return out


# ---------------------------------------------------------------------------
# Vetores
# ---------------------------------------------------------------------------
def n(v):
    v = round(v, 2)
    return str(int(v)) if v == int(v) else str(v)


def rounded_rect_path(x, y, w, h, r):
    return (
        f"M{n(x + r)},{n(y)} H{n(x + w - r)} "
        f"A{n(r)},{n(r)} 0 0 1 {n(x + w)},{n(y + r)} V{n(y + h - r)} "
        f"A{n(r)},{n(r)} 0 0 1 {n(x + w - r)},{n(y + h)} H{n(x + r)} "
        f"A{n(r)},{n(r)} 0 0 1 {n(x)},{n(y + h - r)} V{n(y + r)} "
        f"A{n(r)},{n(r)} 0 0 1 {n(x + r)},{n(y)} Z"
    )


def hexcolor(c):
    return "#FF{:02X}{:02X}{:02X}".format(*c)


background_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Gerado por tools/generate_launcher_icon.py. Não edite à mão. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:startColor="{hexcolor(BG_START)}"
                android:endColor="{hexcolor(BG_END)}" />
        </aapt:attr>
    </path>
</vector>
"""

with open(os.path.join(RES, "drawable/ic_launcher_background.xml"), "w") as f:
    f.write(background_xml)

# Ícone temático (Android 13+): silhueta da calculadora. O Android usa só o
# alfa e aplica a cor do sistema, então display e teclas são vazados com
# evenOdd para o desenho continuar legível como calculadora.
scale = FOREGROUND_GLYPH_H / BODY_H
body_w, body_h = BODY_W * scale, BODY_H * scale
bx, by = (108 - body_w) / 2, (108 - body_h) / 2

subpaths = [rounded_rect_path(bx, by, body_w, body_h, BODY_R * scale)]
subpaths.append(rounded_rect_path(
    bx + DISPLAY["x"] * scale, by + DISPLAY["y"] * scale,
    DISPLAY["w"] * scale, DISPLAY["h"] * scale, DISPLAY["r"] * scale))
for row in KEY_ROWS:
    for col in KEY_COLS:
        subpaths.append(rounded_rect_path(
            bx + col * scale, by + row * scale,
            KEY_W * scale, KEY_H * scale, KEY_R * scale))

monochrome_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Gerado por tools/generate_launcher_icon.py. Não edite à mão. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:fillType="evenOdd"
        android:pathData="{' '.join(subpaths)}" />
</vector>
"""

with open(os.path.join(RES, "drawable/ic_launcher_monochrome.xml"), "w") as f:
    f.write(monochrome_xml)

print("Wrote background gradient + monochrome vector drawables.")

# ---------------------------------------------------------------------------
# Bitmaps
# ---------------------------------------------------------------------------
for folder, (adaptive_px, legacy_px) in DENSITIES.items():
    out_dir = os.path.join(RES, folder)
    os.makedirs(out_dir, exist_ok=True)

    # Camada de frente do adaptive icon: só o glifo, fundo transparente.
    fg = Image.new("RGBA", (adaptive_px, adaptive_px), (0, 0, 0, 0))
    glyph_px = adaptive_px * FOREGROUND_GLYPH_H / 108.0
    centered(fg, scaled_glyph(glyph_px)).save(
        os.path.join(out_dir, "ic_launcher_foreground.png"))

    # Legado: degradê + glifo, recortado em quadrado arredondado e círculo.
    base = centered(gradient(legacy_px).convert("RGBA"),
                    scaled_glyph(legacy_px * LEGACY_GLYPH_FRAC))
    masked(base, "square").save(os.path.join(out_dir, "ic_launcher.png"))
    masked(base, "round").save(os.path.join(out_dir, "ic_launcher_round.png"))

print("Wrote adaptive foregrounds and legacy mipmaps for all densities.")

store_dir = os.path.join(REPO, "play-store")
os.makedirs(store_dir, exist_ok=True)
store = centered(gradient(512).convert("RGBA"), scaled_glyph(512 * STORE_GLYPH_FRAC))
store.convert("RGB").save(os.path.join(store_dir, "ic_launcher-playstore.png"))
print("Wrote Play Store 512x512 listing icon.")
