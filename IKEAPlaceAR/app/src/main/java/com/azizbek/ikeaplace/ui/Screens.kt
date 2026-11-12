package com.azizbek.ikeaplace.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.azizbek.ikeaplace.Catalog
import com.azizbek.ikeaplace.FurnitureItem
import com.azizbek.ikeaplace.R
import com.azizbek.ikeaplace.ScreenshotSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(54.dp), shape = RoundedCornerShape(28.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick, modifier = modifier.height(44.dp), shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Sand, contentColor = Ink),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = Warm,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Warm),
            )
        },
    ) { pad -> Column(Modifier.padding(pad).fillMaxSize().padding(horizontal = 20.dp), content = content) }
}

@Composable
fun MessageScreen(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, onBack: () -> Unit) {
    Page(stringResource(R.string.app_name), onBack) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            if (actionLabel != null && onAction != null) { Spacer(Modifier.height(24.dp)); PrimaryButton(actionLabel, onClick = onAction) }
        }
    }
}

@Composable
fun HomeScreen(onStartAr: () -> Unit, onCatalog: () -> Unit, onGallery: () -> Unit, onSettings: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Column(Modifier.fillMaxSize().background(Warm).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("IKEA Place AR", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(shown, enter = fadeIn() + slideInVertically { it / 4 }) {
            Column {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Sand), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.thumb_sofa_modern), null, Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.start_ar), Modifier.fillMaxWidth(), onStartAr)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton(stringResource(R.string.catalog), Modifier.weight(1f), onCatalog)
            SoftButton(stringResource(R.string.gallery), Modifier.weight(1f), onGallery)
            SoftButton(stringResource(R.string.settings), Modifier.weight(1f), onSettings)
        }
    }
}

@Composable
fun CatalogScreen(onBack: () -> Unit, onPick: (FurnitureItem) -> Unit) {
    Page(stringResource(R.string.catalog), onBack) {
        LazyVerticalGrid(GridCells.Adaptive(150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)) {
            items(Catalog.items, key = { it.id }) { item ->
                Card(Modifier.clickable { onPick(item) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color_White)) {
                    Column(Modifier.padding(12.dp)) {
                        Image(painterResource(item.thumb), null, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(item.nameRes), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(stringResource(R.string.dims, item.widthCm, item.depthCm, item.heightCm), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private val Color_White = androidx.compose.ui.graphics.Color.White

@Composable
fun PreviewScreen(bitmap: Bitmap, onDone: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    fun doSave() {
        val ok = ScreenshotSaver.save(ctx, bitmap) != null
        Toast.makeText(ctx, if (ok) R.string.saved_ok else R.string.saved_err, Toast.LENGTH_LONG).show()
        if (ok) onSaved()
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) doSave() else Toast.makeText(ctx, R.string.saved_err, Toast.LENGTH_LONG).show()
    }
    Page(stringResource(R.string.preview), onDone) {
        Image(bitmap.asImageBitmap(), null, Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftButton(stringResource(R.string.discard), Modifier.weight(1f), onDone)
            PrimaryButton(stringResource(R.string.save), Modifier.weight(1f)) {
                if (Build.VERSION.SDK_INT < 29 &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ) permLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else doSave()
            }
        }
    }
}

@Composable
fun GalleryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var uris by remember { mutableStateOf<List<android.net.Uri>?>(null) }
    LaunchedEffect(Unit) { uris = withContext(Dispatchers.IO) { ScreenshotSaver.list(ctx) } }
    Page(stringResource(R.string.gallery), onBack) {
        val list = uris
        if (list != null && list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.empty_gallery)) }
        } else if (list != null) {
            LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.toString() }) { uri ->
                    val bmp by produceState<Bitmap?>(null, uri) { value = withContext(Dispatchers.IO) { ScreenshotSaver.decode(ctx, uri) } }
                    Box(Modifier.aspectRatio(0.6f).clip(RoundedCornerShape(16.dp)).background(Sand).clickable {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    }) { bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Page(stringResource(R.string.settings), onBack) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("O‘zbekcha", Modifier.weight(1f)) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("uz")) }
            PrimaryButton("Русский", Modifier.weight(1f)) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.about), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("v1.0 · com.azizbek.ikeaplace", style = MaterialTheme.typography.bodyMedium)
    }
}
