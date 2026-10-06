package com.example.clockth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathChallengeTest {
    @Test
    fun answersMatchPrompt() {
        val challenge = MathChallenge(Random(42))
        repeat(50) {
            val question = challenge.next()
            val expected = eval(question.prompt)
            assertEquals(question.prompt, expected, question.answer)
            assertTrue(question.answer >= 0)
        }
    }

    private fun eval(prompt: String): Int {
        val parts = prompt.split(" ")
        val a = parts[0].toInt()
        val b = parts[2].toInt()
        return when (parts[1]) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            else -> error("unknown op ${parts[1]}")
        }
    }
}
