package bot

import JirayaBot._
import models.{IssueDB, IssueWorkingDB, ProjectDB}
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message, ReplyKeyboardMarkup}
import info.mukel.telegrambot4s.Implicits._

object Markup {
  def projectTag = prefixTag("PROJECTS_TAG") _
  def issueTag = prefixTag("ISSUES_TAG") _
  def interactiveTag = prefixTag("INTERACTIVE_TAG") _
  def interactiveSwitchTag = prefixTag("INTERACTIVE_SWITCH_TAG") _

  def statusesTag = prefixTag("STATUSES_TAG") _
  def statusIssueTag = prefixTag("STATUS_ISSUE_TAG") _
  def statusFinish = prefixTag("FINISH_STATUS_TAG") _
  def statusPause = prefixTag("PAUSE_STATUS_TAG") _
  def statusResume = prefixTag("RESUME_STATUS_TAG") _

  def markupProjects(projects: List[ProjectDB]) = {
    InlineKeyboardMarkup(projects.grouped(2).map(y => y.map(x => InlineKeyboardButton.callbackData(
      s"Project ${x.key}",
      projectTag(x.key)))
    ).toSeq
    )
  }

  def markupIssues(issues: List[IssueDB], projectKey: String, offset: Int = 0) = {
    print("WE ARE HERE")
    InlineKeyboardMarkup(InlineKeyboardButton.callbackData("<", projectTag(s"$projectKey^${offset-5}")) ++
      issues.slice(offset, offset + 5).grouped(1).map(y => y.map(x => InlineKeyboardButton.callbackData(
        s"${x.key}: ${x.name}",
        issueTag(x.key))).toSeq
      ).toSeq ++ InlineKeyboardButton.callbackData(">", projectTag(s"$projectKey^${offset+5}"))
    )
  }

  def markupStatus(issue: IssueWorkingDB) = {
    InlineKeyboardMarkup(
      InlineKeyboardButton.callbackData(
        "Finish",
        statusFinish(issue.issueId.toString)
      ).toSeq ++ (if(issue.paused) {
        InlineKeyboardButton.callbackData(
          "Resume",
          statusResume(issue.issueId.toString)
        ).toSeq
      } else {
        InlineKeyboardButton.callbackData(
          "Pause",
          statusPause(issue.issueId.toString)
        ).toSeq
      })
    )
  }

  def markupStatusIssues(issues: List[IssueWorkingDB], issueNames: Map[Option[Long], String]) = {
    InlineKeyboardMarkup(
      issues.map(x => InlineKeyboardButton.callbackData(
        s"${issueNames(Some(x.issueId))}",
        statusIssueTag(x.issueId.toString))
      )
    )
  }


  def markupInteractive(issue: IssueDB, switch: Boolean) = {
    InlineKeyboardMarkup(
      if(!switch) {
        InlineKeyboardButton.callbackData(
          "Start work",
          interactiveTag(issue.key)
        ).toSeq
      } else {
        InlineKeyboardButton.callbackData(
          "Switch",
          interactiveSwitchTag(issue.key)
        ).toSeq
      }
    )
  }
}
