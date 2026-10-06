package com.example.clockth.domain

import kotlin.random.Random

data class MathQuestion(
    val prompt: String,
    val answer: Int,
)

class MathChallenge(
    private val random: Random = Random.Default,
) {
    fun next(): MathQuestion {
        return when (random.nextInt(3)) {
            0 -> addition()
            1 -> subtraction()
            else -> multiplication()
        }
    }

    private fun addition(): MathQuestion {
        val a = random.nextInt(11, 41)
        val b = random.nextInt(11, 41)
        return MathQuestion("$a + $b", a + b)
    }

    private fun subtraction(): MathQuestion {
        val a = random.nextInt(20, 81)
        val b = random.nextInt(10, a.coerceAtMost(40) + 1)
        return MathQuestion("$a − $b", a - b)
    }

    private fun multiplication(): MathQuestion {
        val a = random.nextInt(3, 13)
        val b = random.nextInt(3, 13)
        return MathQuestion("$a × $b", a * b)
    }
}
