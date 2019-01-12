package com.parassidhu.pdfpin

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.getIntent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.util.CollectionUtils.listOf
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private var chooseBtn: Button? = null
    private var pinFiles: Button? = null
    private var btnPDF1: Button? = null
    private var btnPDF2: Button? = null
    private var blue: RadioButton? = null
    private var docPaths = ArrayList<String>()
    private val listItems = ArrayList<ListItem>()
    private var image: Int = 0
    private var num = 0
    private var file_list: RecyclerView? = null
    private var dataAdapter: DataAdapter? = null
    private var info: TextView? = null
    private var pinInfo: TextView? = null
    private var centerInfo: TextView? = null
    private var dev: TextView? = null
    private var radios: RadioGroup? = null

    private val isOreo: Boolean
        get() = if (Build.VERSION.SDK_INT > 25)
            true
        else
            false

    protected fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setTitle("")
        initViews()
        initAds()
        pinDynamicShortcut()

        // Check if the app is opened using long-press shortcut menu
        try {
            val intent = getIntent()
            if (intent.getExtras() != null) {
                if (intent.getExtras()!!.getString(SHORTCUT_KEY) == SHORTCUT_VALUE)
                    chooseBtn!!.callOnClick()
            }
        } catch (e: Exception) {
        }

    }

    //Shows the choose files picker
    private fun choosePDF() {
        val pdf = arrayOf(".pdf")
        FilePickerBuilder.getInstance()
                .addFileSupport("PDF", pdf)
                .setActivityTheme(R.style.LibAppTheme)
                .enableDocSupport(false)
                .enableSelectAll(true)
                .pickFile(this@MainActivity)
    }

    private fun checkPerm() {
        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!hasPermissions(this, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        } else {
            choosePDF()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int,
                                   permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            choosePDF()
        } else {
            Toast.makeText(this, "Permissions for accessing External Storage not granted! To choose a PDF file, it's required." + "Please go to Settings->Apps to grant Storage permission.", Toast.LENGTH_SHORT).show()
        }
    }

    //Initializes all the mess. btnPDFx represents both icons.
    private fun initViews() {
        chooseBtn = findViewById(R.id.chooseBtn)
        file_list = findViewById(R.id.file_list)
        file_list!!.setLayoutManager(LinearLayoutManager(this))
        pinFiles = findViewById(R.id.pinFiles)
        blue = findViewById(R.id.blueIcon)
        image = R.drawable.pdf

        chooseBtn!!.setOnClickListener { checkPerm() }

        info = findViewById(R.id.info)
        pinInfo = findViewById(R.id.pinInfo)
        radios = findViewById(R.id.radios)
        btnPDF1 = findViewById(R.id.btnPDF1)
        btnPDF2 = findViewById(R.id.btnPDF2)
        centerInfo = findViewById(R.id.centerInfo)
        dev = findViewById(R.id.dev)
        toggle(0)

        pinFiles!!.setOnClickListener {
            if (!isOreo) {
                for (i in listItems.indices) {
                    addShortcut(listItems[i].path, listItems[i].name)
                }
                Toast.makeText(this@MainActivity, "Success!", Toast.LENGTH_SHORT).show()
            } else {
                oneByOne()
            }
        }

        blue!!.setOnCheckedChangeListener { compoundButton, b ->
            if (blue!!.isChecked)
                image = R.drawable.pdf
            else
                image = R.drawable.pdf2
        }
    }

    private fun oneByOne() {
        // Could've used For-Loop but purposefully didn't do so
        if (num < listItems.size - 1) {
            if (listItems.size > 1) {
                pinFiles!!.setText(R.string.pin_next)
                addShortcutInOreo(listItems[num].path, listItems[num].name)
                num++
            } else {
                addShortcutInOreo(listItems[num].path, listItems[num].name)
                successMessage()
            }
        } else if (num == listItems.size - 1) {
            pinFiles!!.text = "Pin File(s)"
            addShortcutInOreo(listItems[num].path, listItems[num].name)
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
            radios!!.visibility = View.GONE
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

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
        pinFiles!!.text = "Pin File(s)"
        dataAdapter = DataAdapter(this, listItems)
        file_list!!.setAdapter(dataAdapter)

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
        if (file.exists()) {
            val path = Uri.fromFile(file)
            val shortcutIntent = Intent(Intent.ACTION_VIEW)
            shortcutIntent.setDataAndTypeAndNormalize(path, "application/pdf")

            val addIntent = Intent()
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, pdfName)
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this.getApplicationContext(), image))
            addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
            getApplicationContext().sendBroadcast(addIntent)
        } else {
            errorMessage(null)
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
                if (blue!!.isChecked)
                    image = R.drawable.pdf
                else
                    image = R.drawable.pdf2

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

    fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.menu_main, menu)
        return true
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        // Rate and Review is clicked
        if (id == R.id.action_settings) {
            val uri = Uri.parse("market://details?id=" + getApplicationContext().getPackageName())
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())))
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

        val mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
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
            shortcutManager.setDynamicShortcuts(listOf<ShortcutInfo>(shortcut))
        }
    }

    companion object {

        val SHORTCUT_VALUE = "yes"
        val SHORTCUT_KEY = "shortcut"
        val SHORTCUT_ID = "id1"

        fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) !== PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
            }
            return true
        }
    }
}
