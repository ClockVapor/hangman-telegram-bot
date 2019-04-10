package clockvapor.telegram.hangman

import clockvapor.telegram.trySuccessful
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import me.ivmg.telegram.Bot
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.entities.ParseMode
import me.ivmg.telegram.entities.Update
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup

class HangmanTelegramBot(private val token: String) {
    private val games = hashMapOf<Long, Game>()

    fun run() {
        bot {
            token = this@HangmanTelegramBot.token
            logLevel = HttpLoggingInterceptor.Level.NONE
            dispatch {
                command("start") { bot, update -> tryCommand(bot, update, ::doHelpCommand) }
                command("help") { bot, update -> tryCommand(bot, update, ::doHelpCommand) }
                command("newgame") { bot, update -> tryCommand(bot, update, ::doNewGameCommand) }
                command("stopgame") { bot, update -> tryCommand(bot, update, ::doStopGameCommand) }
                command("guess") { bot, update -> tryCommand(bot, update, ::doGuessCommand) }
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
        if (games.containsKey(chatId)) {
            bot.sendMessage(chatId, "There's already a game in progress.", replyToMessageId = message.messageId)
        } else {
            val game = Game(fetchRandomWord())
            games[chatId] = game
            bot.sendMessage(chatId, game.toString(), replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN)
        }
    }

    private fun doStopGameCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        val game = games.remove(chatId)
        val replyText =
            if (game == null) "There's no game in progress."
            else "Okay, the game is cancelled. The word was `${game.word}`."
        bot.sendMessage(chatId, replyText, replyToMessageId = message.messageId, parseMode = ParseMode.MARKDOWN)
    }

    private fun doGuessCommand(bot: Bot, update: Update) {
        val message = update.message!!
        val chatId = message.chat.id
        val game = games[chatId]
        if (game == null) {
            bot.sendMessage(chatId, "There's no game in progress.", replyToMessageId = message.messageId)
        } else {
            val command = message.entities!!.first()
            val letter = message.text!!.substring(command.offset + command.length).trim()
                .takeIf { it.length == 1 }
                ?.firstOrNull()
                ?.takeIf { it.isLetterOrDigit() }
            if (letter == null) {
                bot.sendMessage(chatId, "Please guess a letter or digit.", replyToMessageId = message.messageId)
            } else {
                when (game.guessLetter(letter)) {
                    Game.State.CONTINUE ->
                        bot.sendMessage(chatId, game.toString(), replyToMessageId = message.messageId,
                            parseMode = ParseMode.MARKDOWN)

                    Game.State.WIN -> {
                        bot.sendMessage(chatId, "You win!\n\n$game", replyToMessageId = message.messageId,
                            parseMode = ParseMode.MARKDOWN)
                        games.remove(chatId)
                    }

                    Game.State.LOSE -> {
                        bot.sendMessage(chatId, "You lose! The word was `${game.word}`.\n\n$game",
                            replyToMessageId = message.messageId, parseMode = ParseMode.MARKDOWN)
                        games.remove(chatId)
                    }
                }
            }
        }
    }

    private fun fetchRandomWord(): String {
        val url = tryCreateUrl("https://en.wiktionary.org/wiki/Special:RandomInCategory/English_lemmas")
        return Jsoup.parse(wget(url)).getElementById("firstHeading").text().trim()
    }

    companion object {
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
