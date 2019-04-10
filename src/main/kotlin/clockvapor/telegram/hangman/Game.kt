package clockvapor.telegram.hangman

class Game(val word: String) {
    private val guessedLetters = linkedSetOf<Char>()
    private val wrongLetters = linkedSetOf<Char>()
    private val missingLetters: Int
        get() = word.sumBy {
            if (it.isLetterOrDigit() && it.toUpperCase() !in guessedLetters) 1
            else 0
        }

    fun guessLetter(letter: Char): State {
        val l = letter.toUpperCase()
        guessedLetters += l
        if (!word.contains(l, ignoreCase = true)) {
            wrongLetters += l
        }
        return when {
            wrongLetters.size > 5 -> State.LOSE
            missingLetters < 1 -> State.WIN
            else -> State.CONTINUE
        }
    }

    override fun toString(): String {
        return "```${getPersonString()}\n\n${getWordString()}\n\n${getWrongLettersString()}```"
    }

    private fun getWordString(): String =
        word.map {
            if (it.isLetterOrDigit()) {
                if (guessedLetters.contains(it.toUpperCase())) it
                else '_'
            } else it
        }.joinToString(" ")

    private fun getWrongLettersString(): String =
        wrongLetters.joinToString(" ")

    private fun getPersonString(): String = when (wrongLetters.size) {
        0 -> """
             ____
            |    |
            |
            |
            |
            |
        """.trimIndent()

        1 -> """
             ____
            |    |
            |    O
            |
            |
            |
        """.trimIndent()

        2 -> """
             ____
            |    |
            |    O
            |    |
            |
            |
        """.trimIndent()

        3 -> """
             ____
            |    |
            |    O
            |   /|
            |
            |
        """.trimIndent()

        4 -> """
             ____
            |    |
            |    O
            |   /|\
            |
            |
        """.trimIndent()

        5 -> """
             ____
            |    |
            |    O
            |   /|\
            |   /
            |
        """.trimIndent()

        else -> """
             ____
            |    |
            |    O
            |   /|\
            |   / \
            |
        """.trimIndent()
    }

    enum class State {
        CONTINUE,
        WIN,
        LOSE
    }
}
