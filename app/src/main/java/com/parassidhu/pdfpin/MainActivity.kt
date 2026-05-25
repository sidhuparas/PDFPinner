package com.parassidhu.pdfpin

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.parassidhu.pdfpin.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private data class IconOption(val view: View, val iconResId: Int)

    private lateinit var binding: ActivityMainBinding
    private val listItems = mutableListOf<ListItem>()
    private lateinit var dataAdapter: DataAdapter
    private lateinit var iconOptions: List<IconOption>
    private var iconResId: Int = R.drawable.pdf
    private var nextPinIndex = 0

    private val documentPicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            setItemsFromUris(uris, persistAccess = true)
            toast(getString(R.string.picked_files_added))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        pinDynamicShortcut()
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun init() {
        setSupportActionBar(binding.toolbar)
        title = ""

        dataAdapter = DataAdapter(this, listItems)
        binding.content.fileList.layoutManager = LinearLayoutManager(this)
        binding.content.fileList.adapter = dataAdapter

        binding.content.chooseBtn.setOnClickListener { openDocumentPicker() }
        binding.content.pinFiles.setOnClickListener { pinSelectedFiles() }
        iconOptions = listOf(
            IconOption(binding.content.iconOptionBlue, R.drawable.pdf),
            IconOption(binding.content.iconOptionCoral, R.drawable.pdf2),
            IconOption(binding.content.iconOptionEmerald, R.drawable.pdf3),
            IconOption(binding.content.iconOptionCharcoal, R.drawable.pdf4),
            IconOption(binding.content.iconOptionSand, R.drawable.pdf5),
            IconOption(binding.content.iconOptionGraphite, R.drawable.pdf6)
        )
        iconOptions.forEach { option ->
            option.view.setOnClickListener { selectIcon(option.iconResId) }
        }
        selectIcon(R.drawable.pdf)

        toggleViewsVisibility(showFiles = false)
    }

    private fun selectIcon(selectedIconResId: Int) {
        iconResId = selectedIconResId
        iconOptions.forEach { option ->
            option.view.isSelected = option.iconResId == selectedIconResId
        }
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return

        if (incomingIntent.getStringExtra(ShortcutExtras.SHORTCUT_KEY) == ShortcutExtras.SHORTCUT_VALUE) {
            binding.content.chooseBtn.post { openDocumentPicker() }
            return
        }

        when (incomingIntent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    setItemsFromUris(listOf(uri), persistAccess = false)
                    toast(getString(R.string.share_import_warning))
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    incomingIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    incomingIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }.orEmpty()

                if (uris.isNotEmpty()) {
                    setItemsFromUris(uris, persistAccess = false)
                    toast(getString(R.string.share_import_warning))
                }
            }
        }
    }

    private fun openDocumentPicker() {
        documentPicker.launch(arrayOf("*/*"))
    }

    private fun setItemsFromUris(uris: List<Uri>, persistAccess: Boolean) {
        listItems.clear()
        uris.distinct().forEach { uri ->
            maybePersistReadAccess(uri, persistAccess)
            val displayName = resolveDisplayName(uri)
            val mimeType = resolveMimeType(uri, displayName)
            listItems.add(
                ListItem(
                    uriString = uri.toString(),
                    name = displayName,
                    mimeType = mimeType,
                    typeLabel = buildTypeLabel(mimeType, displayName)
                )
            )
        }

        nextPinIndex = 0
        binding.content.pinFiles.setText(R.string.pin)
        dataAdapter.notifyDataSetChanged()
        toggleViewsVisibility(showFiles = listItems.isNotEmpty())
    }

    private fun maybePersistReadAccess(uri: Uri, persistAccess: Boolean) {
        if (!persistAccess) return

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some document providers do not offer persistable access. Verification still works
            // for the current session, but launcher shortcuts may stop working later.
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val displayName = cursor.getString(nameIndex)?.trim().orEmpty()
                if (displayName.isNotEmpty()) {
                    return displayName.substringBeforeLast('.', displayName)
                }
            }
        }

        val fallback = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.', "")
        return fallback?.takeIf { it.isNotBlank() } ?: "File"
    }

    private fun resolveMimeType(uri: Uri, displayName: String): String {
        contentResolver.getType(uri)?.let { return it }

        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (extension.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let { return it }
        }

        return "*/*"
    }

    private fun buildTypeLabel(mimeType: String, displayName: String): String {
        val extension = displayName.substringAfterLast('.', "").uppercase()
        val extensionLabel = extension.takeIf { it.isNotBlank() }
        val category = mimeType.substringBefore('/').replaceFirstChar { it.uppercase() }

        return when {
            mimeType == "*/*" && extensionLabel != null -> extensionLabel
            mimeType == "*/*" -> getString(R.string.file_type_generic)
            extensionLabel != null -> "$extensionLabel · $category"
            else -> category
        }
    }

    private fun pinSelectedFiles() {
        if (listItems.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            if (!shortcutManager.isRequestPinShortcutSupported) {
                toast(getString(R.string.no_shortcut_support))
                return
            }
            pinOneByOne(shortcutManager)
            return
        }

        listItems.forEach { item ->
            sendLegacyShortcutBroadcast(item)
        }
        toast(getString(R.string.pin_success_legacy))
    }

    private fun pinOneByOne(shortcutManager: ShortcutManager) {
        val item = listItems.getOrNull(nextPinIndex) ?: return
        shortcutManager.requestPinShortcut(buildPinnedShortcut(item), null)

        if (listItems.size == 1 || nextPinIndex == listItems.lastIndex) {
            nextPinIndex = 0
            binding.content.pinFiles.setText(R.string.pin)
            toast(getString(R.string.all_files_pinned))
        } else {
            nextPinIndex += 1
            binding.content.pinFiles.setText(R.string.pin_next)
        }
    }

    private fun buildPinnedShortcut(item: ListItem): ShortcutInfo {
        val launchIntent = buildPinnedShortcutIntent(item)
        return ShortcutInfo.Builder(this, UUID.randomUUID().toString())
            .setShortLabel(item.name.take(25))
            .setLongLabel(item.name)
            .setIcon(Icon.createWithResource(this, iconResId))
            .setIntent(launchIntent)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun sendLegacyShortcutBroadcast(item: ListItem) {
        val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, buildPinnedShortcutIntent(item))
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.name)
        addIntent.putExtra(
            Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource.fromContext(applicationContext, iconResId)
        )
        sendBroadcast(addIntent)
    }

    private fun buildPinnedShortcutIntent(item: ListItem): Intent {
        return Intent(this, OpenPinnedPdfActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(item.uriString)
            clipData = ClipData.newRawUri(item.name, Uri.parse(item.uriString))
            putExtra(ShortcutExtras.EXTRA_URI, item.uriString)
            putExtra(ShortcutExtras.EXTRA_NAME, item.name)
            putExtra(ShortcutExtras.EXTRA_MIME_TYPE, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun toggleViewsVisibility(showFiles: Boolean) {
        val content = binding.content
        val visibility = if (showFiles) View.VISIBLE else View.GONE
        content.filesPanel.visibility = visibility
        content.appearancePanel.visibility = visibility
        content.pinFiles.visibility = visibility
        content.pinNote.visibility = visibility
        content.dev.visibility = visibility
        content.centerInfo.visibility = if (showFiles) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val marketIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    )
                }

                try {
                    startActivity(marketIntent)
                } catch (_: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                        )
                    )
                }
                return true
            }

            R.id.privacy -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url))))
                } catch (_: ActivityNotFoundException) {
                    toast(getString(R.string.link_open_failed))
                }
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun pinDynamicShortcut() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val shortcutManager = getSystemService(ShortcutManager::class.java)
        val shortcutIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(ShortcutExtras.SHORTCUT_KEY, ShortcutExtras.SHORTCUT_VALUE)
        }

        val shortcut = ShortcutInfo.Builder(this, ShortcutExtras.CHOOSE_FILES_SHORTCUT_ID)
            .setShortLabel(getString(R.string.choose_shortcut))
            .setIcon(Icon.createWithResource(this, R.drawable.choose_files))
            .setIntent(shortcutIntent)
            .build()

        shortcutManager.dynamicShortcuts = listOf(shortcut)
    }
}
