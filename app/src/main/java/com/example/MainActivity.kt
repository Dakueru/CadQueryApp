package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CadViewModel
import com.example.ui.MainCadScreen
import com.example.ui.theme.CadQueryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CadQueryTheme {
                val viewModel: CadViewModel = viewModel()
                MainCadScreen(viewModel = viewModel)
            }
        }
    }
}
