package com.ofthestreet.frequencyanalyzer.analysis

/**
 * Format de la chaîne d'analyse, indépendant de la plateforme.
 *
 * Fenêtre de 4096 échantillons (93 ms à 44,1 kHz) : assez longue pour que la plus basse fréquence
 * analysée tienne plusieurs périodes, assez courte pour que l'affichage suive le jeu. Le saut de
 * 2048 fait avancer l'affichage deux fois par fenêtre, sans perte de résolution.
 */
object CaptureFormat {
    const val FRAME_SIZE = 4096
    const val HOP_SIZE = 2048
}
