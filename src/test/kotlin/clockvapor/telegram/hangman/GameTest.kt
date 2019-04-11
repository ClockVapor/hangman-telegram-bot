package clockvapor.telegram.hangman

import org.junit.Assert
import org.junit.Test

class GameTest {
    @Test
    fun testEmoji() {
        val game = Game("\uD83D\uDE02") // crying laughing emoji
        Assert.assertEquals("\uD83D\uDE02", game.getWordString())
    }

    @Test
    fun testEmoji2() {
        val game = Game("\uD83D\uDE02\uD83D\uDE02") // crying laughing emoji
        Assert.assertEquals("\uD83D\uDE02 \uD83D\uDE02", game.getWordString())
    }

    @Test
    fun testEmoji3() {
        val game = Game("\uD83D\uDE02hello") // crying laughing emoji
        Assert.assertEquals("\uD83D\uDE02 _ _ _ _ _", game.getWordString())
    }
}
