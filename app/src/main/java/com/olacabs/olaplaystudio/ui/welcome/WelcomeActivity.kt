package com.olacabs.olaplaystudio.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.olacabs.olaplaystudio.R
import com.olacabs.olaplaystudio.ui.library.LibraryActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        Handler().postDelayed({
            startActivity(Intent(applicationContext, LibraryActivity::class.java))
            finish()
        }, 1000)
    }
}
