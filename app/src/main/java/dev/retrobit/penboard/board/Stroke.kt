package dev.retrobit.penboard.board

/** Titik goresan dalam koordinat papan (piksel). Murni Kotlin supaya mudah diuji. */
data class InkPoint(val x: Float, val y: Float)

/** Satu goresan pena lengkap dengan gaya gambarnya. */
class Stroke(
    val points: MutableList<InkPoint> = mutableListOf(),
    var color: Int = 0,
    var widthPx: Float = 0f
) {
    val size: Int get() = points.size

    fun isEmpty(): Boolean = points.isEmpty()

    fun last(): InkPoint? = points.lastOrNull()

    fun copy(): Stroke = Stroke(points.toMutableList(), color, widthPx)

    companion object {
        /** Goresan dari daftar titik (dipakai pengujian). */
        fun of(vararg points: InkPoint): Stroke =
            Stroke(points.toMutableList(), color = 0xFF000000.toInt(), widthPx = 4f)
    }
}
