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

        if (uriString.isNullOrBlank()) {
            toast(getString(R.string.shortcut_target_missing))
            finish()
            return
        }

        val pdfUri = Uri.parse(uriString)
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            clipData = ClipData.newRawUri(intent.getStringExtra(ShortcutExtras.EXTRA_NAME), pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(openIntent)
        } catch (_: SecurityException) {
            toast(getString(R.string.pdf_permission_missing))
        } catch (_: ActivityNotFoundException) {
            toast(getString(R.string.no_pdf_viewer))
        } catch (_: Exception) {
            toast(getString(R.string.pdf_open_failed))
        } finally {
            finish()
        }
    }
}
