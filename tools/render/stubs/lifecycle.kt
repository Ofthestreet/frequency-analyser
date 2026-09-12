package androidx.lifecycle

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

abstract class ViewModel {
    protected open fun onCleared() {}
}

abstract class AndroidViewModel(private val application: Application) : ViewModel() {
    @Suppress("UNCHECKED_CAST")
    open fun <T : Application> getApplication(): T = application as T
}

val ViewModel.viewModelScope: CoroutineScope
    get() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

interface LifecycleOwner {
    val lifecycle: Lifecycle
}

interface LifecycleObserver

abstract class Lifecycle {
    enum class Event { ON_CREATE, ON_START, ON_RESUME, ON_PAUSE, ON_STOP, ON_DESTROY, ON_ANY }
    abstract fun addObserver(observer: LifecycleObserver)
    abstract fun removeObserver(observer: LifecycleObserver)
}

fun interface LifecycleEventObserver : LifecycleObserver {
    fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event)
}
