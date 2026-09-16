package com.claudianapolitano.leyla.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Shared plumbing for "pick or shoot a photo, then upload it". Snap Hunt and the
 * journal both do this, so the decoding rules live in one place — a photo picked
 * for the diary should not come out a different size from one picked for a hunt.
 */

/** Compresses a picked shot the way the iOS client does before uploading. */
fun Bitmap.toJpeg(quality: Int = 85): ByteArray =
    ByteArrayOutputStream().use { out ->
        compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }

/**
 * A fresh file in the app's cache for the camera to write into, exposed through
 * the manifest's FileProvider. Null only if the cache directory is unavailable.
 *
 * ACTION_IMAGE_CAPTURE only hands back a thumbnail unless it is given a file to
 * write into, which is far too small to keep in a diary.
 */
fun Context.newCaptureUri(prefix: String = "photo"): Uri? = runCatching {
    val dir = File(cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "$prefix-${System.currentTimeMillis()}.jpg")
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}.getOrNull()

/**
 * Decodes a picked or captured photo, downsampled so a 12-megapixel shot doesn't
 * have to be held in memory whole just to become a ~1600px upload.
 */
fun Context.loadBitmap(uri: Uri, maxDimension: Int = 1600): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()
