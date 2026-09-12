# Banc de rendu sans SDK Android

`./render.sh` compile **toutes** les sources de l'app et dessine ses écrans en PNG, sur une machine
sans SDK Android.

```bash
./tools/render/render.sh          # écrans dans tools/render/build/shots
```

Les PNG ne sont pas versionnés : ce script les régénère.

Il produit `app-ecoute.png`, `app-silence.png`, `app-reglages.png`, `app-permission.png` et
`staves.png` (les cas limites de la portée : clé de fa, dièses, lignes supplémentaires, 8va/15ma),
et exécute au passage les tests unitaires du traitement du signal.

## Comment c'est possible

| Besoin | Substitut |
| --- | --- |
| `androidx.compose.*` | jars de Compose Multiplatform (mêmes paquets, publiés sur Maven Central) |
| `android.jar` | `org.robolectric:android-all` |
| `androidx.activity`, `androidx.lifecycle`, `androidx.datastore` | bouchons de `stubs/`, réduits aux signatures utilisées |
| compilateur Compose | `kotlin-compose-compiler-plugin-embeddable` |

## Ce que ça ne prouve pas

Le rendu vient de Compose Desktop : la mise en page, les tracés et la logique sont ceux de l'app,
mais ni le vrai moteur de rendu Android, ni les polices du téléphone, ni la capture micro. Les
bouchons reproduisent les signatures d'AndroidX, pas leur comportement. L'APK et sa validation
finale restent l'affaire de `gradle assembleDebug` et du workflow GitHub Actions.
