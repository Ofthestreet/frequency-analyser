#!/usr/bin/env bash
#
# Compile l'app et dessine ses écrans, sans SDK Android.
#
# Le SDK Android n'est pas toujours installable (réseau filtré, machine de passage). Ce banc
# compile malgré tout l'intégralité des sources et rend les écrans en PNG :
#   - les jars de Compose Multiplatform portent les mêmes paquets androidx.compose.* que Compose
#     Android, et se téléchargent depuis Maven Central ;
#   - android.jar est remplacé par le android-all de Robolectric ;
#   - les rares API AndroidX absentes de Maven Central (activity, lifecycle, datastore) sont
#     remplacées par les bouchons de stubs/, qui ne servent qu'à la compilation.
#
# Ce n'est pas un substitut à `assembleDebug` : le rendu vient de Compose Desktop, pas d'Android.
# Il attrape en revanche les erreurs de compilation et les défauts de mise en page, tout de suite.
#
# Usage : ./tools/render/render.sh [dossier de sortie]
set -euo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT=$(cd "$HERE/../.." && pwd)
OUT=${1:-$HERE/build/shots}
WORK=$HERE/build
GRADLE=${GRADLE:-gradle}

mkdir -p "$OUT" "$WORK"

echo "== Téléchargement de la chaîne d'outils"
(cd "$HERE" && "$GRADLE" collect -q --console=plain)

LIBS=$(ls "$HERE"/libs/compose/*.jar | tr '\n' ':')
T=$HERE/libs/tools
COMPILER="$T/kotlin-compiler-embeddable-2.0.21.jar:$T/kotlin-stdlib-2.0.21.jar:$T/kotlin-reflect-1.6.10.jar:$T/kotlin-script-runtime-2.0.21.jar:$T/kotlin-daemon-embeddable-2.0.21.jar:$T/trove4j-1.0.20200330.jar:$T/annotations-13.0.jar:$T/kotlinx-coroutines-core-jvm-1.6.4.jar"
PLUGIN="$T/kotlin-compose-compiler-plugin-embeddable-2.0.21.jar"

# Compilation avec le plugin Compose, pour tout ce qui contient des @Composable.
kotlinc() {
    local out=$1; shift
    local cp=$1; shift
    java -cp "$COMPILER" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
        -no-stdlib -nowarn -jvm-target 17 -Xplugin="$PLUGIN" \
        -cp "$cp" -d "$out" "$@"
}

# Sans le plugin : les tests n'ont pas le runtime Compose sur leur chemin de classes, et le plugin
# refuse de tourner sans lui.
kotlinc_plain() {
    local out=$1; shift
    local cp=$1; shift
    java -cp "$COMPILER" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
        -no-stdlib -nowarn -jvm-target 17 \
        -cp "$cp" -d "$out" "$@"
}

echo "== Compilation de l'application"
rm -rf "$WORK/app" "$WORK/render" "$WORK/test"
kotlinc "$WORK/app" "$LIBS$T/kotlin-stdlib-2.0.21.jar" \
    $(find "$PROJECT/app/src/main/java" -name '*.kt') $(find "$HERE/stubs" -name '*.kt')

echo "== Compilation et exécution des tests unitaires"
kotlinc_plain "$WORK/test" "$WORK/app:$T/kotlin-stdlib-2.0.21.jar:$T/junit-4.13.2.jar:$T/hamcrest-core-1.3.jar" \
    $(find "$PROJECT/app/src/test/java" -name '*.kt')
java -cp "$WORK/test:$WORK/app:$T/kotlin-stdlib-2.0.21.jar:$T/junit-4.13.2.jar:$T/hamcrest-core-1.3.jar" \
    org.junit.runner.JUnitCore \
    com.ofthestreet.frequencyanalyzer.YinDetectorTest \
    com.ofthestreet.frequencyanalyzer.NoteNamingTest \
    com.ofthestreet.frequencyanalyzer.FrameAnalyzerTest

echo "== Rendu des écrans"
kotlinc "$WORK/render" "$LIBS$WORK/app:$T/kotlin-stdlib-2.0.21.jar" "$HERE"/src/*.kt
cd "$OUT"
java -Djava.awt.headless=true -cp "$LIBS$WORK/app:$WORK/render:$T/kotlin-stdlib-2.0.21.jar" RenderKt
java -Djava.awt.headless=true -cp "$LIBS$WORK/app:$WORK/render:$T/kotlin-stdlib-2.0.21.jar" StavesKt
echo "== Écrans dans $OUT"
