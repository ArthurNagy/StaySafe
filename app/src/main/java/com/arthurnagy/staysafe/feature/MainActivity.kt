package com.arthurnagy.staysafe.feature

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arthurnagy.staysafe.MainBinding
import com.arthurnagy.staysafe.R
import dev.chrisbanes.insetter.Insetter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DataBindingUtil.setContentView<MainBinding>(this, R.layout.activity_main)
    }
}