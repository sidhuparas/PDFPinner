package com.parassidhu.pdfpin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Troubleshooting : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_troubleshooting)
        supportActionBar?.title = "Troubleshooting"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
