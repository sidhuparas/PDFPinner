package com.parassidhu.pdfpin

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import droidninja.filepicker.FilePickerBuilder
import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.utils.FilePickerUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(){

    private var docPaths = ArrayList<String>()
    private val listItems = ArrayList<ListItem>()
    private var image: Int = 0
    private var num = 0
    private var dataAdapter: DataAdapter? = null

    private val isOreo: Boolean
        get() = Build.VERSION.SDK_INT > 25

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        title = ""
        initViews()
        initAds()
        pinDynamicShortcut()
        // Check if the app is opened using long-press shortcut menu
        try {
            val intent = intent
            if (intent.extras != null) {
                if (intent.extras!!.getString(SHORTCUT_KEY) == SHORTCUT_VALUE)
                    chooseBtn!!.callOnClick()
            }
        } catch (e: Exception) {
        }
        FilePickerUtils.notifyMediaStore(this, "/sdcard/")
    }

    //Shows the choose files picker
    private fun choosePDF() {
        val pdf = arrayOf(".pdf")
        FilePickerBuilder.instance
                .addFileSupport("PDF", pdf)
                .setActivityTheme(R.style.LibAppTheme)
                .enableDocSupport(false)
                .enableSelectAll(true)
                .pickFile(this@MainActivity)
    }

    private fun checkPerm() {
        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!hasPermissions(this, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        } else {
            choosePDF()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                   permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            choosePDF()
        } else {
            Toast.makeText(this,
                    """Permissions for accessing External Storage not granted! To choose a PDF file, it's required.
                            "Please go to Settings->Apps to grant Storage permission.""", Toast.LENGTH_SHORT).show()
        }
    }

    //Initializes all the mess. btnPDFx represents both icons.
    private fun initViews() {
        file_list.layoutManager = LinearLayoutManager(this)
        image = R.drawable.pdf

        chooseBtn.setOnClickListener { checkPerm() }
        toggle(0)

        pinFiles.setOnClickListener {
            if (!isOreo) {
                for (i in listItems.indices) {
                    addShortcut(listItems[i].path.toString(), listItems[i].name.toString())
                }
                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
            } else {
                oneByOne()
            }
        }

        blueIcon.setOnCheckedChangeListener { _, _ ->
            image = if (blueIcon.isChecked)
                R.drawable.pdf
            else
                R.drawable.pdf2
        }
    }

    private fun oneByOne() {
        // Could've used For-Loop but purposefully didn't do so
        if (num < listItems.size - 1) {
            if (listItems.size > 1) {
                pinFiles!!.setText(R.string.pin_next)
                addShortcutInOreo(listItems[num].path.toString(), listItems[num].name.toString())
                num++
            } else {
                addShortcutInOreo(listItems[num].path.toString(), listItems[num].name.toString())
                successMessage()
            }
        } else if (num == listItems.size - 1) {
            pinFiles!!.text = "Pin File(s)"
            addShortcutInOreo(listItems[num].path.toString(), listItems[num].name.toString())
            num = 0
            successMessage()
        }
    }

    private fun successMessage() {
        Toast.makeText(this, "All selected files have been pinned!", Toast.LENGTH_SHORT).show()
    }

    //Custom toggle to set views' visibility
    private fun toggle(`val`: Int) {
        if (`val` == 0) {
            radios.visibility = View.GONE
            btnPDF1!!.visibility = View.GONE
            btnPDF2!!.visibility = View.GONE
            pinFiles!!.visibility = View.GONE
            centerInfo!!.visibility = View.VISIBLE
            dev!!.visibility = View.GONE
        } else {
            radios!!.visibility = View.VISIBLE
            btnPDF1!!.visibility = View.VISIBLE
            btnPDF2!!.visibility = View.VISIBLE
            pinFiles!!.visibility = View.VISIBLE
            centerInfo!!.visibility = View.GONE
            dev!!.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FilePickerConst.REQUEST_CODE_DOC -> if (resultCode == Activity.RESULT_OK && data != null) {
                docPaths = ArrayList()
                docPaths.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS))
            }
        }

        listItems.clear()
        for (i in docPaths.indices) {
            val path = docPaths[i]
            // Example: root/DCIM/Pictures/IMG_8004.JPG
            val name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."))
            listItems.add(ListItem(path, name))
        }
        addDataToList(listItems)
    }

    //Adds the selected PDFs to RecyclerView
    private fun addDataToList(listItems: ArrayList<ListItem>) {
        num = 0
        pinFiles.text = "Pin File(s)"
        dataAdapter = DataAdapter(this, listItems)
        file_list.adapter = dataAdapter

        // Show Oreo specific information
        if (isOreo)
            info!!.visibility = View.VISIBLE

        pinInfo!!.visibility = View.VISIBLE

        if (listItems.size != 0)
            toggle(1)
        else {
            toggle(0)
            info!!.visibility = View.GONE
            pinInfo!!.visibility = View.GONE
            dev!!.visibility = View.GONE
        }
    }

    private fun addShortcut(path1: String, pdfName: String) {
        val file = File(path1)
        if (Build.VERSION.SDK_INT<26) {
            if (file.exists()) {
                val path = Uri.fromFile(file)
                val shortcutIntent = Intent(Intent.ACTION_VIEW)
                shortcutIntent.setDataAndTypeAndNormalize(path, "application/pdf")

                val addIntent = Intent()
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, pdfName)
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(applicationContext, image))
                addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
                applicationContext.sendBroadcast(addIntent)
            } else {
                errorMessage(null)
            }
        }
    }

    private fun addShortcutInOreo(path1: String, pdfName: String) {
        try {
            val file = File(path1)
            val pdfIntent = Intent(Intent.ACTION_VIEW)
            pdfIntent.setDataAndType(Uri.fromFile(file), "application/pdf")

            if (Build.VERSION.SDK_INT > 25) {
                val shortcutManager = getSystemService(ShortcutManager::class.java)

                // Check which icon is to be pinned
                image = if (blueIcon.isChecked)
                    R.drawable.pdf
                else
                    R.drawable.pdf2

                // Create a shortcut
                val shortcut = ShortcutInfo.Builder(this, pdfName)
                        .setShortLabel(pdfName)
                        .setLongLabel(pdfName)
                        .setIcon(Icon.createWithResource(this, image))
                        .setIntent(pdfIntent)
                        .build()

                shortcutManager.requestPinShortcut(shortcut, null)
            }
        } catch (e: Exception) {
            errorMessage(e)
        }

    }

    private fun errorMessage(e: Exception?) {
        if (e != null)
            Toast.makeText(this, "Some error occurred: " + e.message, Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(this, "Some error occurred!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        // Rate and Review is clicked
        if (id == R.id.action_settings) {
            val uri = Uri.parse("market://details?id=" + applicationContext.packageName)
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + applicationContext.packageName)))
            }

            return true
        } else if (id == R.id.privacy) {
            val uri = Uri.parse("https://docs.google.com/document/d/1WU1hg3PmqMPhVEQuS-Um4mgIMTc9L5KhZZTC6gxEwao/edit")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(intent)
            } catch (ignored: Exception) {
            }

        }

        return super.onOptionsItemSelected(item)
    }

    private fun initAds() {
        MobileAds.initialize(this, BuildConfig.BANNER_KEY)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    // This is a Nougat specific shortcut which is shown on long press of app shortcut
    private fun pinDynamicShortcut() {
        // If it's Nougat
        if (Build.VERSION.SDK_INT > 24) {
            // Get instance of ShortcutManager Service
            val shortcutManager = getSystemService(ShortcutManager::class.java)

            val intent = Intent(Intent.ACTION_MAIN, Uri.EMPTY, this, MainActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

            // This extra will be later used to check if there exists a shortcut or no
            intent.putExtra(SHORTCUT_KEY, SHORTCUT_VALUE)

            // Create a shortcut
            val shortcut = ShortcutInfo.Builder(this, SHORTCUT_ID)
                    .setShortLabel("Choose File(s)")
                    .setIcon(Icon.createWithResource(this, R.drawable.choose_files))
                    .setIntent(intent)
                    .build()

            // Ask the service to pin the shortcut
            shortcutManager.dynamicShortcuts = listOf(shortcut)
        }
    }

    companion object {

        const val SHORTCUT_VALUE = "yes"
        const val SHORTCUT_KEY = "shortcut"
        const val SHORTCUT_ID = "id1"

        fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
            }
            return true
        }
    }
}
