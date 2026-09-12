# Frequency Analyzer

Application Android qui écoute le micro et affiche, en temps réel, la fréquence dominante du son :
en hertz, en nom de note (français **et** américain), et à sa place sur une portée.

## Ce que l'écran montre

| Élément | Détail |
| --- | --- |
| Fréquence | chiffre principal, en Hz, avec l'indice de confiance et le niveau en dBFS |
| Note | `FR La3` et `US A4` côte à côte — le 440 Hz s'appelle `la3` en notation française traditionnelle et `A4` en notation scientifique |
| Portée | clé de sol, avec lignes supplémentaires et dièse automatiques ; passage en clé de fa sous Do3 |
| Écart | aiguille de -50 à +50 cents par rapport à la note tempérée la plus proche |
| Spectre | FFT 4096 points, fenêtre de Hann, 56 bandes en échelle logarithmique |
| Historique | fréquence des dernières secondes, échelle logarithmique |

## Comment la mesure est faite

1. **Capture** — `AudioRecord` sur la source la moins traitée que le téléphone accepte
   (`UNPROCESSED`, sinon `VOICE_RECOGNITION`, sinon `MIC`), 48 ou 44,1 kHz, mono 16 bits.
   La fenêtre d'analyse fait 4096 échantillons et avance de 2048 : environ 21 mesures par seconde.
2. **Niveau** — valeur efficace de la trame, en dBFS ; sous le seuil réglé, la détection est
   suspendue plutôt que de commenter du bruit de fond.
3. **Hauteur** — trois algorithmes au choix :
   - **YIN** (défaut) : fonction de différence moyenne cumulée normalisée, puis interpolation
     parabolique. C'est le plus fiable sur une note tenue, et il ne se trompe pas d'octave sur un
     son riche en harmoniques.
   - **FFT + HPS** : pic de FFT corrigé par repliement harmonique, avec recherche de la
     sous-harmonique qui explique le mieux les partiels observés.
   - **Autocorrélation** : corrélation normalisée, premier sommet après la première retombée.
4. **Note** — conversion en numéro MIDI par rapport au diapason réglé, puis nom, altération, écart
   en cents et degré diatonique pour le placement sur la portée.

Le traitement est entièrement local : rien n'est enregistré, rien ne sort du téléphone.

## Vérifier sans SDK Android

Le SDK Android n'est pas installable partout. `tools/render/render.sh` compile malgré tout
l'intégralité des sources, exécute les tests du traitement du signal et dessine les écrans en PNG,
en s'appuyant sur les jars de Compose Multiplatform (mêmes paquets `androidx.compose.*`) et sur le
`android-all` de Robolectric. Voir `tools/render/README.md` pour ce que cela prouve — et ce que
cela ne prouve pas.

```bash
./tools/render/render.sh
```

## Construire et installer

Le SDK Android n'est pas nécessaire pour lire le code, mais il l'est pour construire. Le plus
simple est de laisser l'intégration continue le faire :

1. pousser — le workflow `.github/workflows/android.yml` lance les tests puis `assembleDebug` ;
2. ouvrir le run dans l'onglet **Actions**, télécharger l'artefact `frequency-analyser-debug-apk` ;
3. décompresser, copier `app-debug.apk` sur le téléphone et l'ouvrir. Android demandera
   d'autoriser l'installation depuis cette source (APK signé avec la clé de debug).

En local, avec un SDK Android installé :

```bash
./gradlew testDebugUnitTest   # tests du traitement du signal, sans appareil
./gradlew assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug     # sur un téléphone branché en débogage USB
```

Minimum Android 8.0 (API 26).

## Maquette

Les écrans ont d'abord été maquettés en HTML avant d'être écrits en Compose. Cette maquette a fait
son office et n'est pas versionnée : l'app dessine désormais elle-même ses écrans, et
`tools/render/render.sh` en produit les rendus.

## Organisation du code

```
app/src/main/java/com/ofthestreet/frequencyanalyzer/
├── analysis/      capture micro, FFT, YIN, bandes de spectre, orchestration par trame
├── music/         noms de notes FR/US, degrés de portée, glyphes musicaux vectoriels
├── settings/      réglages persistés (DataStore)
├── ui/            écrans et tracés Compose
└── AnalyzerViewModel.kt
```

Les tests unitaires (`app/src/test`) couvrent le traitement du signal et la nomenclature des
notes : ils tournent sur la JVM, sans appareil ni émulateur.
