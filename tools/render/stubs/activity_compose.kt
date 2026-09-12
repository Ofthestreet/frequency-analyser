package androidx.activity.compose

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable

fun ComponentActivity.setContent(content: @Composable () -> Unit): Unit = error("bouchon de vérification")

class ManagedActivityResultLauncher<I, O> {
    fun launch(input: I): Unit = error("bouchon de vérification")
}

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ManagedActivityResultLauncher<I, O> = error("bouchon de vérification")
