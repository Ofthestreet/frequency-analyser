plugins { base }

repositories { mavenCentral() }

val compose by configurations.creating { isTransitive = false }
val tools by configurations.creating

val cmp = "1.6.11"
val skiko = "0.8.4"

val skikoTarget = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    when {
        os.contains("mac") && arch.contains("aarch64") -> "macos-arm64"
        os.contains("mac") -> "macos-x64"
        os.contains("windows") -> "windows-x64"
        arch.contains("aarch64") -> "linux-arm64"
        else -> "linux-x64"
    }
}

dependencies {
    listOf(
        "org.jetbrains.compose.runtime:runtime-desktop",
        "org.jetbrains.compose.runtime:runtime-saveable-desktop",
        "org.jetbrains.compose.ui:ui-desktop",
        "org.jetbrains.compose.ui:ui-graphics-desktop",
        "org.jetbrains.compose.ui:ui-text-desktop",
        "org.jetbrains.compose.ui:ui-unit-desktop",
        "org.jetbrains.compose.ui:ui-geometry-desktop",
        "org.jetbrains.compose.ui:ui-util-desktop",
        "org.jetbrains.compose.foundation:foundation-desktop",
        "org.jetbrains.compose.foundation:foundation-layout-desktop",
        "org.jetbrains.compose.material3:material3-desktop",
        "org.jetbrains.compose.material:material-ripple-desktop",
        "org.jetbrains.compose.animation:animation-desktop",
        "org.jetbrains.compose.animation:animation-core-desktop",
    ).forEach { compose("$it:$cmp") }

    compose("org.jetbrains.skiko:skiko-awt:$skiko")
    // Bibliothèque native de rendu, propre à la machine qui exécute le banc.
    compose("org.jetbrains.skiko:skiko-awt-runtime-$skikoTarget:$skiko")
    // androidx.collection n'est pas publié sur Maven Central : la variante interne de Compose
    // Multiplatform fournit les mêmes classes, dont le moteur a besoin à l'exécution.
    compose("org.jetbrains.compose.collection-internal:collection-desktop:1.6.0-beta02")
    compose("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1")
    compose("org.robolectric:android-all:14-robolectric-10818077")

    tools("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    tools("org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:2.0.21")
    tools("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    tools("junit:junit:4.13.2")
}

tasks.register<Copy>("collect") {
    from(compose) { into("compose") }
    from(tools) { into("tools") }
    into(layout.projectDirectory.dir("libs"))
}
