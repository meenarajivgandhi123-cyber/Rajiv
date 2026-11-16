package com.example.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object PdfFileHelper {
  fun saveToDownloadsIfSupported(
    context: Context,
    fileName: String,
    bytes: ByteArray,
  ): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val resolver = context.contentResolver
    val values = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
      put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
      put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SPF")
      put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
      resolver.openOutputStream(uri)?.use { out ->
        out.write(bytes)
        out.flush()
      }
      values.clear()
      values.put(MediaStore.MediaColumns.IS_PENDING, 0)
      resolver.update(uri, values, null, null)
      uri
    } catch (_: Exception) {
      resolver.delete(uri, null, null)
      null
    }
  }

  fun writeToUri(context: Context, uri: Uri, bytes: ByteArray): Boolean {
    return try {
      context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(bytes)
        out.flush()
      } != null
    } catch (_: Exception) {
      false
    }
  }

  fun saveToCacheAndGetShareUri(
    context: Context,
    fileName: String,
    bytes: ByteArray,
  ): Uri {
    val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
    val file = File(pdfDir, fileName)
    file.writeBytes(bytes)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }

  fun launchViewOrSharePdf(context: Context, uri: Uri, shareTitle: String, shareOnly: Boolean = false) {
    if (!shareOnly) {
      val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      if (viewIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(Intent.createChooser(viewIntent, shareTitle))
        return
      }
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, shareTitle)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, shareTitle))
  }
}
