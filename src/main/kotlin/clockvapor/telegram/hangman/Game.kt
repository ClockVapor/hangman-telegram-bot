package clockvapor.telegram.hangman

class Game(val word: String) {
    private val wordParts: List<String>
    private val guessedLetters = linkedSetOf<Char>()
    private val wrongLetters = linkedSetOf<Char>()
    private val numMissingLetters: Int
        get() = wordParts.sumBy {
            if (it.length == 1 && it[0].isLetterOrDigit() && it[0].toUpperCase() !in guessedLetters) 1
            else 0
        }

    val state: State
        get() = when {
            wrongLetters.size > 5 -> State.LOSE
            numMissingLetters < 1 -> State.WIN
            else -> State.CONTINUE
        }

    init {
        val tempWordParts = arrayListOf<String>()
        var i = 0
        while (i < word.length) {
            if (word[i].isLowSurrogate()) {
                tempWordParts += word.substring(i - 1, i + 1)
            } else if (!word[i].isSurrogate()) {
                tempWordParts += word[i].toString()
            }
            i++
        }
        wordParts = tempWordParts
    }

    fun guess(char: Char) {
        if (char.isLetterOrDigit()) {
            val c = char.toUpperCase()
            guessedLetters += c
            if (wordParts.none { it.equals(c.toString(), ignoreCase = true) }) {
                wrongLetters += c
            }
        }
    }

    override fun toString(): String {
        return "```${getPersonString()}\n\n${getWordString()}\n\n${getWrongLettersString()}```"
    }

    fun getWordString(): String =
        wordParts.map {
            if (it.length == 1 && it[0].isLetterOrDigit() && it[0].toUpperCase() !in guessedLetters) "_"
            else it
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
