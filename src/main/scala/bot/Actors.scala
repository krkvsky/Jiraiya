package bot

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import Util._
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.methods.{ParseMode, SendMessage}
import models.Users._
import bot.Authentication

object Actors {

  class Validator(req: RequestHandler) extends Actor {
    def receive = {
      case tup: (Long, UserClient) => {

        val chatID = tup._1
        val l = updateUserIssuesActor(getUser(chatID), tup._2)
        if(l.nonEmpty){
          if(l.nonEmpty && l.length < 25) {
            val newIssues = l.filter(_._1).map(_._2)
            val updatedIssues = l.filterNot(_._1).map(_._2)
            if(newIssues.nonEmpty) {
              req(SendMessage(chatID, "YOU HAVE NEW ISSUES: "))
              Thread.sleep(1000)
              showIssues(newIssues).foreach(x => req(SendMessage(chatID, x, Some(ParseMode.Markdown))))
            }
            if (updatedIssues.nonEmpty) {
              req(SendMessage(chatID, "YOU HAVE UPDATED ISSUES: "))
              Thread.sleep(1000)
              showIssues(updatedIssues).foreach(x => req(SendMessage(chatID, x, Some(ParseMode.Markdown))))
            }
          }
        }
        Thread.sleep(120000)
        self ! tup
      }
    }
  }



}
