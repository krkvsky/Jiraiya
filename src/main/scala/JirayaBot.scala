import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, PoisonPill, Props}
import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.Issue
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative._
import info.mukel.telegrambot4s.methods.{EditMessageReplyMarkup, ParseMode, SendMessage}
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message, ReplyKeyboardMarkup}
import info.mukel.telegrambot4s.Implicits._
import slick.driver.SQLiteDriver.api._

import scala.concurrent.duration._
import Data._

import scala.concurrent.Future
//import akka.stream.actor.ActorPublisher.Internal.Canceled
import org.apache.http.concurrent.Cancellable

import scala.concurrent.duration.Duration

object JirayaBot extends TelegramBot with Polling with Commands with Callbacks with Authentication {
  def token = "393149916:AAFf7YWkfwhUa2PlXRPXPk-anHmYvusFyco"

  def projectTag = prefixTag("PROJECTS_TAG") _
  def issueTag = prefixTag("ISSUES_TAG") _

  def markupProjects(projects: List[ProjectDB]) = {
    InlineKeyboardMarkup(projects.grouped(2).map(y => y.map(x => InlineKeyboardButton.callbackData(
        s"Project ${x.key}",
        projectTag(x.key)))
      ).toSeq
    )
  }

  def markupIssues(issues: List[IssueDB]) = {
    InlineKeyboardMarkup(issues.take(5).grouped(3).map(y => y.map(x => InlineKeyboardButton.callbackData(
      s"${x.key}",
      issueTag(x.key))).toSeq
    ).toSeq
    )
  }

  onCallbackWithTag("PROJECTS_TAG") { implicit cbq =>
    val user = isAuthenticated(cbq.from).get
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val project = getProjectByKey(cbq.data.get)
    println(project)
    println(cbq.data.get)
    println(s"Project id: $project.id")
    val issues = getIssuesByUser(getUser(cbq.message.get.source), user).filter(x => x.projectID == project.id.get)
    println("final")
    request(SendMessage(cbq.message.get.source, "Issues: ", replyMarkup = markupIssues(issues)))
    }

  onCallbackWithTag("ISSUES_TAG") { implicit cbq =>
    val user = isAuthenticated(cbq.from).get
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issue = getIssue(cbq.data.get, user)
    request(SendMessage(cbq.message.get.source, showIssue(issue), parseMode = Some(ParseMode.Markdown)))
  }


  onCommand("/start") { implicit msg =>
    reply("TYPE FUCKING /login TO GUESS WHATT ?? TO LOGIN MAYBE YOU STUPID REDO")
  }

  onCommand("/projects") { implicit msg =>
    authenticatedOrElse {
      admin => {
        val projects = getProjectsByUser(getUser(msg.source), admin)
        println(projects)
        reply("Projects: ", replyMarkup = markupProjects(projects))
      }
    } /* or else */ {
      user =>
        reply(s"${user.firstName}, you must /login first.")
    }
  }

  onCommand("/login") { implicit msg =>
    val (username: String, password: String) = msg.text.getOrElse("").split(" ") match {
      case Array(_, y, z) => (y, z)
      case _ => ("", "")
    }
    if(username != ""){
      val loginResult = login(msg.from.get, username, password)
      reply("wait few minutes to synchronize your data")
      if(loginResult.isDefined){
        val user: UserDB = if(userFirst(msg.source)){
          firstLaunch(msg.source, username, loginResult.get)
        } else getUser(msg.source, username)
        val system = ActorSystem("CheckerValidatorSystem")
        val validator = system.actorOf(Props(new Validator(request)), name = "validator")
        val checker = system.actorOf(Props(new Checker((msg.source, loginResult.get), validator)), name = "checker")
        checker ! StartMessage
        reply("login successful")
      } else
        reply("login failed")
    } else {
      reply("Correct format is '/login username password'")
    }

  }

  onCommand("/logout") { implicit msg =>
    msg.from.foreach(logout)
    reply("You cannot access /secret anymore. Bye bye!" )
  }

  onCommand("/project") { implicit msg =>
    authenticatedOrElse{
      admin => {
        val project: String = msg.text.get.split(" ") match {//getOrElse("").split(" ") match {
          case Array(_, y) => y
          case _ => ""
        }
        if(project != ""){
          val projectIssues = admin.rest.getSearchClient().searchJql(s"project=$project and assignee=${admin.rest.getSessionClient().getCurrentSession.claim().getUsername}").claim().getIssues
          //        println(myProjects.claim().getIssues().iterator().next().toString)

          projectIssues.forEach(x => {
            val time = x.getStatus
            println(time)
            val message = s"[${x.getKey}](http://jira.tallium.com:8085/browse/${x.getKey})\n" +
              s"`${x.getDescription}`"
            reply(message, Some(ParseMode.Markdown))

          })
        } else {
          reply("Correct format is '/project PROJECTNAME'\n" +
            "Example: /project GEEKS")
        }
      }
    } { user =>
      reply(s"${user.firstName}, you must /login first.")
    }

  }

  onCommand("/issues") { implicit msg =>
    authenticatedOrElse {
      admin => {
        val issues = getIssuesByUsername(getUser(msg.source).firstName, admin)
        issues.foreach(x => reply(x.key + ":\n" + x.description))
      }
    } /* or else */ {
      user =>
        reply(s"${user.firstName}, you must /login first.")
    }
  }

  def showIssues(iss : List[IssueDB]): List[String] ={
    iss.map(showIssue)
  }

  def briefIssues(iss : List[IssueDB]): List[String] ={
    iss.map(briefIssue)
  }

  def showIssue(iss: IssueDB): String = s"[${iss.key}](http://jira.tallium.com:8085/browse/${iss.key})\n" +
    s"STATUS: ${iss.status} \n" +
    s" ${iss.description.getOrElse("None description provided :(")}"
  def briefIssue(iss: IssueDB): String = s"[${iss.key}](http://jira.tallium.com:8085/browse/${iss.key})\n STATUS: ${iss.status}"

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
        if(l.length < 25) {
          req(SendMessage(chatID, "YOU HAVE NEW AND UPDATED ISSUES: "))
          showIssues(l).foreach(x => req(SendMessage(chatID, x, Some(ParseMode.Markdown))))
        }
    }
  }
}

