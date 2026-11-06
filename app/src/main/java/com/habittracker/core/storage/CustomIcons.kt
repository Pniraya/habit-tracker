package com.habittracker.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.habittracker.core.designsystem.HabitVisuals
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copies a user-picked image into app-private storage and returns the value stored in
 * `habits.icon` (`custom:<file>`). The image is centre-cropped to 256px so a 12 MP photo does not
 * become a 12 MP icon on disk, and no storage permission is needed because the picker hands back a
 * content URI the app can read for the duration of the callback.
 */
object CustomIcons {

    private const val MAX_PX: Int = 256

    fun save(context: Context, uri: Uri): String? = runCatching {
        val bitmap = decodeSquare(context, uri) ?: return null
        val dir = File(context.filesDir, "icons").apply { mkdirs() }
        val name = UUID.randomUUID().toString() + ".png"
        FileOutputStream(File(dir, name)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        HabitVisuals.CUSTOM_PREFIX + name
    }.getOrNull()

    /** The file for a `custom:<file>` value, or null when the value is not a custom icon. */
    fun file(context: Context, icon: String): File? =
        HabitVisuals.customFileName(icon)?.let { File(File(context.filesDir, "icons"), it) }

    private fun decodeSquare(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_PX && bounds.outHeight / (sample * 2) >= MAX_PX) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val side = minOf(decoded.width, decoded.height)
        if (side <= 0) return decoded
        val x = (decoded.width - side) / 2
        val y = (decoded.height - side) / 2
        val cropped = Bitmap.createBitmap(decoded, x, y, side, side)
        if (cropped !== decoded) decoded.recycle()
        return if (cropped.width == MAX_PX) cropped else Bitmap.createScaledBitmap(cropped, MAX_PX, MAX_PX, true).also {
            if (it !== cropped) cropped.recycle()
        }
    }
}
