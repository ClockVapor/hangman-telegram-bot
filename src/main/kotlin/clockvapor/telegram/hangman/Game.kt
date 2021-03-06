package clockvapor.telegram.hangman

import com.xenomachina.text.codePointSequence

class Game(val word: String) {
    private val guessedLetters = linkedSetOf<Char>()
    private val wrongLetters = linkedSetOf<Char>()
    private val numMissingLetters: Int
        get() = word.codePointSequence().sumBy {
            if (Utils.isLetterOrDigit(it) && it.toChar().toUpperCase() !in guessedLetters) 1
            else 0
        }

    val state: State
        get() = when {
            wrongLetters.size > 5 -> State.LOSE
            numMissingLetters < 1 -> State.WIN
            else -> State.CONTINUE
        }

    fun guess(char: Char) {
        if (char.isLetterOrDigit()) {
            val c = char.toUpperCase()
            guessedLetters += c
            if (!word.contains(c, ignoreCase = true)) {
                wrongLetters += c
            }
        }
    }

    override fun toString(): String {
        return "```${getPersonString()}\n\n${getWordString()}\n\n${getWrongLettersString()}```"
    }

    fun getWordString(): String =
        word.codePointSequence().map {
            if (Utils.isLetterOrDigit(it) && it.toChar().toUpperCase() !in guessedLetters) "_"
            else Utils.toChars(it).joinToString("")
        }.joinToString(" ")

    fun getWrongLettersString(): String =
        wrongLetters.joinToString(" ")

    fun getPersonString(): String = when (wrongLetters.size) {
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
