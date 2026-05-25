package com.parassidhu.pdfpin

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class OpenPinnedPdfActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPinnedPdf()
    }

    private fun openPinnedPdf() {
        val uriString = intent.getStringExtra(ShortcutExtras.EXTRA_URI)
            ?: intent.dataString
        val mimeType = intent.getStringExtra(ShortcutExtras.EXTRA_MIME_TYPE).orEmpty()

        if (uriString.isNullOrBlank()) {
            toast(getString(R.string.shortcut_target_missing))
            finish()
            return
        }

        val fileUri = Uri.parse(uriString)
        val resolvedMimeType = mimeType.ifBlank { contentResolver.getType(fileUri).orEmpty() }.ifBlank { "*/*" }
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, resolvedMimeType)
            clipData = ClipData.newRawUri(intent.getStringExtra(ShortcutExtras.EXTRA_NAME), fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(openIntent)
        } catch (_: SecurityException) {
            toast(getString(R.string.file_permission_missing))
        } catch (_: ActivityNotFoundException) {
            toast(getString(R.string.no_file_viewer))
        } catch (_: Exception) {
            toast(getString(R.string.file_open_failed))
        } finally {
            finish()
        }
    }
}
