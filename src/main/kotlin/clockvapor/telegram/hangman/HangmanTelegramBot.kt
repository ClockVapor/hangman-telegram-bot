package clockvapor.telegram.hangman

import clockvapor.telegram.trySuccessful
import com.fasterxml.jackson.databind.ObjectMapper
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import me.ivmg.telegram.Bot
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.dispatcher.text
import me.ivmg.telegram.entities.ParseMode
import me.ivmg.telegram.entities.Update
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup

class HangmanTelegramBot(private val token: String) {
    private val games = hashMapOf<Long, Game>()

    // from user id to chat id
    private val customWordGivers = hashMapOf<Long, Long>()

    fun run() {
        bot {
            token = this@HangmanTelegramBot.token
            logLevel = HttpLoggingInterceptor.Level.NONE
            dispatch {
                command("start") { bot, update -> tryCommand(bot, update, ::doHelpCommand) }
                command("help") { bot, update -> tryCommand(bot, update, ::doHelpCommand) }
                command("newgame") { bot, update -> tryCommand(bot, update, ::doNewGameCommand) }
                command("newcustomgame") { bot, update -> tryCommand(bot, update, ::doNewCustomGameCommand) }
                command("stopgame") { bot, update -> tryCommand(bot, update, ::doStopGameCommand) }
                command("guess") { bot, update -> tryCommand(bot, update, ::doGuessCommand) }
                text { bot, update -> tryCommand(bot, update, ::readCustomWord) }
            }
        }.startPolling()
    }

    private fun tryCommand(bot: Bot, update: Update, action: (Bot, Update) -> Unit) {
        if (!trySuccessful(reportException = true) { action(bot, update) }) {
            val message = update.message!!
            bot.sendMessage(message.chat.id, "<an error occurred>", replyToMessageId = message.messageId)
        }
    }

    private fun doHelpCommand(bot: Bot, update: Update) {
        bot.sendMessage(update.message!!.chat.id, """
            Use the `/newgame` command to start a game of Hangman! Then, use the `/guess` command to guess a letter or
            digit, or the `/stopgame` command to stop the game.
        """.trimIndent(),
            parseMode = ParseMode.MARKDOWN
        )
    }

    private fun doNewGameCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        if (chatId in games) {
            bot.sendMessage(chatId, GAME_IN_PROGRESS, replyToMessageId = message.messageId)
        } else {
            val command = message.entities!![0]
            val source = message.text!!.substring(command.offset + command.length).trim()
            val word = when {
                source.isBlank() -> fetchRandomWordWiktionary()
                source == "wiktionary" -> fetchRandomWordWiktionary()
                source == "urbandictionary" -> fetchRandomWordUrbanDictionary()
                else -> null
            }
            if (word == null) {
                bot.sendMessage(chatId,
                    "Please give one of the following word sources: `wiktionary`, `urbandictionary`. " +
                    "If nothing is given, `wiktionary` will be used by default.", parseMode = ParseMode.MARKDOWN)
            } else {
                val game = Game(word)
                games[chatId] = game
                bot.sendMessage(chatId, game.toString(), replyToMessageId = message.messageId,
                    parseMode = ParseMode.MARKDOWN)
            }
        }
    }

    private fun doNewCustomGameCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        if (chatId in games) {
            bot.sendMessage(chatId, GAME_IN_PROGRESS, replyToMessageId = message.messageId)
        } else {
            val (response, _) = bot.sendMessage(message.from!!.id, "What word do you want to use?")
            if (response?.isSuccessful != true) {
                bot.sendMessage(chatId, "I wasn't able to talk to you in a private chat. " +
                    "Make sure you've opened a private chat with me so you can secretly send me the word to play with.",
                    replyToMessageId = message.messageId
                )
            } else {
                customWordGivers[message.from!!.id] = chatId
                bot.sendMessage(chatId,
                    "I sent you a private message. Please send me the word to play with over there.",
                    replyToMessageId = message.messageId
                )
            }
        }
    }

    private fun doStopGameCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        val game = games.remove(chatId)
        val replyText =
            if (game == null) NO_GAME_IN_PROGRESS
            else "Okay, the game is cancelled. The word was `${game.word}`."
        bot.sendMessage(chatId, replyText, replyToMessageId = message.messageId, parseMode = ParseMode.MARKDOWN)
    }

    private fun doGuessCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        val game = games[chatId]
        if (game == null) {
            bot.sendMessage(chatId, NO_GAME_IN_PROGRESS, replyToMessageId = message.messageId)
        } else {
            val command = message.entities!!.first()
            val guesses = message.text!!.substring(command.offset + command.length).trim()
                .filter { it.isLetterOrDigit() }
                .toList()
            if (guesses.isEmpty()) {
                bot.sendMessage(chatId, "Please guess some letters or digits.", replyToMessageId = message.messageId)
            } else {
                for (guess in guesses) {
                    game.guess(guess)
                    if (game.state != Game.State.CONTINUE) break
                }
                handleGame(bot, chatId, message.messageId, game)
            }
        }
    }

    private fun readCustomWord(bot: Bot, update: Update) {
        val message = update.message!!
        if (message.chat.type == "private" && message.from!!.id in customWordGivers) {
            val targetChatId = customWordGivers.remove(message.from!!.id)!!
            if (targetChatId in games) {
                bot.sendMessage(message.chat.id, "It looks like there's already a game going on in that group.")
            } else {
                val word = message.text!!.trim()
                val game = Game(word)
                games[targetChatId] = game
                handleGame(bot, targetChatId, null, game)
            }
        }
    }

    private fun fetchRandomWordWiktionary(): String {
        val url = tryCreateUrl("https://en.wiktionary.org/wiki/Special:RandomInCategory/English_lemmas")
        return Jsoup.parse(wget(url)).getElementById("firstHeading").text().trim()
    }

    private fun fetchRandomWordUrbanDictionary(): String {
        val url = tryCreateUrl("https://api.urbandictionary.com/v0/random")
        val jsonString = wget(url)
        val json = ObjectMapper().readTree(jsonString)
        return json["list"].elements().asSequence().toList().random()["word"].asText()
    }

    private fun handleGame(bot: Bot, chatId: Long, replyToMessageId: Long?, game: Game) {
        val text = when (game.state) {
            Game.State.CONTINUE -> game.toString()

            Game.State.WIN -> {
                games -= chatId
                "You win!\n\n$game"
            }

            Game.State.LOSE -> {
                games -= chatId
                "You lose! The word was `${game.word}`.\n\n$game"
            }
        }

        bot.sendMessage(chatId, text, replyToMessageId = replyToMessageId, parseMode = ParseMode.MARKDOWN)
    }

    companion object {
        private const val GAME_IN_PROGRESS = "There's already a game in progress."
        private const val NO_GAME_IN_PROGRESS = "There's no game in progress."

        @JvmStatic
        fun main(a: Array<String>) = mainBody {
            val args = ArgParser(a).parseInto(::Args)
            HangmanTelegramBot(args.telegramBotToken).run()
        }
    }

    private class Args(parser: ArgParser) {
        val telegramBotToken: String by parser.storing("-t", help = "Telegram bot token")
    }
}
