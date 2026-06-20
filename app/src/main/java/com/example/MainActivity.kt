package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.screens.AppNavigationHost
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        
        // Initialize local Room DB & Repository
        val database = remember { AppDatabase.getDatabase(context.applicationContext, coroutineScope) }
        val repository = remember { AppRepository(database.appDao()) }
        
        // Initialize our central state coordinator ViewModel
        val mainViewModel: MainViewModel = viewModel(
          factory = MainViewModelFactory(context.applicationContext, repository)
        )

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          AppNavigationHost(viewModel = mainViewModel)
        }
      }
    }
  }
}

