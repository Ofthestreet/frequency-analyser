package com.ofthestreet.frequencyanalyzer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ofthestreet.frequencyanalyzer.ui.AnalyzerScreen
import com.ofthestreet.frequencyanalyzer.ui.PermissionScreen
import com.ofthestreet.frequencyanalyzer.ui.SettingsScreen
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Une mesure se lit en jouant : l'écran ne doit pas s'éteindre au milieu.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            AnalyzerTheme {
                AnalyzerApp()
            }
        }
    }
}

@Composable
private fun AnalyzerApp(viewModel: AnalyzerViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var refused by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        refused = !result
        if (result) viewModel.startListening()
    }

    // L'écoute suit le cycle de vie : le micro est relâché dès que l'app passe en arrière-plan.
    DisposableEffect(lifecycleOwner, granted) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (granted) viewModel.startListening()
                Lifecycle.Event.ON_STOP -> viewModel.stopListening()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopListening()
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val modifier = Modifier
        .fillMaxSize()
        .background(AnalyzerColors.Background)
        .windowInsetsPadding(WindowInsets.systemBars)

    when {
        !granted -> PermissionScreen(
            permanentlyDenied = refused,
            onRequest = {
                if (refused) openApplicationSettings(context) else launcher.launch(Manifest.permission.RECORD_AUDIO)
            },
            modifier = modifier,
        )

        showSettings -> SettingsScreen(
            settings = settings,
            onChange = { transform -> viewModel.updateSettings(transform) },
            onBack = { showSettings = false },
            modifier = modifier,
        )

        else -> AnalyzerScreen(
            state = state,
            settings = settings,
            onOpenSettings = { showSettings = true },
            onToggleListening = {
                if (state.listening) viewModel.stopListening() else viewModel.startListening()
            },
            onToggleFreeze = viewModel::toggleFreeze,
            modifier = modifier,
        )
    }
}

private fun openApplicationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
