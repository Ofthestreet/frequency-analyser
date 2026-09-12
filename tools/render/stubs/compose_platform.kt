package androidx.compose.ui.platform

import android.content.Context
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.LifecycleOwner

val LocalContext: ProvidableCompositionLocal<Context> = staticCompositionLocalOf { error("bouchon") }
val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner> = staticCompositionLocalOf { error("bouchon") }
