package bot

import Util._
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.methods.{ParseMode, SendMessage}
import models.Projects._
import models.Issues._
import models.Users._
import models.IssueWorking._
import Markup._
import JirayaBot._

object Callbacks {

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
    if(getIssueWorkingOn(getUser(cbq.message.get.source), user).isDefined)
      request(SendMessage(cbq.message.get.source, showIssue(issue), parseMode = Some(ParseMode.Markdown)))
    else
      request(SendMessage(cbq.message.get.source, showIssue(issue), parseMode = Some(ParseMode.Markdown), replyMarkup = markupInteractive(issue)))

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

  onCallbackWithTag("FINISH_STATUS_TAG") { implicit cbq =>
    val userClient = isAuthenticated(cbq.from).get
    val user = getUser(cbq.message.get.source)
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issueWork = if(getIssueWorkingOn(user, userClient).isDefined) getIssueWorkingOn(user, userClient) else getIssueWorkingOnPaused(user, userClient)
    val issue = getIssueById(issueWork.get.issueId, userClient).get
    val time = Util.millisToMinutes(finishIssueWorkingOn(user, userClient))
    request(SendMessage(cbq.message.get.source, s"Work on ${issue.key} was finished! You've spent $time minutes on it!"))
  }

  onCallbackWithTag("PAUSE_STATUS_TAG") { implicit cbq =>
    println("here pause")
    val userClient = isAuthenticated(cbq.from).get
    val user = getUser(cbq.message.get.source)
    println("here pause 2")
    // Notification only shown to the user who pressed the button.
    ackCallback(cbq.from.firstName + " pressed the button!")
    // Or just ackCallback()
    val issueWork = getIssueWorkingOn(user, userClient)
    val issue = getIssueById(issueWork.get.issueId, userClient).get
    println("here pause3")
    pauseIssueWorkingOn(user, userClient)
    println("here pause4")
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

}
