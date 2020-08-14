package com.firedragon.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_first.*


class FirstActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        goToVisualOdometerBtn.setOnClickListener {
            startActivity(Intent(this@FirstActivity, VisualOdometerActivity::class.java))
        }

        goToInertialOdometerBtn.setOnClickListener {
            startActivity(Intent(this@FirstActivity, InertialOdometerActivity::class.java))
        }
    }
}
