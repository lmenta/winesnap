package com.winesnap.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** A photo ready for display and upload. */
class PreparedImage(val bitmap: Bitmap, val jpeg: ByteArray)

/**
 * Decodes a picked/captured photo, downscales it so the longest edge is at most
 * [maxEdge] px and re-encodes as JPEG. Phone photos are often 5–15 MB; labels stay
 * perfectly legible at 2048 px and uploads get much faster.
 * ImageDecoder applies EXIF rotation for us.
 */
suspend fun prepareImage(resolver: ContentResolver, uri: Uri, maxEdge: Int = 2048): PreparedImage =
    withContext(Dispatchers.IO) {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > maxEdge) {
                val scale = maxEdge.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt(),
                    (info.size.height * scale).toInt(),
                )
            }
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        PreparedImage(bitmap, out.toByteArray())
    }
