package com.azizbek.ikeaplace

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.azizbek.ikeaplace.ui.ArScreen
import com.azizbek.ikeaplace.ui.CatalogScreen
import com.azizbek.ikeaplace.ui.GalleryScreen
import com.azizbek.ikeaplace.ui.HomeScreen
import com.azizbek.ikeaplace.ui.IkeaTheme
import com.azizbek.ikeaplace.ui.PreviewScreen
import com.azizbek.ikeaplace.ui.SettingsScreen

enum class Screen { Home, Catalog, Ar, Preview, Gallery, Settings }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IkeaTheme { App() } }
    }
}

@androidx.compose.runtime.Composable
private fun App() {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var itemId by rememberSaveable { mutableStateOf<String?>(null) }
    var shot by remember { mutableStateOf<Bitmap?>(null) }

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (screen) { Screen.Preview -> Screen.Ar; else -> Screen.Home }
    }
    when (screen) {
        Screen.Home -> HomeScreen(
            onStartAr = { screen = Screen.Ar }, onCatalog = { screen = Screen.Catalog },
            onGallery = { screen = Screen.Gallery }, onSettings = { screen = Screen.Settings })
        Screen.Catalog -> CatalogScreen(onBack = { screen = Screen.Home }, onPick = { itemId = it.id; screen = Screen.Ar })
        Screen.Ar -> ArScreen(initialItemId = itemId, onBack = { screen = Screen.Home },
            onCaptured = { shot = it; screen = Screen.Preview })
        Screen.Preview -> shot?.let { PreviewScreen(it, onDone = { screen = Screen.Ar }, onSaved = { screen = Screen.Ar }) } ?: Unit
        Screen.Gallery -> GalleryScreen(onBack = { screen = Screen.Home })
        Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home })
    }
}
