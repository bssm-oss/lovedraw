package com.example.couplecanvas.util

import kotlin.random.Random

object RoomCodeGenerator {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(length: Int = 6, random: Random = Random.Default): String =
        buildString(length) {
            repeat(length) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }

    fun isValid(code: String): Boolean =
        code.length == 6 && code.all { it in ALPHABET }
}
