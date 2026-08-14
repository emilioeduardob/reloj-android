package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color

/**
 * 8x8 pixel-art coin icon drawn in the color section of the display.
 */
private val darkGold = Color(0xFFB8860B)
private val gold = Color(0xFFDAA520)
private val lightGold = Color(0xFFFFD700)
private val paleYellow = Color(0xFFFFFFB0)

private val coinPattern: List<String> = listOf(
    "........",
    "..RRRR..",
    ".RBBBBR.",
    ".RBCCBR.",
    ".RBCCBR.",
    ".RBBBBR.",
    "..RRRR..",
    "........"
)

private fun colorFor(char: Char): Color? = when (char) {
    'C' -> darkGold
    'B' -> gold
    'R' -> lightGold
    'P' -> paleYellow
    else -> null
}

fun PixelMatrix.drawCoinIcon(offsetX: Int, offsetY: Int): PixelMatrix {
    return mutate {
        coinPattern.forEachIndexed { row, line ->
            line.forEachIndexed { col, char ->
                colorFor(char)?.let { color ->
                    this[offsetX + col, offsetY + row] = color
                }
            }
        }
    }
}
