package androidx.activity.result.contract

abstract class ActivityResultContract<I, O>

object ActivityResultContracts {
    class RequestPermission : ActivityResultContract<String, Boolean>()
}
