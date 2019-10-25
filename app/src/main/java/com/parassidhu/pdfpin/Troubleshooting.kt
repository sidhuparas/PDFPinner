package com.parassidhu.pdfpin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Troubleshooting : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_troubleshooting)
        supportActionBar?.title = "Troubleshooting"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
