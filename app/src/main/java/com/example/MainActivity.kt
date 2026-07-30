package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainViewModel
import com.example.ui.VisualAutoClickerApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            val settings by viewModel.settings.collectAsState()
            MyApplicationTheme(darkTheme = settings.isDarkMode, dynamicColor = false) {
                VisualAutoClickerApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}
