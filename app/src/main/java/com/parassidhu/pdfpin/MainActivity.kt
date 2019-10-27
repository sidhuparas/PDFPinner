package com.parassidhu.pdfpin

import android.Manifest
import android.app.Activity
import android.app.PendingIntent.getActivity
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
import android.os.Environment
import android.os.Parcelable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import droidninja.filepicker.FilePickerBuilder
import droidninja.filepicker.FilePickerConst
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File

class MainActivity : AppCompatActivity(), BillingProcessor.IBillingHandler {

    private var docPaths = ArrayList<String>()
    private val listItems = ArrayList<ListItem>()
    private var image: Int = 0
    private var num = 0
    private var dataAdapter: DataAdapter? = null
    private lateinit var interstitialAd: InterstitialAd
    private lateinit var billingProcessor: BillingProcessor

    private val FULL_SCREEN_AD_COUNT = "full_screen_ad_count"

    private val isOreo: Boolean
        get() = Build.VERSION.SDK_INT > 25

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize(this)
        init()

        if (getShowAds()) initAds()
        pinDynamicShortcut()

        // Check if the app is opened using long-press shortcut menu
        checkIfDynamicShortcutIsTapped()

        checkIfAppIsLaunchedFromShareMenu()
        setClickListeners()

        //BillingManager.startBillingFlow(this)
    }

    private fun checkIfDynamicShortcutIsTapped() {
        try {
            val intent = intent
            if (intent.extras != null) {
                if (intent.extras!!.getString(SHORTCUT_KEY) == SHORTCUT_VALUE)
                    chooseBtn!!.callOnClick()
            }
        } catch (e: Exception) {
        }
    }

    private fun setClickListeners() {
        btn_pinned_shortcut_not_opening.setOnClickListener {
            val intent = Intent(this, Troubleshooting::class.java)
            startActivity(intent)
        }
    }

    private fun checkIfAppIsLaunchedFromShareMenu() {
        if (intent?.extras == null)
            return

        if (intent?.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri
            customLogicForShareMenuFiles(uri, true)
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            val list = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
            docPaths = ArrayList()
            for (i in 0 until list.size) {
                val uriString = (list[i] as? Uri)?.path.toString()
                docPaths.add(uriString)
            }
            customLogicForShareMenuFiles(list[0] as Uri, false)
        }
    }

    private fun customLogicForShareMenuFiles(uri: Uri, isSingleFile: Boolean) {
        if (isSingleFile) {
            docPaths = ArrayList()
            docPaths.add(uri.path.toString())
        }

        listItems.clear()
        for (i in docPaths.indices) {
            val path = docPaths[i]
            val newPath = Environment.getExternalStorageDirectory().path + "/" +
                    path.substring(path.indexOf(":") + 1)
            // Example: root/DCIM/Pictures/IMG_8004.JPG
            Log.d("Data", path)

            var name: String
            try {
                name = path.substring(path.lastIndexOf("/") + 1,
                        path.lastIndexOf("."))
                //name = path
            } catch (e: Exception) {
                toast("There's an error: ${e.message}")
                return
            }

            listItems.add(ListItem(newPath, name))
        }
        addDataToList(listItems)
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
            toast("""Permissions for accessing External Storage not granted! To choose a PDF file, it's required.
                            "Please go to Settings->Apps to grant Storage permission.""")
        }
    }

    //Initializes all the mess. btnPDFx represents both icons.
    private fun init() {
        setSupportActionBar(toolbar)
        title = ""

        billingProcessor = BillingProcessor.newBillingProcessor(this,
                BuildConfig.PLAY_KEY, this)
        billingProcessor.initialize()

        file_list.layoutManager = LinearLayoutManager(this)
        image = R.drawable.pdf

        chooseBtn.setOnClickListener { checkPerm() }
        toggleViewsVisibility(0)

        pinFiles.setOnClickListener {
            handleInterstitialAd()
            if (!isOreo) {
                for (i in listItems.indices) {
                    addShortcut(listItems[i].path.toString(), listItems[i].name.toString())
                }
                toast("Success!")
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

    private fun handleInterstitialAd() {
        val numOfTimes = getIntValue(FULL_SCREEN_AD_COUNT, 0)
        if (numOfTimes > 2) {
            if (interstitialAd.isLoaded) {
                interstitialAd.show()
            }
            saveOffline(FULL_SCREEN_AD_COUNT, 0)
            Log.d("MainActivity: ", "Yeah: $numOfTimes")
        } else {
            saveOffline(FULL_SCREEN_AD_COUNT, numOfTimes + 1)
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
        toast("All selected files have been pinned!")
    }

    //Custom toggleViewsVisibility to set views' visibility
    private fun toggleViewsVisibility(`val`: Int) {
        if (`val` == 0) {
            radios.visibility = View.GONE
            btnPDF1.visibility = View.GONE
            btnPDF2.visibility = View.GONE
            pinFiles.visibility = View.GONE
            centerInfo.visibility = View.VISIBLE
            dev.visibility = View.GONE
            btn_pinned_shortcut_not_opening.visibility = View.GONE
        } else {
            radios.visibility = View.VISIBLE
            btnPDF1.visibility = View.VISIBLE
            btnPDF2.visibility = View.VISIBLE
            pinFiles.visibility = View.VISIBLE
            centerInfo.visibility = View.GONE
            dev.visibility = View.VISIBLE
            btn_pinned_shortcut_not_opening.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FilePickerConst.REQUEST_CODE_DOC ->
                if (resultCode == Activity.RESULT_OK && data != null) {
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
        // Filter Incorrect URIs
        for (i in listItems.indices) {
            val uri = listItems[i].path
            if (uri?.contains("/external_files") == true)
                listItems[i].path = uri.replace("/external_files", "")
            listItems[i].path = listItems[i].path?.replace("//", "/")
        }

        num = 0
        pinFiles.text = "Pin File(s)"
        dataAdapter = DataAdapter(this, listItems)
        file_list.adapter = dataAdapter

        if (listItems.size != 0)
            toggleViewsVisibility(1)
        else {
            toggleViewsVisibility(0)
            dev!!.visibility = View.GONE
        }
    }

    private fun addShortcut(path1: String, pdfName: String) {
        val file = File(path1)
        if (Build.VERSION.SDK_INT < 26) {
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
        }
    }

    private fun addShortcutInOreo(path1: String, pdfName: String) {
        val file = File(path1)
        try {
            Log.d("Data", file.path)
            val pdfIntent = Intent(Intent.ACTION_VIEW)

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this,
                        applicationContext.packageName + ".provider", file)
            } else {
                Uri.fromFile(file)
            }

            //val uri = Uri.fromFile(file)
            pdfIntent.setDataAndType(uri, "application/pdf")
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            Log.d("DataPath", uri.path)
            Log.d("DataPath", pdfName)

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
            toast("Some error occurred: " + e.message)
        else
            toast("Some error occurred!")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Rate and Review is clicked
        when (item.itemId) {
            R.id.action_settings -> {
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
            }
            R.id.privacy -> {
                val uri = Uri.parse(getString(R.string.privacy_url))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(intent)
                } catch (ignored: Exception) {
                }
            }
            R.id.go_ad_free -> {
                billingProcessor.purchase(this, "ad_free")
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initAds() {
        MobileAds.initialize(this, BuildConfig.BANNER_KEY)

        val adRequest = AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("4AD76DBC69C8AE7FD181B7F6578B12B7")
                .build()

        adView.loadAd(adRequest)

        interstitialAd = InterstitialAd(this)
        interstitialAd.adUnitId = BuildConfig.INTERSTITIAL_KEY

        val request = AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("4AD76DBC69C8AE7FD181B7F6578B12B7")
                .build()

        interstitialAd.loadAd(request)
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

    override fun onBillingInitialized() {}

    override fun onPurchaseHistoryRestored() {}

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        setShowAds(false)
        //billingProcessor.consumePurchase(productId)
        toast("You have gone ad-free! Ads will be removed on app restart.")
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        Toast.makeText(this, "Error: ($errorCode): ${error?.message}"
                , Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        billingProcessor.release()
        super.onDestroy()
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
