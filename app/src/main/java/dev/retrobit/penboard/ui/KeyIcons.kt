package dev.retrobit.penboard.ui

import android.graphics.Path
import android.graphics.RectF
import dev.retrobit.penboard.data.KeyAction

/**
 * Ikon tombol papan Penboard.
 *
 * Semua ikon digambar dari kode dalam kotak 24x24 (garis tebal, sudut membulat) supaya
 * tampilannya seragam dengan tema retro cyberpunk: monokrom, garis tipis, tanpa warna.
 * Aksi yang tidak punya ikon (mis. [KeyAction.TEXT]) tetap digambar memakai label teks.
 */
object KeyIcons {

    /** Ukuran kotak gambar ikon (satuan asli sebelum diperbesar). */
    const val VIEWPORT = 24f

    /** Bentuk ikon: jalur gambar dan apakah diisi penuh atau hanya garis. */
    data class Glyph(val path: Path, val filled: Boolean)

    private val cache = HashMap<KeyAction, Glyph?>()

    fun glyph(action: KeyAction): Glyph? = cache.getOrPut(action) { build(action) }

    /** Ikon tombol minimize (versi vektor): satu garis mendatar, sama seperti versi pixel art. */
    val minimize: Glyph by lazy { strokePath { moveTo(6f, 12f); lineTo(18f, 12f) } }

    private fun strokePath(block: Path.() -> Unit): Glyph = Glyph(Path().apply(block), filled = false)

    private fun fillPath(block: Path.() -> Unit): Glyph = Glyph(Path().apply(block), filled = true)

    private fun build(action: KeyAction): Glyph? = when (action) {
        // ---------------------------------------------------------- penempatan kursor
        KeyAction.CURSOR_LEFT -> fillPath { moveTo(16f, 4f); lineTo(7f, 12f); lineTo(16f, 20f); close() }
        KeyAction.CURSOR_RIGHT -> fillPath { moveTo(8f, 4f); lineTo(17f, 12f); lineTo(8f, 20f); close() }
        KeyAction.CURSOR_UP -> fillPath { moveTo(4f, 16f); lineTo(12f, 7f); lineTo(20f, 16f); close() }
        KeyAction.CURSOR_DOWN -> fillPath { moveTo(4f, 8f); lineTo(12f, 17f); lineTo(20f, 8f); close() }
        KeyAction.CURSOR_HOME -> strokePath {
            moveTo(5f, 5f); lineTo(5f, 19f)
            moveTo(19f, 12f); lineTo(10f, 12f)
            moveTo(14f, 8f); lineTo(10f, 12f); lineTo(14f, 16f)
        }
        KeyAction.CURSOR_WORD_LEFT -> strokePath {
            moveTo(5f, 5f); lineTo(5f, 19f)
            moveTo(16f, 6f); lineTo(8f, 12f); lineTo(16f, 18f)
        }
        KeyAction.CURSOR_WORD_RIGHT -> strokePath {
            moveTo(19f, 5f); lineTo(19f, 19f)
            moveTo(8f, 6f); lineTo(16f, 12f); lineTo(8f, 18f)
        }
        KeyAction.CURSOR_DOC_START -> strokePath {
            moveTo(5f, 5f); lineTo(19f, 5f)
            moveTo(12f, 19f); lineTo(12f, 9f); lineTo(7f, 13f); moveTo(12f, 9f); lineTo(17f, 13f)
        }
        KeyAction.CURSOR_DOC_END -> strokePath {
            moveTo(5f, 19f); lineTo(19f, 19f)
            moveTo(12f, 5f); lineTo(12f, 15f); lineTo(7f, 11f); moveTo(12f, 15f); lineTo(17f, 11f)
        }
        KeyAction.PAGE_UP -> strokePath {
            moveTo(7f, 11f); lineTo(12f, 6f); lineTo(17f, 11f)
            moveTo(7f, 17f); lineTo(12f, 12f); lineTo(17f, 17f)
        }
        KeyAction.PAGE_DOWN -> strokePath {
            moveTo(7f, 7f); lineTo(12f, 12f); lineTo(17f, 7f)
            moveTo(7f, 13f); lineTo(12f, 18f); lineTo(17f, 13f)
        }
        KeyAction.CURSOR_END -> strokePath {
            moveTo(19f, 5f); lineTo(19f, 19f)
            moveTo(5f, 12f); lineTo(14f, 12f)
            moveTo(10f, 8f); lineTo(14f, 12f); lineTo(10f, 16f)
        }

        // ---------------------------------------------------------- keyboard konvensional
        KeyAction.SPACE -> strokePath {
            moveTo(4f, 10f); lineTo(4f, 15f); lineTo(20f, 15f); lineTo(20f, 10f)
        }
        KeyAction.ENTER -> strokePath {
            moveTo(18f, 5f); lineTo(18f, 14f); lineTo(7f, 14f)
            moveTo(11f, 10f); lineTo(7f, 14f); lineTo(11f, 18f)
        }
        KeyAction.BACKSPACE -> strokePath {
            moveTo(9f, 5f); lineTo(21f, 5f); lineTo(21f, 19f); lineTo(9f, 19f); lineTo(3f, 12f); close()
            moveTo(13f, 9f); lineTo(17f, 15f)
            moveTo(17f, 9f); lineTo(13f, 15f)
        }
        KeyAction.DELETE_WORD -> strokePath {
            // tiga "huruf" yang dicoret = hapus satu kata
            moveTo(5f, 8f); lineTo(5f, 17f)
            moveTo(9f, 8f); lineTo(9f, 17f)
            moveTo(13f, 8f); lineTo(13f, 17f)
            moveTo(3f, 20f); lineTo(19f, 5f)
        }
        KeyAction.DELETE_FORWARD -> strokePath {
            moveTo(15f, 5f); lineTo(3f, 5f); lineTo(3f, 19f); lineTo(15f, 19f); lineTo(21f, 12f); close()
            moveTo(7f, 9f); lineTo(11f, 15f)
            moveTo(11f, 9f); lineTo(7f, 15f)
        }
        KeyAction.TAB -> strokePath {
            moveTo(4f, 5f); lineTo(4f, 19f)
            moveTo(20f, 5f); lineTo(20f, 19f)
            moveTo(4f, 12f); lineTo(15f, 12f)
            moveTo(11f, 8f); lineTo(15f, 12f); lineTo(11f, 16f)
        }

        // ---------------------------------------------------------- papan klip & pilihan
        KeyAction.SELECT_ALL -> strokePath {
            moveTo(3.5f, 8f); lineTo(3.5f, 3.5f); lineTo(8f, 3.5f)
            moveTo(16f, 3.5f); lineTo(20.5f, 3.5f); lineTo(20.5f, 8f)
            moveTo(20.5f, 16f); lineTo(20.5f, 20.5f); lineTo(16f, 20.5f)
            moveTo(8f, 20.5f); lineTo(3.5f, 20.5f); lineTo(3.5f, 16f)
            moveTo(7f, 12f); lineTo(17f, 12f)
        }
        KeyAction.COPY -> strokePath {
            moveTo(9f, 3f); lineTo(21f, 3f); lineTo(21f, 15f); lineTo(17f, 15f)
            moveTo(3f, 9f); lineTo(15f, 9f); lineTo(15f, 21f); lineTo(3f, 21f); close()
        }
        KeyAction.CUT -> strokePath {
            addOval(RectF(4f, 15f, 10f, 21f), Path.Direction.CW)
            addOval(RectF(14f, 15f, 20f, 21f), Path.Direction.CW)
            moveTo(7f, 14f); lineTo(17f, 3f)
            moveTo(17f, 14f); lineTo(7f, 3f)
        }
        KeyAction.SELECT_WORD -> strokePath {
            moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 19f); lineTo(5f, 19f); close()
            moveTo(8f, 9f); lineTo(16f, 9f)
            moveTo(8f, 12f); lineTo(16f, 12f)
            moveTo(8f, 15f); lineTo(13f, 15f)
        }
        KeyAction.TEXT_UNDO -> strokePath {
            moveTo(8f, 8f); lineTo(8f, 5f); lineTo(5f, 8f)
            moveTo(8f, 6f); lineTo(13f, 6f); lineTo(16f, 9f); lineTo(16f, 14f); lineTo(13f, 17f); lineTo(8f, 17f)
            moveTo(6f, 20f); lineTo(18f, 20f)
        }
        KeyAction.TEXT_REDO -> strokePath {
            moveTo(16f, 8f); lineTo(16f, 5f); lineTo(19f, 8f)
            moveTo(16f, 6f); lineTo(11f, 6f); lineTo(8f, 9f); lineTo(8f, 14f); lineTo(11f, 17f); lineTo(16f, 17f)
            moveTo(6f, 20f); lineTo(18f, 20f)
        }
        KeyAction.SHIFT_TAB -> strokePath {
            moveTo(6f, 5f); lineTo(6f, 19f)
            moveTo(18f, 8f); lineTo(10f, 8f); lineTo(13f, 5f); moveTo(10f, 8f); lineTo(13f, 11f)
            moveTo(18f, 16f); lineTo(12f, 16f)
        }
        KeyAction.ESCAPE -> strokePath {
            moveTo(7f, 7f); lineTo(17f, 17f)
            moveTo(17f, 7f); lineTo(7f, 17f)
        }
        KeyAction.EDITOR_ACTION -> strokePath {
            moveTo(6f, 7f); lineTo(6f, 16f); lineTo(12f, 16f)
            moveTo(10f, 13f); lineTo(18f, 13f); lineTo(15f, 10f); moveTo(18f, 13f); lineTo(15f, 16f)
        }
        KeyAction.PASTE -> strokePath {
            moveTo(5f, 7f); lineTo(5f, 21f); lineTo(19f, 21f); lineTo(19f, 7f); lineTo(15f, 7f)
            moveTo(9f, 7f); lineTo(9f, 3f); lineTo(15f, 3f); lineTo(15f, 7f)
            moveTo(9f, 12f); lineTo(15f, 12f)
            moveTo(9f, 16f); lineTo(15f, 16f)
        }

        // ---------------------------------------------------------- papan tulis
        KeyAction.TEXT -> null
        KeyAction.CHAR_PAD -> strokePath {
            addRect(RectF(3f, 3f, 8f, 8f), Path.Direction.CW)
            addRect(RectF(10f, 3f, 15f, 8f), Path.Direction.CW)
            addRect(RectF(17f, 3f, 22f, 8f), Path.Direction.CW)
            addRect(RectF(3f, 10f, 8f, 15f), Path.Direction.CW)
            addRect(RectF(10f, 10f, 15f, 15f), Path.Direction.CW)
            addRect(RectF(17f, 10f, 22f, 15f), Path.Direction.CW)
            moveTo(4f, 20f)
            lineTo(20f, 20f)
        }
        KeyAction.RECOGNIZE -> strokePath {
            // "huruf A + percikan" = ubah tulisan menjadi teks
            moveTo(4f, 19f); lineTo(10f, 5f); lineTo(16f, 19f)
            moveTo(6.5f, 14f); lineTo(13.5f, 14f)
            moveTo(19f, 4f); lineTo(19f, 9f)
            moveTo(16.5f, 6.5f); lineTo(21.5f, 6.5f)
        }
        KeyAction.CLEAR -> strokePath {
            moveTo(6f, 7f); lineTo(18f, 7f)
            moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f)
            moveTo(8f, 7f); lineTo(9f, 20f); lineTo(15f, 20f); lineTo(16f, 7f)
        }
        KeyAction.DELETE_STROKE -> strokePath {
            // penghapus miring di atas garis papan
            moveTo(7f, 16f); lineTo(14f, 6f); lineTo(20f, 11f); lineTo(13f, 21f); close()
            moveTo(3f, 21f); lineTo(17f, 21f)
        }
        KeyAction.UNDO -> curvedArrow(forward = false)
        KeyAction.REDO -> curvedArrow(forward = true)
        KeyAction.CYCLE_PEN -> strokePath {
            moveTo(19f, 5f); lineTo(21f, 7f); lineTo(8f, 20f); lineTo(4f, 20f); lineTo(4f, 16f); close()
            moveTo(16f, 8f); lineTo(18f, 10f)
        }

        // ---------------------------------------------------------- perangkat & jendela
        KeyAction.SYSTEM_HANDWRITING -> strokePath {
            // coretan tangan di atas garis
            moveTo(4f, 19f); lineTo(20f, 19f)
            moveTo(5f, 15f)
            cubicTo(8f, 8f, 10f, 8f, 12f, 13f)
            cubicTo(14f, 18f, 16f, 18f, 19f, 10f)
        }
        KeyAction.KEYBOARD_PICKER -> strokePath {
            // papan tombol kecil: bingkai, tiga tombol pendek, satu tombol panjang
            moveTo(3f, 7f); lineTo(21f, 7f); lineTo(21f, 17f); lineTo(3f, 17f); close()
            moveTo(6f, 10.5f); lineTo(8f, 10.5f)
            moveTo(11f, 10.5f); lineTo(13f, 10.5f)
            moveTo(16f, 10.5f); lineTo(18f, 10.5f)
            moveTo(8f, 14f); lineTo(16f, 14f)
        }
        KeyAction.HIDE -> strokePath {
            moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
            moveTo(6f, 19f); lineTo(18f, 19f)
        }
    }

    /** Panah melengkung untuk batal (undo) dan ulangi (redo). */
    private fun curvedArrow(forward: Boolean): Glyph {
        val path = Path()
        val startAngle = if (forward) 200f else 340f
        val sweep = if (forward) -230f else 230f
        val rect = if (forward) RectF(5f, 5f, 21f, 21f) else RectF(3f, 5f, 19f, 21f)
        path.addArc(rect, startAngle, sweep)
        if (forward) {
            path.moveTo(15f, 5.5f)
            path.lineTo(20.5f, 4.5f)
            path.lineTo(19f, 10f)
        } else {
            path.moveTo(9f, 5.5f)
            path.lineTo(3.5f, 4.5f)
            path.lineTo(5f, 10f)
        }
        return Glyph(path, filled = false)
    }
}
