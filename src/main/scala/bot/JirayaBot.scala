package bot

import akka.actor.{ActorSystem, Props}
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative._
import info.mukel.telegrambot4s.methods.{EditMessageReplyMarkup, ParseMode, SendMessage}
import models._
import models.Projects._
import models.Issues._
import models.Users._
import models.IssueWorking._
import jira.Issue._
import Markup._
import Actors._
import Util._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object JirayaBot extends TelegramBot with Polling with Commands with Callbacks with Authentication {

//  lazy val token = scala.util.Properties
//    .envOrNone("BOT_TOKEN")
//    .getOrElse(Source.fromFile("bot.token").getLines().mkString)

  def token = "393149916:AAFf7YWkfwhUa2PlXRPXPk-anHmYvusFyco"
  val mySystem = ActorSystem("CheckerValidatorSystem")


  onCallbackWithTag("PROJECTS_TAG") { implicit cbq =>
    val user = isAuthenticated(cbq.from).get
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val (key, offs) = cbq.data.get.splitAt(cbq.data.get.indexOf("^"))
    if (key.isEmpty){
      val project = getProjectByKey(offs)
      println(project)
      println(cbq.data.get)
      println(s"Project id: ${project.id}")
      val issues  = getIssuesDBByProject(project.id.get, getUser(cbq.message.get.source), user)
//      val issues = getIssuesByUser(getUser(cbq.message.get.source), user).filter(x => x.projectID == project.id.get)
      println("final")
      request(SendMessage(cbq.message.get.source, "Issues: ", replyMarkup = markupIssues(issues, cbq.data.get)))
    } else {
      val offset = offs.drop(1).toInt
      val project = getProjectByKey(key)
//      val issues = getIssuesByUser(getUser(cbq.message.get.source), user).filter(x => x.projectID == project.id.get)
      val issues = getIssuesDBByProject(project.id.get, getUser(cbq.message.get.source), user)
      if(issues.slice(offset, offset+5).nonEmpty)
        request(EditMessageReplyMarkup(cbq.message.get.source, cbq.message.get.messageId, replyMarkup = markupIssues(issues, key, offset)))
//      request(SendMessage(cbq.message.get.source, "Issues: ", replyMarkup = markupIssues(issues, key, offs.drop(1).toInt)))
    }
  }

  onCallbackWithTag("ISSUES_TAG") { implicit cbq =>
    val user = isAuthenticated(cbq.from).get
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issue = getIssue(cbq.data.get, user)
    val workingIss = getIssueWorkingOn(getUser(cbq.message.get.source), user)
    val issues = getIssuesWorkingOn(getUser(cbq.message.get.source), user)
    val isCurrent = workingIss.isDefined && workingIss.get.issueId == issue.id.get
    if (isCurrent)
      request(SendMessage(cbq.message.get.source, showIssue(issue), parseMode = Some(ParseMode.Markdown)))
    else
      request(SendMessage(cbq.message.get.source, showIssue(issue), parseMode = Some(ParseMode.Markdown), replyMarkup = markupInteractive(issue, workingIss.isEmpty)))
  }

  onCallbackWithTag("INTERACTIVE_TAG") { implicit cbq =>
    val userClient = isAuthenticated(cbq.from).get
    val user = getUser(cbq.message.get.source)
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issue = getIssue(cbq.data.get, userClient)
    startIssueWorkingOn(issue, user, userClient)
    request(SendMessage(cbq.message.get.source, s"Work on ${issue.key} was started!"))
  }

  onCallbackWithTag("INTERACTIVE_SWITCH_TAG") { implicit cbq =>
    val userClient = isAuthenticated(cbq.from).get
    val user = getUser(cbq.message.get.source)
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issue = getIssue(cbq.data.get, userClient)
    switchIssueWorkingOn(issue, user, userClient)
    request(SendMessage(cbq.message.get.source, s"Work on ${issue.key} was started!"))
  }

  onCallbackWithTag("STATUSES_TAG") { implicit cbq =>
    val user = isAuthenticated(cbq.from).get
    val issues = getIssuesWorkingOn(getUser(cbq.message.get.source), user)
    val issueNames = issues.map(x => getIssueById(x.issueId, user).get).map(x => x.id -> (x.key+":"+x.name)).toMap
    request(SendMessage(cbq.message.get.source, "Working on:", replyMarkup = markupStatusIssues(issues, issueNames)))
  }

  onCallbackWithTag("STATUS_ISSUE_TAG") { implicit cbq =>
    val user = isAuthenticated(cbq.from).get
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val query = cbq.data.get
    val current = getIssueWorkingOnById(query.toInt, getUser(cbq.message.get.source), user)
    if (current.isDefined) {
      val curr = current.get
      if(curr.paused){
        val issue = getIssueById(curr.issueId, user).get
        request(SendMessage(cbq.message.get.source, s"You were working on ${issue.key} for ${millisFormat(curr.time)}", replyMarkup = markupStatus(curr)))
      } else {
        val previous = if (curr.continuedAt != 0) curr.continuedAt else curr.startedAt
        val newTime = Util.millisFormat(curr.time + (System.currentTimeMillis() - previous))
        val issue = getIssueById(curr.issueId, user).get
        request(SendMessage(cbq.message.get.source, s"You are working on ${issue.key} for $newTime", replyMarkup = markupStatus(curr)))
      }
    }
  }

  onCallbackWithTag("FINISH_STATUS_TAG") { implicit cbq =>
    val userClient = isAuthenticated(cbq.from).get
    val user = getUser(cbq.message.get.source)
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issueWork = if(getIssueWorkingOn(user, userClient).isDefined) getIssueWorkingOn(user, userClient) else getIssueWorkingOnPaused(user, userClient)
    val issue = getIssueById(issueWork.get.issueId, userClient).get
    val time = Util.millisFormat(finishIssueWorkingOn(user, userClient))
    request(SendMessage(cbq.message.get.source, s"Work on ${issue.key} was finished! You've spent ${time}!"))
  }

  onCallbackWithTag("PAUSE_STATUS_TAG") { implicit cbq =>
    val userClient = isAuthenticated(cbq.from).get
    val user = getUser(cbq.message.get.source)
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issueWork = getIssueWorkingOn(user, userClient)
    val issue = getIssueById(issueWork.get.issueId, userClient).get
    pauseIssueWorkingOn(user, userClient)
    request(SendMessage(cbq.message.get.source, s"Work on ${issue.key} was paused!"))
  }

  onCallbackWithTag("RESUME_STATUS_TAG") { implicit cbq =>
    val userClient = isAuthenticated(cbq.from).get
    val user = getUser(cbq.message.get.source)
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issueWork = getIssueWorkingOnPaused(user, userClient)
    val issue = getIssueById(issueWork.get.issueId, userClient).get
    resumeIssueWorkingOn(issueWork.get, user, userClient)
    request(SendMessage(cbq.message.get.source, s"Work on ${issue.key} was resumed!"))
  }

  onCommand("/start") { implicit msg =>
    reply("type /login USERNAME PASSWORD")
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

  onCommand("/status") { implicit msg =>
    authenticatedOrElse {
      admin => {
        val issues = getIssuesWorkingOn(getUser(msg.source), admin)
        val current = getIssueWorkingOn(getUser(msg.source), admin)
        println("1")
        if(issues.isEmpty){
          println("2")
          reply("You are not working now")
        } else if(issues.length == 1) {
          println("3")
          if (current.isDefined) {
            val curr = current.get
            val previous = if (curr.continuedAt != 0) curr.continuedAt else curr.startedAt
            val newTime = Util.millisFormat(curr.time + (System.currentTimeMillis() - previous))
            val issue = getIssueById(curr.issueId, admin).get
            reply(s"You are working on ${issue.key} for $newTime", replyMarkup = markupStatus(curr))
          } else {
            val curr = getIssueWorkingOnPaused(getUser(msg.source), admin).get
            val issue = getIssueById(curr.issueId, admin).get
            reply(s"You were working on ${issue.key} for ${millisFormat(curr.time)}", replyMarkup = markupStatus(curr))
          }
        } else if(issues.length > 1 && current.isDefined){
          val curr = current.get
          val previous = if (curr.continuedAt != 0) curr.continuedAt else curr.startedAt
          val newTime = Util.millisFormat(curr.time + (System.currentTimeMillis() - previous))
          val issue = getIssueById(curr.issueId, admin).get
          val issueNames = issues.map(x => getIssueById(x.issueId, admin).get).map(x => x.id -> (x.key+":"+x.name)).toMap
          reply(s"You are working on ${issue.key} for $newTime", replyMarkup = markupStatusIssues(issues, issueNames))
        }else if(issues.length > 1 && current.isEmpty){
          val issueNames = issues.map(x => getIssueById(x.issueId, admin).get).map(x => x.id -> (x.key+":"+x.name)).toMap
          reply(s"You are working on ${issues.length} issues: ", replyMarkup = markupStatusIssues(issues, issueNames))
        }
      }
    } /* or else */ {
      user =>
        reply(s"${user.firstName}, you must /login first.")
    }
  }


  onCommand("/login") { implicit msg =>
    authenticatedOrElse { admin => {
      reply(s"You are authorized as ${admin.user.firstName}")
    }
    }{ user =>
      val (username: String, password: String) = msg.text.getOrElse("").split(" ") match {
        case Array(_, y, z) => (y, z)
        case _ => ("", "")
      }
      if (username != "") {
        val loginResult = login(msg.from.get, username, password, request)
        reply("wait few minutes to synchronize your data")
        if (loginResult._1.isDefined) {
          println(loginResult)
          println(userFirst(msg.source))
          val user: UserDB = if (userFirst(msg.source)) {
            println("here with beautiful username" + username)
            firstLaunch(msg.source, username, loginResult._1.get)
          } else getUser(msg.source, username)
          loginResult._2 ! (msg.source, loginResult._1.get)
          reply("login successful")
        } else
          reply("login failed")
      } else {
        reply("Correct format is '/login username password'")
      }

    }
  }

  onCommand("/logout") { implicit msg =>
    msg.from.foreach(logout)
    reply("Bye bye!" )
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

  onCommand("/logs") { implicit msg =>
    authenticatedOrElse {
      admin => {
        val date = msg.text.getOrElse("").split(" ")
        val worklogs = if(date.length > 1) getUserWorklogs(DateTimeFormat.forPattern("yyyy/MM/dd").parseDateTime(date(1)), admin) else
          getUserYesterdayWorklogs(admin)
        worklogs.foreach(x => reply(briefIssue(x._1)+ ":\n" + (x._2/3600) + "h " + (x._2/60)%60 + "m", parseMode = Some(ParseMode.Markdown)))
      }
    } /* or else */ {
      user =>
        reply(s"${user.firstName}, you must /login first.")
    }
  }

}

