import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.Message
import info.mukel.telegrambot4s.Implicits._

import scala.concurrent.Future

object JirayaBot extends TelegramBot with Polling with Commands {
  def token = "393149916:AAFf7YWkfwhUa2PlXRPXPk-anHmYvusFyco"

  onCommand("/naruto") { implicit msg => reply("alo")}
  onCommand("/saske") { implicit msg =>
    f
  }

  def f: Unit = Future {
    Thread.sleep(2000)
    request(SendMessage(257888125L, "saske"))
    f
  }
}