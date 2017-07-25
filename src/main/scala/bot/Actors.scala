package bot

import akka.actor.{Actor, ActorRef, PoisonPill}
import Util._
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.methods.{ParseMode, SendMessage}
import models._
import models.Users._
import JirayaBot._

object Actors {

  class Checker(tup: (Long, UserClient), validator: ActorRef) extends Actor {
    def receive = {
      case StartMessage =>
        while(isAuthenticated(tup._2.user).isDefined) {
          val issues = updateUserIssuesActor(getUser(tup._1), tup._2)
          if(issues.nonEmpty)
            validator ! (tup._1, issues)
          Thread.sleep(120000)
        }
        validator ! PoisonPill
        self ! PoisonPill
    }
  }

  class Validator(req: RequestHandler) extends Actor {
    def receive = {
      case (chatID: Long, l: List[IssueDB])=>
        if(l.nonEmpty && l.length < 25) {
          req(SendMessage(chatID, "YOU HAVE NEW AND UPDATED ISSUES: "))
          showIssues(l).foreach(x => req(SendMessage(chatID, x, Some(ParseMode.Markdown))))
        }
    }
  }



}
