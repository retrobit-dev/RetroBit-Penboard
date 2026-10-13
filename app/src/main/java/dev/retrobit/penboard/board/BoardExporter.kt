package dev.retrobit.penboard.board

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dev.retrobit.penboard.data.Colors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Menyimpan isi papan menjadi berkas PNG di Galeri (tanpa izin tambahan di Android 10+). */
object BoardExporter {

    fun renderToBitmap(view: BoardView): Bitmap {
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Colors.withOpacity(view.config.boardBgColor, 100))
        view.draw(canvas)
        return bitmap
    }

    fun saveToGallery(context: Context, view: BoardView): Uri? {
        val bitmap = renderToBitmap(view)
        val name = "penboard-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".png"
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(context, bitmap, name)
        } else {
            saveLegacy(context, bitmap, name)
        }
        bitmap.recycle()
        return uri
    }

    private fun saveScoped(context: Context, bitmap: Bitmap, name: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Penboard")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, bitmap: Bitmap, name: String): Uri? {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Penboard")
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, name)
        return try {
            file.outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }
}
