package com.example.relojandroid.engine

import androidx.compose.ui.graphics.Color

/**
 * Simple 3x5 pixel font for digits and a small set of uppercase letters/symbols.
 * Each character is encoded as 5 rows of 3 bits.
 */

private val digits = mapOf(
    '0' to listOf("111", "101", "101", "101", "111"),
    '1' to listOf("010", "110", "010", "010", "111"),
    '2' to listOf("111", "001", "111", "100", "111"),
    '3' to listOf("111", "001", "111", "001", "111"),
    '4' to listOf("101", "101", "111", "001", "001"),
    '5' to listOf("111", "100", "111", "001", "111"),
    '6' to listOf("111", "100", "111", "101", "111"),
    '7' to listOf("111", "001", "001", "001", "001"),
    '8' to listOf("111", "101", "111", "101", "111"),
    '9' to listOf("111", "101", "111", "001", "111"),
)

private val letters = mapOf(
    'A' to listOf("111", "101", "111", "101", "101"),
    'B' to listOf("110", "101", "110", "101", "110"),
    'C' to listOf("111", "100", "100", "100", "111"),
    'D' to listOf("110", "101", "101", "101", "110"),
    'E' to listOf("111", "100", "111", "100", "111"),
    'F' to listOf("111", "100", "110", "100", "100"),
    'G' to listOf("111", "100", "101", "101", "111"),
    'H' to listOf("101", "101", "111", "101", "101"),
    'I' to listOf("111", "010", "010", "010", "111"),
    'J' to listOf("001", "001", "001", "101", "111"),
    'K' to listOf("101", "101", "110", "101", "101"),
    'L' to listOf("100", "100", "100", "100", "111"),
    'M' to listOf("101", "111", "101", "101", "101"),
    'N' to listOf("111", "101", "101", "101", "101"),
    'O' to listOf("111", "101", "101", "101", "111"),
    'P' to listOf("111", "101", "111", "100", "100"),
    'Q' to listOf("111", "101", "101", "110", "011"),
    'R' to listOf("110", "101", "110", "101", "101"),
    'S' to listOf("111", "100", "111", "001", "111"),
    'T' to listOf("111", "010", "010", "010", "010"),
    'U' to listOf("101", "101", "101", "101", "111"),
    'V' to listOf("101", "101", "101", "101", "010"),
    'W' to listOf("101", "101", "101", "111", "101"),
    'X' to listOf("101", "101", "010", "101", "101"),
    'Y' to listOf("101", "101", "010", "010", "010"),
    'Z' to listOf("111", "001", "010", "100", "111"),
    ' ' to listOf("000", "000", "000", "000", "000"),
    ':' to listOf("000", "010", "000", "010", "000"),
    '/' to listOf("001", "001", "010", "100", "100"),
    '.' to listOf("000", "000", "000", "000", "010"),
    '-' to listOf("000", "000", "111", "000", "000"),
    '°' to listOf("010", "101", "000", "000", "000"),
    '$' to listOf("011", "101", "011", "001", "110"),
)

private val allGlyphs = digits + letters

private fun glyphFor(char: Char): List<String> {
    return allGlyphs[char.uppercaseChar()]
        ?: allGlyphs['?']
        ?: listOf("010", "101", "010", "000", "010")
}

/**
 * Draw a single character at (x, y) using the 3x5 font.
 */
fun PixelMatrix.drawChar(
    char: Char,
    x: Int,
    y: Int,
    color: Color
): PixelMatrix {
    val glyph = glyphFor(char)
    return mutate {
        glyph.forEachIndexed { row, line ->
            line.forEachIndexed { col, bit ->
                if (bit == '1') {
                    this[x + col, y + row] = color
                }
            }
        }
    }
}

/**
 * Draw a string left-to-right with 1 pixel spacing between characters.
 */
fun PixelMatrix.drawString(
    text: String,
    x: Int,
    y: Int,
    color: Color
): PixelMatrix {
    var result = this
    var cursorX = x
    text.forEach { char ->
        result = result.drawChar(char, cursorX, y, color)
        cursorX += 4 // 3px wide + 1px spacing
    }
    return result
}

fun measureString(text: String): Int = text.length * 4 - 1

/**
 * 5x7 pixel font for large time digits and the colon.
 * Each character is encoded as 7 rows of 5 bits.
 */
private val bigDigits = mapOf(
    '0' to listOf("11111", "10001", "10011", "10101", "11001", "10001", "11111"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("11110", "00001", "00001", "00110", "01000", "10000", "11111"),
    '3' to listOf("11110", "00001", "00010", "00110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
)

private val bigSymbols = mapOf(
    ':' to listOf("000", "000", "010", "000", "010", "000", "000"),
    'C' to listOf("01110", "10001", "10000", "10000", "10000", "10001", "01110"),
    'F' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
    'V' to listOf("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
    '°' to listOf("01110", "10001", "10001", "01110", "00000", "00000", "00000"),
)

private val bigGlyphs = bigDigits + bigSymbols

private fun bigGlyphFor(char: Char): List<String> {
    return bigGlyphs[char]
        ?: bigGlyphs['?']
        ?: listOf("010", "101", "010", "000", "010", "000", "000")
}

fun PixelMatrix.drawBigChar(
    char: Char,
    x: Int,
    y: Int,
    color: Color
): PixelMatrix {
    val glyph = bigGlyphFor(char)
    return mutate {
        glyph.forEachIndexed { row, line ->
            line.forEachIndexed { col, bit ->
                if (bit == '1') {
                    this[x + col, y + row] = color
                }
            }
        }
    }
}

fun PixelMatrix.drawBigString(
    text: String,
    x: Int,
    y: Int,
    color: Color
): PixelMatrix {
    var result = this
    var cursorX = x
    text.forEach { char ->
        result = result.drawBigChar(char, cursorX, y, color)
        cursorX += bigGlyphFor(char)[0].length + 1
    }
    return result
}

fun measureBigString(text: String): Int {
    return text.sumOf { bigGlyphFor(it)[0].length + 1 } - 1
}
