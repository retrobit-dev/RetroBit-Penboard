"""Pratinjau susunan panel Penboard.

Digambar dari angka yang sama dengan aplikasi:
- ukuran tombol   : GridMath.cellSizePx (persegi: tinggi = sisi tombol x jumlah baris)
- tinggi papan    : GridMath.boardHeightPx / gridWidthPx / boardWidthPx
- tombol bawah    : GridMath.columnsFor / wrapRows, tingginya = sisi tombol samping
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
PANEL_W = 540                  # lebar panel penuh (px gambar, setara 1080 px asli / 2)
PANEL_PADDING = int(3 * D)
SPACING = int(3 * D)           # GridMath.SPACING_DP
MIN_CELL = int(34 * D)         # GridMath.MIN_TOUCH_DP
MIN_BOARD_W = int(72 * D)
MIN_BOARD_FRACTION = 0.40
MAX_BOARD_FRACTION = 0.80
DEFAULT_BOARD_FRACTION = 0.60
MAX_COLUMNS = 6
MAX_ROWS = 6
MIN_LINES = 1
MAX_LINES = 6
DEFAULT_LINES = 3
LINE_GAP_DP = 48
MIN_HEIGHT_MULTIPLIER = 0.6
MAX_HEIGHT_MULTIPLIER = 2.0

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
    "SELECT_ALL", "CUT", "COPY", "PASTE",
    "CURSOR_HOME", "CURSOR_END", "DELETE_WORD", "DELETE_FORWARD",
    "TAB", "CLEAR", "RECOGNIZE", "UNDO", "REDO", "CYCLE_PEN",
    "SYSTEM_HANDWRITING", "KEYBOARD_PICKER", "SELECT_ALL", "PASTE",
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


# ----------------------------------------------------------------- GridMath (salinan persis)

def _trunc(value, low, high):
    return max(low, min(high, int(value)))


def columns_or(value):
    return _trunc(value, 1, MAX_COLUMNS)


def rows_or(value):
    return _trunc(value, 1, MAX_ROWS)


def lines_or(value):
    return _trunc(value, MIN_LINES, MAX_LINES)


def board_height_for_lines(lines, line_gap_px):
    return lines_or(lines) * max(1, line_gap_px)


def button_height_px(cell_px, height_multiplier):
    """Salinan GridMath.buttonHeightPx."""
    value = max(MIN_HEIGHT_MULTIPLIER, min(MAX_HEIGHT_MULTIPLIER, height_multiplier))
    return max(1, int(round(cell_px * value)))


def bottom_row_heights(rows, cell_px, height_multipliers):
    """Salinan GridMath.bottomRowHeights."""
    result = []
    for indexes in rows:
        result.append(max(
            button_height_px(cell_px,
                             height_multipliers[index] if index < len(height_multipliers) else 1.0)
            for index in indexes
        ))
    return result


def bottom_bar_height_px(row_heights, spacing_px=0):
    """Salinan GridMath.bottomBarHeightPx."""
    if not row_heights:
        return 0
    return sum(row_heights) + spacing_px * (len(row_heights) - 1)


def board_height_px(rows, cell_px, spacing_px=0):
    rows = rows_or(rows)
    return rows * cell_px + spacing_px * (rows - 1)


def grid_width_px(columns, cell_px, spacing_px=0):
    columns = columns_or(columns)
    return columns * cell_px + spacing_px * (columns - 1)


def board_width_px(panel_width_px, padding_px, grid_width, spacing_px=0):
    return max(panel_width_px - 2 * padding_px - grid_width - spacing_px, 1)


def board_fraction_or(value):
    """Salinan GridMath.boardFractionOr: lebar area tulis 40-80% isi panel."""
    return max(MIN_BOARD_FRACTION, min(MAX_BOARD_FRACTION, value))


def board_width_for(usable_width, fraction, min_board_width, grid_min_width, spacing_px=0):
    """Salinan GridMath.boardWidthFor: lebar area tulis yang dijamin."""
    usable = max(1, usable_width)
    max_board = max(1, usable - spacing_px - grid_min_width)
    lower = min(max(1, min_board_width), max_board)
    wanted = max(1, int(usable * board_fraction_or(fraction)))
    return max(lower, min(max_board, wanted))


def max_cell_by_width(usable_width, board_width, columns, spacing_px=0):
    """Salinan GridMath.maxCellByWidthPx: tombol tidak boleh menekan area tulis."""
    columns = columns_or(columns)
    room = max(1, usable_width - board_width - spacing_px)
    return max(1, (room - spacing_px * (columns - 1)) // columns)


def cell_size(board_height, available_width, spacing_px=0, min_cell_px=0, rows=3,
              columns=3, cell_dp=0, density=1.0, min_board_width_px=0):
    """Salinan GridMath.cellSizePx: tombol persegi, dibatasi tinggi & lebar."""
    rows = rows_or(rows)
    columns = columns_or(columns)
    from_height = max((board_height - spacing_px * (rows - 1)) // rows, 1)
    if min_board_width_px > 0:
        budget = max(available_width - min_board_width_px - spacing_px, 1)
    else:
        budget = int(available_width * 0.34)
    from_width = max((budget - spacing_px * (columns - 1)) // columns, 1)
    explicit = int(cell_dp * density) if cell_dp > 0 else 0
    desired = explicit if explicit > 0 else from_width
    fitting = max(min(desired, from_width, from_height), 1)
    return max(fitting, min(min_cell_px, from_height), 1)


def columns_for(available_width_px, cell_px, spacing_px=0):
    """Salinan GridMath.columnsFor: berapa tombol muat dalam satu baris."""
    if cell_px <= 0:
        return 1
    return max(1, (available_width_px + spacing_px) // (cell_px + spacing_px))


def wrap_rows(count, columns):
    """Salinan GridMath.wrapRows: berapa baris yang dipakai [count] tombol."""
    cols = max(1, columns)
    return 0 if count <= 0 else (count + cols - 1) // cols


def button_width_px(cell_px, width_multiplier):
    """Salinan GridMath.buttonWidthPx: tinggi tombol samping x pengali lebarnya."""
    value = max(1.0, min(4.0, width_multiplier))
    return max(1, int(round(cell_px * value)))


def bottom_row_indexes(count, cell_px, widths, available_width_px, spacing_px=0):
    """Salinan GridMath.bottomRowIndexes: tombol atas selalu SATU baris.

    Tombol dipasang berurutan dari kiri; begitu tidak muat lagi, sisanya tidak
    ditampilkan (sisa ruang di kanan dibiarkan kosong).
    """
    if count <= 0:
        return []
    visible = []
    used = 0
    for index in range(count):
        width = button_width_px(cell_px, widths[index] if index < len(widths) else 1.0)
        needed = width if not visible else used + spacing_px + width
        if visible and needed > available_width_px:
            break
        used = needed
        visible.append(index)
    return [visible]


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


def draw_board_ink(draw, left, top, right, line_gap, line_index=0):
    """Contoh tulisan tangan pada baris ke-[line_index] (0 = baris pertama)."""
    base = top + line_gap * (line_index + 1) - line_gap * 0.28
    u = (right - left) / 12.0
    ink = alpha(COL_INK, 235)

    def stroke(points, w=3):
        draw.line([(left + x * u, base + y * (line_gap * 0.30)) for x, y in points],
                  fill=ink, width=w, joint="curve")

    stroke([(0.9, -0.5), (1.1, 0.2), (1.5, -0.7), (1.9, 0.3), (2.3, -0.6), (2.7, 0.1)])
    stroke([(3.1, -0.9), (3.05, 0.4), (3.6, -0.2), (4.0, 0.3), (4.5, -0.6), (5.0, 0.2)])
    stroke([(5.5, -0.9), (5.5, 0.35), (6.6, -0.35), (6.6, 0.35)])
    stroke([(7.2, 0.3), (7.0, -0.4), (7.9, -0.6), (8.3, 0.1), (7.8, 0.5), (7.3, 0.2)])


def panel_geometry(lines, columns=3, cell_dp=0, panel_width=None, frame=True, minimize=True,
                   bottom_count=0, widths=None, heights=None, line_gap_dp=LINE_GAP_DP,
                   board_fraction=DEFAULT_BOARD_FRACTION, rows=None):
    """Ukuran-ukuran panel, dihitung dengan rumus yang sama seperti BoardPanel.

    - area tulis: tinggi = baris x jarak baris
    - tombol samping: baris = baris area tulis, rata atas, ukuran bisa otomatis/ sendiri
    - tombol bawah: di ATAS area tulis, lebar & tingginya bisa diatur per tombol
    """
    panel_w = panel_width or PANEL_W
    lines = lines_or(lines)
    # Baris tombol bisa mengikuti area tulis (None) atau diatur sendiri 1-6.
    rows = lines if rows is None else rows_or(rows)
    columns = columns_or(columns)
    title_h = int((22 if frame else 18) * D) if (frame or minimize) else 0
    line_gap_px = int(line_gap_dp * D)
    board_h = board_height_for_lines(lines, line_gap_px)
    usable_w = panel_w - 2 * PANEL_PADDING
    # Lebar area tulis dijamin dulu; kolom tombol mengambil sisanya dan ukurannya
    # dipangkas supaya tidak terpotong di tepi kanan.
    board_w = board_width_for(usable_w, board_fraction, MIN_BOARD_W,
                              grid_width_px(columns, MIN_CELL, SPACING), SPACING)
    max_cell_w = max_cell_by_width(usable_w, board_w, columns, SPACING)
    # Ukuran otomatis = tinggi satu baris tulisan, berapa pun baris tombolnya.
    cell = cell_size(board_h, usable_w, SPACING, MIN_CELL, lines, columns, 0, D, board_w)
    if cell_dp > 0:
        cell = int(cell_dp * D)
    cell = max(1, min(cell, max_cell_w))
    grid_w = grid_width_px(columns, cell, SPACING)
    widths = widths or []
    heights = heights or []
    # Bilah bawah memakai lebar isi panel (di atas area tulis), bukan lebar papan.
    rows_of_buttons = bottom_row_indexes(bottom_count, cell, widths, usable_w, SPACING)
    row_heights = bottom_row_heights(rows_of_buttons, cell, heights)
    bottom_h = 0 if not row_heights else SPACING + bottom_bar_height_px(row_heights, SPACING)
    title_block = (title_h + SPACING) if title_h else 0
    grid_h = board_height_px(rows, cell, SPACING)
    # jarak setelah bilah bawah hanya ada bila bilahnya tampil
    panel_h = 2 * PANEL_PADDING + title_block + bottom_h + (SPACING if bottom_h else 0) + max(board_h, grid_h)
    return {
        "panel_w": panel_w, "panel_h": panel_h, "title_h": title_h, "cell": cell,
        "rows": rows, "columns": columns, "lines": lines, "line_gap": line_gap_px,
        "board_h": board_h, "board_w": board_w, "grid_w": grid_w, "grid_h": grid_h,
        "bottom_rows": row_heights, "bottom_indexes": rows_of_buttons,
        "bottom_h": bottom_h,
    }


def draw_panel(lines, keys, flash=None, pending=None,
               title="PENBOARD", bottom_keys=None, frame=True, minimize=True,
               columns=3, cell_dp=0, panel_width=None, widths=None, heights=None, marks=None,
               ink_line=0, line_gap_dp=LINE_GAP_DP, board_fraction=DEFAULT_BOARD_FRACTION,
               rows=None):
    """Panel Penboard lengkap: bilah judul, papan tulis, kolom tombol persegi, tombol bawah.

    frame=False -> bingkai dimatikan; bila minimize tetap nyala, yang digambar hanya
    bilah kecil berisi tombol minimize (persis seperti BoardPanel di aplikasi).
    marks: penanda pegangan tarik ("drag" = bilah judul, "corner" = sudut pengubah ukuran).
    """
    bottom_keys = BOTTOM_KEYS if bottom_keys is None else bottom_keys
    bottom_keys = bottom_keys or []
    bottom_count = len(bottom_keys)
    geo = panel_geometry(lines, columns, cell_dp, panel_width,
                         frame, minimize, bottom_count, widths, heights, line_gap_dp,
                         board_fraction, rows)
    panel_w, panel_h = geo["panel_w"], geo["panel_h"]
    cell, board_h, board_w, grid_w = geo["cell"], geo["board_h"], geo["board_w"], geo["grid_w"]
    title_h = geo["title_h"]
    show_bar = frame or minimize
    title = title if frame else None

    img = Image.new("RGBA", (panel_w, panel_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    radius = int(RADIUS_DP * D)

    draw.rounded_rectangle([0, 0, panel_w - 1, panel_h - 1], radius,
                           fill=alpha(COL_PANEL, 255 * PANEL_OPACITY),
                           outline=alpha(COL_ACCENT, 190 if frame else 120),
                           width=2 if frame else 1)
    draw_scanlines(img, (0, 0, panel_w - 1, panel_h - 1), radius, COL_ACCENT)

    # bilah judul jendela (bingkai) + garis pemisah; tanpa bingkai hanya tombol minimize
    if title_h and show_bar:
        if frame:
            draw.rectangle([1, 1, panel_w - 2, 1 + title_h + PANEL_PADDING],
                           fill=alpha(COL_ACCENT, 30))
            draw.line([(1, 2 + title_h + PANEL_PADDING), (panel_w - 2, 2 + title_h + PANEL_PADDING)],
                      fill=alpha(COL_ACCENT, 150), width=1)
            draw.text((PANEL_PADDING + 4, PANEL_PADDING + (title_h - 12 * D) / 2), title,
                      font=ImageFont.truetype(FONT_BOLD, int(10 * D)), fill=alpha(COL_ACCENT, 230))
        if minimize:
            ms = int(18 * D)
            mx1 = panel_w - PANEL_PADDING - 2
            my0 = PANEL_PADDING + (1 if frame else 0)
            draw.rounded_rectangle([mx1 - ms, my0, mx1, my0 + ms], int(RADIUS_DP * D * 0.6),
                                   fill=alpha(COL_BUTTON, 255 * BUTTON_OPACITY),
                                   outline=alpha(COL_ACCENT, 170), width=1)
            draw_pixel_icon(draw, PIXEL_ART.get("MINIMIZE"), mx1 - ms / 2, my0 + ms / 2,
                            ms * 0.66, COL_TEXT)

    bl = PANEL_PADDING
    # urutan: bilah judul -> bilah tombol bawah -> area tulis + kolom tombol
    bt = PANEL_PADDING + title_h + (SPACING if title_h else 0) + geo["bottom_h"]
    br, bb = bl + board_w, bt + board_h
    draw.rounded_rectangle([bl, bt, br, bb], radius, fill=alpha(COL_PAPER, 255 * BOARD_OPACITY),
                           outline=alpha(COL_LINE, 110), width=1)
    # garis panduan titik-titik
    line_gap = 48 * D if board_h >= 200 * D else 40 * D
    inset = 3 * D
    number_font = ImageFont.truetype(FONT_PATH, max(9, int(line_gap * 0.30)))
    y = bt + line_gap
    number = 1
    while y < bb - 4:
        x = bl + inset
        while x < br - inset:
            draw.line([(x, y), (min(x + 2, br - inset), y)], fill=alpha(COL_LINE, 150), width=1)
            x += 7
        # nomor baris: 1 = teks di posisi kursor, 2 = satu enter, 3 = dua enter
        draw.text((bl + inset + 2, y - line_gap * 0.5 - line_gap * 0.15),
                  str(number), font=number_font, fill=alpha(COL_ACCENT, 120))
        y += line_gap
        number += 1
    # sudut kiri papan rata (edgeToEdge)
    draw.rectangle([bl, bt, bl + radius, bt + radius], fill=alpha(COL_PAPER, 255 * BOARD_OPACITY))
    draw.rectangle([bl, bb - radius, bl + radius, bb], fill=alpha(COL_PAPER, 255 * BOARD_OPACITY))
    if flash:
        draw_board_ink(draw, bl, bt, br, line_gap, ink_line)
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
        draw.rounded_rectangle([bl + 6 * D, chip_top, bl + 6 * D + (box[2] - box[0]) + pad * 2,
                                chip_top + chip_h], chip_h / 2, fill=alpha(COL_ACCENT, 240))
        draw.text((bl + 6 * D + pad - box[0], chip_top + (chip_h - (box[3] - box[1])) / 2 - box[1]),
                  flash, font=font, fill=alpha((0x0B, 0x0B, 0x0B), 255))

    # tombol bawah: tepat di bawah papan tulis, di dalam bingkai, tinggi = tombol samping,
    # lebar = tinggi x pengali (diatur sendiri per tombol).
    bottom_top = PANEL_PADDING + title_h + (SPACING if title_h else 0) + SPACING
    y_row = bottom_top
    for row_index, indexes in enumerate(geo["bottom_indexes"]):
        row_h = geo["bottom_rows"][row_index]
        row_widths = [button_width_px(cell, widths[index] if widths and index < len(widths) else 1.0)
                      for index in indexes]
        # Tombol bersebelahan dengan jarak tetap, sama seperti tombol samping (rata kiri).
        x_cursor = bl
        for position, index in enumerate(indexes):
            action = bottom_keys[index]
            width = row_widths[position]
            height = button_height_px(cell, heights[index] if heights and index < len(heights) else 1.0)
            y0 = y_row + (row_h - height) / 2
            draw.rounded_rectangle([x_cursor, y0, x_cursor + width, y0 + height],
                                   max(2, int(RADIUS_DP * D * 0.9)),
                                   fill=alpha(COL_BUTTON, 255 * BUTTON_OPACITY),
                                   outline=alpha(COL_ACCENT, 130), width=1)
            draw_pixel_icon(draw, PIXEL_ART.get(action), x_cursor + width / 2, y0 + height / 2,
                            min(width, height) * 0.62, COL_TEXT)
            x_cursor += width + SPACING
        y_row += row_h + SPACING

    # tombol samping: semuanya persegi (sisi = cell)
    gx = br + SPACING
    gy = bt
    for index, action in enumerate(keys[: geo["rows"] * geo["columns"]]):
        col, row = index % geo["columns"], index // geo["columns"]
        x0 = gx + col * (cell + SPACING)
        y0 = gy + row * (cell + SPACING)
        x1, y1 = x0 + cell, y0 + cell
        draw.rounded_rectangle([x0, y0, x1, y1], max(2, int(RADIUS_DP * D * 0.9)),
                               fill=alpha(COL_BUTTON, 255 * BUTTON_OPACITY),
                               outline=alpha(COL_ACCENT, 130), width=1)
        draw_pixel_icon(draw, PIXEL_ART.get(action), (x0 + x1) / 2, (y0 + y1) / 2,
                        cell * 0.62, COL_TEXT)

    # penanda pegangan: bilah judul (pindah) dan sudut kanan bawah (ubah ukuran)
    if marks and "drag" in marks and title_h:
        draw.rectangle([2, 2, panel_w - 3, 2 + title_h + PANEL_PADDING],
                       outline=alpha(COL_ACCENT, 210), width=1)
        arrow_y = PANEL_PADDING + title_h / 2
        arrow_x = panel_w - PANEL_PADDING - int(18 * D) - int(30 * D)
        draw.line([(arrow_x, arrow_y), (arrow_x + int(22 * D), arrow_y)],
                  fill=alpha(COL_ACCENT, 230), width=2)
        for dx in (0, int(22 * D)):
            direction = 1 if dx == 0 else -1
            draw.line([(arrow_x + dx, arrow_y), (arrow_x + dx + direction * 5, arrow_y - 5)],
                      fill=alpha(COL_ACCENT, 230), width=2)
            draw.line([(arrow_x + dx, arrow_y), (arrow_x + dx + direction * 5, arrow_y + 5)],
                      fill=alpha(COL_ACCENT, 230), width=2)
    if marks and "corner" in marks:
        step = max(2, int(2 * D))
        corner_x = panel_w - int(3 * D)
        corner_y = panel_h - int(3 * D)
        for index in range(3):
            length = step * (index * 2 + 1)
            offset = int(2 * D) + step * (2 - index) * 2
            draw.line([(corner_x, corner_y - offset), (corner_x - length, corner_y - offset)],
                      fill=alpha(COL_ACCENT, 235), width=step)
            draw.line([(corner_x - offset, corner_y), (corner_x - offset, corner_y - length)],
                      fill=alpha(COL_ACCENT, 235), width=step)
    return img


def section(canvas, y, caption, sub=None):
    draw = ImageDraw.Draw(canvas)
    draw.text((40, y), caption, font=LABEL_FONT, fill=alpha(COL_ACCENT, 255))
    if sub:
        draw.text((40, y + 22), sub, font=SMALL_FONT, fill=alpha(COL_ACCENT, 150))
        return y + 44
    return y + 26


def screen_with_panel(panel, panel_left, panel_top, area_h):
    """Area keyboard: panel Penboard yang sudah dipindah/diubah ukurannya."""
    area_w = PANEL_W
    block = Image.new("RGBA", (area_w, max(area_h, panel_top + panel.height)), (0x1E, 0x1E, 0x1E, 255))
    bd = ImageDraw.Draw(block)
    bd.rectangle([0, 0, area_w - 1, block.height - 1], outline=alpha(COL_ACCENT, 60), width=1)
    bd.text((12, 8), "area keyboard", font=SMALL_FONT, fill=alpha(COL_ACCENT, 130))
    block.alpha_composite(panel, (panel_left, panel_top))
    return block


def main():
    baseline = draw_panel(3, DEFAULT_KEYS, flash="\u2713 halo", pending=0.55)
    wide_grid = draw_panel(3, DEFAULT_KEYS, columns=4)
    small_cells = draw_panel(3, DEFAULT_KEYS, cell_dp=32,
                             widths=(1, 1, 1, 2, 2), heights=(1, 1, 1, 1, 1.5))
    stretched = draw_panel(3, DEFAULT_KEYS, frame=False)
    moved = draw_panel(3, DEFAULT_KEYS, cell_dp=32, panel_width=int(PANEL_W * 0.68),
                       marks=("drag", "corner"), bottom_keys=BOTTOM_KEYS[:3])
    longer = draw_panel(5, DEFAULT_KEYS)
    ime = draw_panel(3, DEFAULT_KEYS, flash="\u2713 halo", pending=0.55)
    second_line = draw_panel(3, DEFAULT_KEYS, flash="\u2713 kata", ink_line=1)
    # 20 tombol atas diminta, tetapi bilah atas hanya satu baris: yang muat 11.
    many_bottom = draw_panel(3, DEFAULT_KEYS, cell_dp=32,
                             bottom_keys=(BOTTOM_KEYS * 4)[:20])
    five_rows = draw_panel(3, DEFAULT_KEYS, rows=5)

    pad = 40
    width = PANEL_W + pad * 2
    header = 66
    panels = [
        ("1. Keyboard Penboard - 3 kolom x 3 baris (bawaan)",
         "semua tombol samping persegi; tinggi area tulis = 3 x sisi tombol",
         ("field", baseline)),
        ("2. Empat kolom tombol (baris = baris area tulis)",
         "kolom 1-6; tombol rata atas dengan area tulis, ukurannya menyesuaikan",
         ("plain", wide_grid)),
        ("3. Ukuran tombol 32 dp, tombol atas 2x lebar 1,5x tinggi",
         "Pengaturan > Tombol papan (ukuran) dan editor tiap tombol bawah (lebar/tinggi)",
         ("plain", small_cells)),
        ("4. Area tulis ditambah jadi 5 baris (bawaan 3 baris)",
         "baris tombol samping otomatis ikut jadi 5, rata atas dengan area tulis",
         ("plain", longer)),
        ("5. Papan dipindah & dilebarkan (tarik sudut)",
         "bilah judul = pegangan pindah, sudut kanan bawah = pegangan ubah ukuran",
         ("screen", moved)),
        ("6. Keyboard Penboard tampil di atas aplikasi lain",
         "papan tulis menempel sebagai keyboard di aplikasi apa pun",
         ("ime", ime)),
        ("7. Bingkai dimatikan - tombol minimize tetap ada",
         "bilah kecil 18 dp di kanan atas papan (Pengaturan > Jendela papan)",
         ("plain", stretched)),
        ("8. Baris ke-2 = teks satu baris di bawah kursor",
         "baris ke-3 = dua enter, baris ke-1 = tepat di posisi kursor",
         ("field2", second_line)),
        ("9. Tombol atas satu baris: 11 dari 20 yang muat tampil",
         "rata kiri, jarak seragam; sisanya tidak ditampilkan, ruang kanan kosong",
         ("plain", many_bottom)),
        ("10. Kolom tombol 3 x 5 - area tulis tetap 3 baris",
         "baris tombol bisa mengikuti area tulis atau diatur sendiri 1-6",
         ("plain", five_rows)),
    ]
    canvas_heights = [header]
    for _, _, (kind, item) in panels:
        # 44 px untuk judul+keterangan contoh, 34 px jarak ke contoh berikutnya.
        extra = 44 + 34
        if kind in ("field", "field2"):
            canvas_heights.append(70 + item.height + extra)
        elif kind == "ime":
            canvas_heights.append(APP_MOCK_H + item.height + extra)
        elif kind == "screen":
            canvas_heights.append(int(3 * LINE_GAP_DP * D) + int(3 * D) * 2 + int(22 * D) + 120 + extra)
        else:
            canvas_heights.append(item.height + extra)
    canvas = Image.new("RGBA", (width, sum(canvas_heights) + 40), alpha((0x12, 0x12, 0x12), 255))
    draw = ImageDraw.Draw(canvas)
    draw.text((pad, 18), "Penboard 3.5.5 - tombol atas selalu satu baris",
              font=ImageFont.truetype(FONT_BOLD, 17), fill=alpha(COL_ACCENT, 255))
    draw.text((pad, 42), "rata kiri, jarak seragam; sisa ruang di kanan dibiarkan kosong",
              font=SMALL_FONT, fill=alpha(COL_ACCENT, 150))

    y = header
    for caption, sub, (kind, img) in panels:
        y = section(canvas, y, caption, sub)
        if kind == "screen":
            block = screen_with_panel(img, PANEL_W - img.width - 18, 70, int(3 * LINE_GAP_DP * D) + 150)
            canvas.alpha_composite(block, (pad, y))
            y += block.height + 34
        elif kind in ("field", "field2"):
            box_h = 44 if kind == "field" else 62
            strip = Image.new("RGBA", (PANEL_W, box_h + 8 + img.height), (0x1A, 0x1A, 0x1A, 255))
            sd = ImageDraw.Draw(strip)
            sd.rounded_rectangle([8, 8, PANEL_W - 8, 8 + box_h], 8, fill=(0x2A, 0x2A, 0x2A, 255),
                                 outline=alpha(COL_ACCENT, 60), width=1)
            if kind == "field":
                sd.text((24, 18), "Halo, aku menulis dengan M-Pen...", font=LABEL_FONT,
                        fill=alpha(COL_ACCENT, 230))
            else:
                # kursor berada di baris pertama; kata dari baris ke-2 papan mendarat di bawahnya
                sd.text((24, 18), "Halo, aku menulis dengan M-Pen", font=LABEL_FONT,
                        fill=alpha(COL_ACCENT, 230))
                sd.line([(PANEL_W - 40, 18), (PANEL_W - 40, 36)], fill=alpha(COL_ACCENT, 200), width=2)
                sd.text((24, 40), "kata\u21b5 di baris berikutnya", font=LABEL_FONT,
                        fill=alpha(COL_ACCENT, 150))
            strip.alpha_composite(img, (0, box_h + 8))
            canvas.alpha_composite(strip, (pad, y))
            y += strip.height + 34
        elif kind == "ime":
            block = Image.new("RGBA", (PANEL_W, APP_MOCK_H + img.height), (0x2A, 0x2A, 0x2A, 255))
            bd = ImageDraw.Draw(block)
            bd.rounded_rectangle([24, 24, PANEL_W - 24, 46], 8, fill=(0x33, 0x33, 0x33, 255),
                                 outline=alpha(COL_ACCENT, 70), width=1)
            bd.text((36, 30), "aplikasi lain", font=LABEL_FONT, fill=alpha(COL_ACCENT, 170))
            for i in range(5):
                bd.rounded_rectangle([24, 70 + i * 34, PANEL_W - 24 - (i % 3) * 90, 88 + i * 34], 6,
                                     fill=(0x3E, 0x3E, 0x3E, 255))
            bd.line([(0, APP_MOCK_H), (PANEL_W, APP_MOCK_H)], fill=alpha(COL_ACCENT, 60), width=1)
            block.alpha_composite(img, (0, APP_MOCK_H))
            canvas.alpha_composite(block, (pad, y))
            y += block.height + 34
        else:
            canvas.alpha_composite(img, (pad, y))
            y += img.height + 34
    canvas.convert("RGB").save("docs/preview.png", optimize=True)
    print("docs/preview.png", canvas.size)


PIXEL_ART = load_pixel_art()
BOTTOM_KEYS = load_bottom_keys()[:5]

if __name__ == "__main__":
    import os
    os.makedirs("docs", exist_ok=True)
    main()
