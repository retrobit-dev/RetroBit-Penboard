"""Pratinjau susunan panel Penboard.

Digambar dari angka yang sama dengan aplikasi:
- ukuran tombol   : GridMath.cellSizePx (3 kolom, 34% lebar layar, min 44 dp)
- warna & opacity : tema retro cyberpunk grayscale ePaper (PenboardConfig)
- tata letak kunci: PenboardConfig.defaultGridKeys()
- bilah tunggu & lencana hasil: BoardView.drawPending / drawFlash
"""
from PIL import Image, ImageDraw, ImageFont

D = 1.375                      # density efektif (2.75 xxhdpi diperkecil 50%)
PANEL_W = 540                  # lebar panel (px gambar, setara 1080 px asli / 2)
PANEL_PADDING = int(3 * D)
SPACING = int(3 * D)           # GridMath.SPACING_DP
MIN_CELL = int(44 * D)
COLUMNS = 3

COL_PANEL = (0x0B, 0x0B, 0x0B)
COL_PAPER = (0xED, 0xED, 0xED)
COL_BUTTON = (0x16, 0x16, 0x16)
COL_ACCENT = (0xC8, 0xC8, 0xC8)
COL_TEXT = (0xE6, 0xE6, 0xE6)
COL_LINE = (0xB9, 0xB9, 0xB9)
COL_INK = (0x10, 0x10, 0x10)
PANEL_OPACITY, BOARD_OPACITY, BUTTON_OPACITY = 0.90, 0.97, 0.92
RADIUS_DP = 6

FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
LABEL_FONT = ImageFont.truetype(FONT_PATH, 17)
TITLE_FONT = ImageFont.truetype(FONT_BOLD, 21)
SMALL_FONT = ImageFont.truetype(FONT_PATH, 14)

DEFAULT_KEYS = [
    "CURSOR_LEFT", "CURSOR_UP", "CURSOR_RIGHT",
    "CURSOR_DOWN", "SPACE", "ENTER",
    "BACKSPACE", "RECOGNIZE", "CLEAR",
    "DELETE_WORD", "UNDO", "REDO",
    "DELETE_FORWARD", "TAB", "CYCLE_PEN",
    "SYSTEM_HANDWRITING", "OVERLAY_TOGGLE", "HIDE",
]


def cell_size(board_height_px, available_width_px, spacing_px, min_cell_px, rows):
    """Salinan GridMath.cellSizePx."""
    rows = max(1, min(6, rows))
    usable = max(board_height_px - spacing_px * (rows - 1), 1)
    cell = usable // rows
    budget = int(available_width_px * 0.34)
    max_by_width = (budget - spacing_px * (COLUMNS - 1)) // COLUMNS
    if 1 <= max_by_width < cell:
        cell = max_by_width
    if min_cell_px > 0 and cell < min_cell_px:
        cell = min(min_cell_px, usable // rows)
    return max(cell, 1)


def alpha(color, a):
    return color + (max(0, min(255, int(a))),)


def glyph(draw, action, cx, cy, size):
    """Gambar ikon tombol pada kanvas 24 x 24 (seperti ui/KeyIcons.kt)."""
    u = size / 24.0
    w = max(2, int(2.3 * u))

    def p(x, y):
        return (cx + (x - 12) * u, cy + (y - 12) * u)

    def path(points, width=None):
        draw.line([p(x, y) for x, y in points], fill=COL_TEXT, width=width or w, joint="curve")

    if action == "CURSOR_LEFT":
        path([(19, 12), (6, 12)]); path([(10, 7), (5, 12), (10, 17)])
    elif action == "CURSOR_RIGHT":
        path([(5, 12), (18, 12)]); path([(14, 7), (19, 12), (14, 17)])
    elif action == "CURSOR_UP":
        path([(12, 19), (12, 6)]); path([(7, 11), (12, 5), (17, 11)])
    elif action == "CURSOR_DOWN":
        path([(12, 5), (12, 18)]); path([(7, 13), (12, 19), (17, 13)])
    elif action == "SPACE":
        path([(4, 18), (4, 13), (20, 13), (20, 18)])
    elif action == "ENTER":
        path([(18, 5), (18, 15), (8, 15)]); path([(12, 10), (6, 15), (12, 20)])
    elif action == "BACKSPACE":
        path([(9, 5), (20, 5), (20, 19), (9, 19), (3, 12), (9, 5)])
        path([(12, 9), (17, 15)]); path([(17, 9), (12, 15)])
    elif action == "DELETE_FORWARD":
        path([(15, 5), (4, 5), (4, 19), (15, 19), (21, 12), (15, 5)])
        path([(7, 9), (12, 15)]); path([(12, 9), (7, 15)])
    elif action == "DELETE_WORD":
        path([(14, 6), (8, 12), (14, 18)]); path([(20, 6), (14, 12), (20, 18)])
    elif action == "TAB":
        path([(21, 6), (21, 18)]); path([(4, 12), (17, 12)]); path([(12, 7), (18, 12), (12, 17)])
    elif action == "RECOGNIZE":
        font = ImageFont.truetype(FONT_BOLD, int(size * 0.82))
        box = draw.textbbox((0, 0), "A+", font=font)
        draw.text((cx - (box[2] - box[0]) / 2 - box[0], cy - (box[3] - box[1]) / 2 - box[1]),
                  "A+", font=font, fill=COL_TEXT)
    elif action == "CLEAR":
        path([(5, 7), (19, 7)]); path([(10, 4), (14, 4)])
        path([(7, 7), (7, 19), (17, 19), (17, 7)])
        path([(10, 10), (10, 16)]); path([(14, 10), (14, 16)])
    elif action == "CYCLE_PEN":
        path([(6, 18), (16, 8)], width=max(3, int(3.4 * u)))
        path([(4, 20), (6, 17), (8, 19)]); path([(16, 8), (19, 5)])
    elif action == "UNDO":
        draw.arc([p(5, 8)[0], p(5, 8)[1], p(19, 20)[0], p(19, 20)[1]], 180, 350,
                 fill=COL_TEXT, width=w)
        path([(5, 8), (5, 14)]); path([(2, 11), (5, 15), (8, 11)])
    elif action == "REDO":
        draw.arc([p(5, 8)[0], p(5, 8)[1], p(19, 20)[0], p(19, 20)[1]], 190, 360,
                 fill=COL_TEXT, width=w)
        path([(19, 8), (19, 14)]); path([(16, 11), (19, 15), (22, 11)])
    elif action == "SYSTEM_HANDWRITING":
        path([(4, 16), (7, 10), (10, 15), (13, 9), (16, 14), (20, 12)])
    elif action == "OVERLAY_TOGGLE":
        r = max(2, int(12 * u))
        draw.rounded_rectangle([p(3, 8)[0], p(3, 8)[1], p(13, 18)[0], p(13, 18)[1]], r,
                               outline=COL_TEXT, width=w)
        draw.rounded_rectangle([p(10, 6)[0], p(10, 6)[1], p(20, 16)[0], p(20, 16)[1]], r,
                               outline=COL_TEXT, width=w)
    elif action == "HIDE":
        path([(12, 4), (12, 13)]); path([(7, 9), (12, 14), (17, 9)]); path([(5, 18), (19, 18)])


def draw_scanlines(img, box, radius, color, spacing=4, a=26):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    y = box[1] + spacing
    while y < box[3]:
        ld.line([(box[0], y), (box[2], y)], fill=alpha(color, a), width=1)
        y += spacing
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(box, radius, fill=255)
    img.paste(layer, (0, 0), Image.composite(layer.split()[3], Image.new("L", img.size, 0), mask))


def draw_board_ink(draw, left, top, right, line_gap):
    """Contoh tulisan tangan pada baris pertama papan."""
    base = top + line_gap - line_gap * 0.28
    u = (right - left) / 12.0
    ink = alpha(COL_INK, 235)

    def stroke(points, w=3):
        draw.line([(left + x * u, base + y * (line_gap * 0.30)) for x, y in points],
                  fill=ink, width=w, joint="curve")

    stroke([(0.9, -0.5), (1.1, 0.2), (1.5, -0.7), (1.9, 0.3), (2.3, -0.6), (2.7, 0.1)])
    stroke([(3.1, -0.9), (3.05, 0.4), (3.6, -0.2), (4.0, 0.3), (4.5, -0.6), (5.0, 0.2)])
    stroke([(5.5, -0.9), (5.5, 0.35), (6.6, -0.35), (6.6, 0.35)])
    stroke([(7.2, 0.3), (7.0, -0.4), (7.9, -0.6), (8.3, 0.1), (7.8, 0.5), (7.3, 0.2)])


def draw_panel(rows, board_height_dp, keys, flash=None, pending=None, handle=False):
    handle_h = 20 if handle else 0
    board_h = int(board_height_dp * D)
    cell = cell_size(board_h, PANEL_W, SPACING, MIN_CELL, rows)
    grid_w = cell * COLUMNS + SPACING * (COLUMNS - 1)
    board_w = PANEL_W - 2 * PANEL_PADDING - grid_w - SPACING
    panel_h = board_h + 2 * PANEL_PADDING + handle_h

    img = Image.new("RGBA", (PANEL_W, panel_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    radius = int(RADIUS_DP * D)
    if handle:
        hw = 46
        draw.rounded_rectangle([PANEL_W / 2 - hw, 8, PANEL_W / 2 + hw, 13], 3,
                               fill=alpha(COL_ACCENT, 170))

    draw.rounded_rectangle([0, 0, PANEL_W - 1, panel_h - 1], radius,
                           fill=alpha(COL_PANEL, 255 * PANEL_OPACITY),
                           outline=alpha(COL_ACCENT, 120), width=1)
    draw_scanlines(img, (0, 0, PANEL_W - 1, panel_h - 1), radius, COL_ACCENT)

    bl, bt = PANEL_PADDING, PANEL_PADDING + handle_h
    br, bb = bl + board_w, bt + board_h
    draw.rounded_rectangle([bl, bt, br, bb], radius, fill=alpha(COL_PAPER, 255 * BOARD_OPACITY),
                           outline=alpha(COL_LINE, 110), width=1)
    # garis panduan titik-titik
    line_gap = 48 * D if board_height_dp >= 200 else 44 * D
    inset = 3 * D
    y = bt + line_gap
    while y < bb - 4:
        x = bl + inset
        while x < br - inset:
            draw.line([(x, y), (min(x + 2, br - inset), y)], fill=alpha(COL_LINE, 150), width=1)
            x += 7
        y += line_gap
    # sudut kiri papan rata (edgeToEdge)
    draw.rectangle([bl, bt, bl + radius, bt + radius], fill=alpha(COL_PAPER, 255 * BOARD_OPACITY))
    draw.rectangle([bl, bb - radius, bl + radius, bb], fill=alpha(COL_PAPER, 255 * BOARD_OPACITY))
    if flash:
        draw_board_ink(draw, bl, bt, br, line_gap)
    # bilah tunggu input otomatis
    if pending is not None:
        bar = max(2, int(3 * D))
        draw.rectangle([bl, bb - bar, br, bb], fill=alpha(COL_LINE, 60))
        draw.rectangle([bl, bb - bar, bl + (br - bl) * pending, bb], fill=alpha(COL_ACCENT, 220))
    # lencana hasil
    if flash:
        font = ImageFont.truetype(FONT_BOLD, int(13 * D))
        box = draw.textbbox((0, 0), flash, font=font)
        pad = 9 * D
        chip_h = (box[3] - box[1]) + 10 * D
        chip_top = bb - chip_h - (8 * D if pending is not None else 5 * D)
        draw.rounded_rectangle([bl + 6 * D, chip_top, bl + 6 * D + (box[2] - box[0]) + pad * 2, chip_top + chip_h],
                               chip_h / 2, fill=alpha(COL_ACCENT, 240))
        draw.text((bl + 6 * D + pad - box[0], chip_top + (chip_h - (box[3] - box[1])) / 2 - box[1]),
                  flash, font=font, fill=alpha((0x0B, 0x0B, 0x0B), 255))
    # tombol
    gx = br + SPACING
    gy = bt
    for index, action in enumerate(keys[: rows * COLUMNS]):
        col, row = index % COLUMNS, index // COLUMNS
        x0 = gx + col * (cell + SPACING)
        y0 = gy + row * (cell + SPACING)
        x1, y1 = x0 + cell, y0 + cell
        draw.rounded_rectangle([x0, y0, x1, y1], max(2, int(RADIUS_DP * D * 0.9)),
                               fill=alpha(COL_BUTTON, 255 * BUTTON_OPACITY),
                               outline=alpha(COL_ACCENT, 130), width=1)
        glyph(draw, action, (x0 + x1) / 2, (y0 + y1) / 2, cell * 0.52)
    return img


def section(canvas, y, caption, sub=None):
    draw = ImageDraw.Draw(canvas)
    draw.text((40, y), caption, font=LABEL_FONT, fill=alpha(COL_ACCENT, 255))
    if sub:
        draw.text((40, y + 22), sub, font=SMALL_FONT, fill=alpha(COL_ACCENT, 150))
        return y + 44
    return y + 26


def main():
    pad = 40
    width = PANEL_W + pad * 2
    header = 66
    panels = [
        ("1. Keyboard Penboard - 3 baris (bawaan)",
         "menggantikan keyboard biasa; hasil tulisan masuk ke kolom isian",
         ("field", draw_panel(3, 176, DEFAULT_KEYS, flash="\u2713 halo", pending=0.55))),
        ("2. Preset HiWrite - 6 baris (18 tombol)",
         "geser 'jumlah baris' 1-6 di Pengaturan > Area tombol",
         ("plain", draw_panel(6, 200, DEFAULT_KEYS))),
        ("3. Papan melayang 60% transparan di atas aplikasi lain",
         "tarik bilah pegangan di atas papan untuk memindahkannya",
         "floating"),
    ]
    heights = [header]
    for _, _, item in panels:
        if isinstance(item, tuple):
            heights.append(52 + item[1].height + 60)
        elif item == "floating":
            heights.append(275 + 60)
        else:
            heights.append(item.height + 60)
    canvas = Image.new("RGBA", (width, sum(heights) + 40), alpha((0x12, 0x12, 0x12), 255))
    draw = ImageDraw.Draw(canvas)
    draw.text((pad, 20), "Penboard 3.0.x - susunan panel", font=TITLE_FONT, fill=alpha(COL_ACCENT, 255))
    draw.text((pad, 44), "digambar dari angka pengaturan yang sama dengan aplikasi", font=SMALL_FONT,
              fill=alpha(COL_ACCENT, 150))

    y = header
    for caption, sub, item in panels:
        y = section(canvas, y, caption, sub)
        img = item[1] if isinstance(item, tuple) else item
        if item == "floating":
            # contoh "aplikasi lain" di belakang papan melayang
            floating = draw_panel(6, 200, DEFAULT_KEYS, handle=True)
            block = Image.new("RGBA", (PANEL_W, floating.height + 16), (0x2A, 0x2A, 0x2A, 255))
            bd = ImageDraw.Draw(block)
            for i in range(7):
                bd.rounded_rectangle([24, 30 + i * 38, PANEL_W - 24 - (i % 3) * 90, 50 + i * 38], 6,
                                     fill=(0x3E, 0x3E, 0x3E, 255))
            a = floating.getchannel("A").point(lambda v: int(v * 0.60))
            floating.putalpha(a)
            block.alpha_composite(floating, (0, 8))
            canvas.alpha_composite(block, (pad, y))
            y += block.height + 34
        elif isinstance(item, tuple) and item[0] == "field":
            strip = Image.new("RGBA", (PANEL_W, 52 + img.height), (0x1A, 0x1A, 0x1A, 255))
            sd = ImageDraw.Draw(strip)
            sd.rounded_rectangle([8, 8, PANEL_W - 8, 44], 8, fill=(0x2A, 0x2A, 0x2A, 255),
                                 outline=alpha(COL_ACCENT, 60), width=1)
            sd.text((24, 18), "Halo, aku menulis dengan M-Pen...", font=LABEL_FONT,
                    fill=alpha(COL_ACCENT, 230))
            strip.alpha_composite(img, (0, 52))
            canvas.alpha_composite(strip, (pad, y))
            y += strip.height + 34
        else:
            canvas.alpha_composite(img, (pad, y))
            y += img.height + 34
    canvas.convert("RGB").save("docs/preview.png", optimize=True)
    print("docs/preview.png", canvas.size)


if __name__ == "__main__":
    import os
    os.makedirs("docs", exist_ok=True)
    main()
