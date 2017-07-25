package bot

import JirayaBot._
import models.{IssueDB, IssueWorkingDB, ProjectDB}
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message, ReplyKeyboardMarkup}
import info.mukel.telegrambot4s.Implicits._

object Markup {
  def projectTag = prefixTag("PROJECTS_TAG") _
  def issueTag = prefixTag("ISSUES_TAG") _
  def interactiveTag = prefixTag("INTERACTIVE_TAG") _

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

  def markupIssues(issues: List[IssueDB]) = {
    InlineKeyboardMarkup(issues.take(5).grouped(3).map(y => y.map(x => InlineKeyboardButton.callbackData(
      s"${x.key}",
      issueTag(x.key))).toSeq
    ).toSeq
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

  def markupInteractive(issue: IssueDB) = {
    InlineKeyboardMarkup(
      InlineKeyboardButton.callbackData(
        "Start work",
        interactiveTag(issue.key)
      ).toSeq
    )
  }

}
