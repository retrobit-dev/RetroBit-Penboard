"""Pratinjau susunan panel Penboard.

Digambar dari angka yang sama dengan aplikasi:
- ukuran tombol   : GridMath.cellSizePx (3 kolom, 34% lebar layar, min 44 dp)
- warna & opacity : tema retro cyberpunk grayscale ePaper (PenboardConfig)
- tata letak kunci: PenboardConfig.defaultGridKeys()/defaultBottomKeys()
- ikon tombol    : util/PixelArt.kt (polanya dibaca dari berkas Kotlin yang sama)
- bilah tunggu & lencana hasil: BoardView.drawPending / drawFlash
"""
import pathlib
import re

from PIL import Image, ImageDraw, ImageFont

D = 1.375                      # density efektif (2.75 xxhdpi diperkecil 50%)
APP_MOCK_H = 250
PANEL_W = 540                  # lebar panel (px gambar, setara 1080 px asli / 2)
PANEL_PADDING = int(3 * D)
SPACING = int(3 * D)           # GridMath.SPACING_DP
MIN_CELL = int(44 * D)
COLUMNS = 3
IME_ROWS_H = int(176 * D) + 2 * PANEL_PADDING

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
    "SYSTEM_HANDWRITING", "KEYBOARD_PICKER", "HIDE",
]


def load_pixel_art(path="app/src/main/java/dev/retrobit/penboard/util/PixelArt.kt"):
    """Pola pixel art diambil dari berkas Kotlin yang sama dengan aplikasi."""
    text = pathlib.Path(path).read_text()
    patterns = {}
    for match in re.finditer(r"KeyAction\.(\w+) to art\((.*?)\n        \)", text, re.S):
        rows = re.findall(r'"([.#]{12})"', match.group(2))
        if len(rows) == 12:
            patterns[match.group(1)] = rows
    mini = re.search(r"val MINIMIZE: Pattern = art\((.*?)\n    \)", text, re.S)
    if mini:
        patterns["MINIMIZE"] = re.findall(r'"([.#]{12})"', mini.group(1))
    return patterns


def load_bottom_keys(path="app/src/main/java/dev/retrobit/penboard/data/PenboardConfig.kt"):
    """Urutan tombol bilah bawah bawaan, juga dari berkas Kotlin yang sama."""
    text = pathlib.Path(path).read_text()
    body = re.search(r"fun defaultBottomKeys\(\): List<KeyDef> = listOf\((.*?)\n        \)", text, re.S)
    if not body:
        return []
    return re.findall(r"KeyAction\.(\w+)", body.group(1))


def draw_pixel_icon(draw, pattern, cx, cy, size, color):
    """Ikon pixel art: satu kotak per piksel, ukurannya bilangan bulat seperti di aplikasi."""
    if not pattern:
        return
    rows = len(pattern)
    cols = max(len(r) for r in pattern)
    unit = max(1, int(size // 12))
    left = cx - unit * cols / 2
    top = cy - unit * rows / 2
    for y, row in enumerate(pattern):
        for x, ch in enumerate(row):
            if ch == '#':
                draw.rectangle([left + x * unit, top + y * unit,
                                left + (x + 1) * unit - 1, top + (y + 1) * unit - 1], fill=color)


def columns_for(available_width_px, cell_px, spacing_px):
    """Salinan GridMath.columnsFor: berapa tombol muat dalam satu baris."""
    if cell_px <= 0:
        return 1
    return max(1, (available_width_px + spacing_px) // (cell_px + spacing_px))


def wrap_rows(count, columns):
    """Salinan GridMath.wrapRows: berapa baris yang dipakai [count] tombol."""
    cols = max(1, columns)
    return 0 if count <= 0 else (count + cols - 1) // cols


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


def draw_panel(rows, board_height_dp, keys, flash=None, pending=None,
               title="PENBOARD", bottom_keys=None, frame=True, minimize=True):
    """Panel Penboard lengkap: bilah judul/mini, papan, grid tombol, dan tombol bawah.

    frame=False -> bingkai dimatikan; bila minimize tetap nyala, yang digambar hanya
    bilah kecil berisi tombol minimize (persis seperti BoardPanel di aplikasi).
    """
    bottom_keys = BOTTOM_KEYS if bottom_keys is None else bottom_keys
    show_bar = frame or minimize
    title = title if frame else None
    """Panel Penboard: bingkai jendela + bilah judul + papan + grid + bilah tombol bawah."""
    board_h = int(board_height_dp * D)
    cell = cell_size(board_h, PANEL_W, SPACING, MIN_CELL, rows)
    grid_w = cell * COLUMNS + SPACING * (COLUMNS - 1)
    board_w = PANEL_W - 2 * PANEL_PADDING - grid_w - SPACING

    title_h = int((22 if frame else 18) * D) if show_bar else 0
    bottom_keys = bottom_keys or []
    count = len(bottom_keys)
    # Tombol bawah: ukurannya sama dengan tombol samping (cell), mengalir ke baris
    # berikutnya bila tidak muat selebar papan tulis (GridMath.columnsFor/wrapRows).
    bottom_columns = columns_for(board_w, cell, SPACING) if count else 0
    bottom_rows = wrap_rows(count, bottom_columns) if count else 0
    bottom_h = bottom_rows * (cell + SPACING) if count else 0
    grid_h = rows * cell + (rows - 1) * SPACING
    board_stack_h = board_h + bottom_h
    content_h = max(board_stack_h, grid_h)
    panel_h = title_h + content_h + 2 * PANEL_PADDING

    img = Image.new("RGBA", (PANEL_W, panel_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    radius = int(RADIUS_DP * D)

    draw.rounded_rectangle([0, 0, PANEL_W - 1, panel_h - 1], radius,
                           fill=alpha(COL_PANEL, 255 * PANEL_OPACITY),
                           outline=alpha(COL_ACCENT, 190 if frame else 120),
                           width=2 if frame else 1)
    draw_scanlines(img, (0, 0, PANEL_W - 1, panel_h - 1), radius, COL_ACCENT)

    # bilah judul jendela (bingkai) + garis pemisah; tanpa bingkai hanya tombol minimize
    if title_h:
        if frame:
            draw.rectangle([1, 1, PANEL_W - 2, 1 + title_h + PANEL_PADDING],
                           fill=alpha(COL_ACCENT, 30))
            draw.line([(1, 2 + title_h + PANEL_PADDING), (PANEL_W - 2, 2 + title_h + PANEL_PADDING)],
                      fill=alpha(COL_ACCENT, 150), width=1)
            draw.text((PANEL_PADDING + 4, PANEL_PADDING + (title_h - 12 * D) / 2), title,
                      font=ImageFont.truetype(FONT_BOLD, int(10 * D)), fill=alpha(COL_ACCENT, 230))
        if minimize:
            ms = int((18 if frame else 18) * D)
            mx1 = PANEL_W - PANEL_PADDING - 2
            my0 = PANEL_PADDING + (1 if frame else 0)
            draw.rounded_rectangle([mx1 - ms, my0, mx1, my0 + ms], int(RADIUS_DP * D * 0.6),
                                   fill=alpha(COL_BUTTON, 255 * BUTTON_OPACITY),
                                   outline=alpha(COL_ACCENT, 170), width=1)
            draw_pixel_icon(draw, PIXEL_ART.get("MINIMIZE"), mx1 - ms / 2, my0 + ms / 2,
                            ms * 0.66, COL_TEXT)

    bl = PANEL_PADDING
    bt = PANEL_PADDING + title_h + (SPACING if title_h else 0)
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
    # tombol bawah: tepat di bawah papan tulis, di dalam bingkai, ukuran = tombol samping
    if count:
        for index, action in enumerate(bottom_keys):
            col = index % bottom_columns
            row = index // bottom_columns
            x0 = bl + col * (cell + SPACING)
            y0 = bt + board_h + SPACING + row * (cell + SPACING)
            draw.rounded_rectangle([x0, y0, x0 + cell, y0 + cell],
                                   max(2, int(RADIUS_DP * D * 0.9)),
                                   fill=alpha(COL_BUTTON, 255 * BUTTON_OPACITY),
                                   outline=alpha(COL_ACCENT, 130), width=1)
            draw_pixel_icon(draw, PIXEL_ART.get(action), x0 + cell / 2, y0 + cell / 2,
                            cell * 0.62, COL_TEXT)

    # tombol samping
    gx = br + SPACING
    gy = bt + max(0, (content_h - grid_h) // 2)
    for index, action in enumerate(keys[: rows * COLUMNS]):
        col, row = index % COLUMNS, index // COLUMNS
        x0 = gx + col * (cell + SPACING)
        y0 = gy + row * (cell + SPACING)
        x1, y1 = x0 + cell, y0 + cell
        draw.rounded_rectangle([x0, y0, x1, y1], max(2, int(RADIUS_DP * D * 0.9)),
                               fill=alpha(COL_BUTTON, 255 * BUTTON_OPACITY),
                               outline=alpha(COL_ACCENT, 130), width=1)
        draw_pixel_icon(draw, PIXEL_ART.get(action), (x0 + x1) / 2, (y0 + y1) / 2,
                        cell * 0.62, COL_TEXT)

    return img


def section(canvas, y, caption, sub=None):
    draw = ImageDraw.Draw(canvas)
    draw.text((40, y), caption, font=LABEL_FONT, fill=alpha(COL_ACCENT, 255))
    if sub:
        draw.text((40, y + 22), sub, font=SMALL_FONT, fill=alpha(COL_ACCENT, 150))
        return y + 44
    return y + 26


def main():
    global IME_PANEL
    IME_PANEL = draw_panel(3, 176, DEFAULT_KEYS, flash="\u2713 halo", pending=0.55)
    pad = 40
    width = PANEL_W + pad * 2
    header = 66
    panels = [
        ("1. Keyboard Penboard - 3 baris (bawaan)",
         "bingkai jendela + minimize + 5 tombol pixel art di bawah papan",
         ("field", draw_panel(3, 176, DEFAULT_KEYS, flash="\u2713 halo", pending=0.55))),
        ("2. Preset HiWrite - 6 baris (18 tombol)",
         "geser 'jumlah baris' 1-6 di Pengaturan > Area tombol",
         ("plain", draw_panel(6, 200, DEFAULT_KEYS))),
        ("3. Keyboard Penboard tampil di atas aplikasi lain",
         "papan tulis menempel sebagai keyboard di aplikasi apa pun",
         "ime"),
        ("4. Bingkai jendela dimatikan - tombol minimize tetap ada",
         "bilah kecil 18 dp di kanan atas papan (Pengaturan > Jendela papan)",
         ("plain", draw_panel(3, 176, DEFAULT_KEYS, frame=False))),
    ]
    heights = [header]
    for _, _, item in panels:
        if isinstance(item, tuple):
            heights.append(52 + item[1].height + 60)
        elif item == "ime":
            heights.append(APP_MOCK_H + IME_PANEL.height + 60)
        else:
            heights.append(item.height + 60)
    canvas = Image.new("RGBA", (width, sum(heights) + 40), alpha((0x12, 0x12, 0x12), 255))
    draw = ImageDraw.Draw(canvas)
    draw.text((pad, 20), "Penboard 3.1.x - susunan panel", font=TITLE_FONT, fill=alpha(COL_ACCENT, 255))
    draw.text((pad, 44), "bingkai jendela + tombol minimize + bilah tombol pixel art", font=SMALL_FONT,
              fill=alpha(COL_ACCENT, 150))

    y = header
    for caption, sub, item in panels:
        y = section(canvas, y, caption, sub)
        img = item[1] if isinstance(item, tuple) else item
        if item == "ime":
            # aplikasi lain di belakang, keyboard Penboard menempel di bawah
            ime = IME_PANEL
            block = Image.new("RGBA", (PANEL_W, APP_MOCK_H + ime.height), (0x2A, 0x2A, 0x2A, 255))
            bd = ImageDraw.Draw(block)
            bd.rounded_rectangle([24, 24, PANEL_W - 24, 46], 8, fill=(0x33, 0x33, 0x33, 255),
                                 outline=alpha(COL_ACCENT, 70), width=1)
            bd.text((36, 30), "aplikasi lain", font=LABEL_FONT, fill=alpha(COL_ACCENT, 170))
            for i in range(5):
                bd.rounded_rectangle([24, 70 + i * 34, PANEL_W - 24 - (i % 3) * 90, 88 + i * 34], 6,
                                     fill=(0x3E, 0x3E, 0x3E, 255))
            bd.line([(0, APP_MOCK_H), (PANEL_W, APP_MOCK_H)], fill=alpha(COL_ACCENT, 60), width=1)
            block.alpha_composite(ime, (0, APP_MOCK_H))
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


PIXEL_ART = load_pixel_art()
BOTTOM_KEYS = load_bottom_keys()[:5]
IME_PANEL = None  # diisi di main() supaya tingginya pasti (bingkai + papan + bilah bawah)

if __name__ == "__main__":
    import os
    os.makedirs("docs", exist_ok=True)
    main()
