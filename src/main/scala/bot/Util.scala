package bot

import models.IssueDB

/**
  * Created by yarik on 25.07.17.
  */
object Util {

  def showIssues(iss : List[IssueDB]): List[String] ={
    iss.map(showIssue)
  }

  def briefIssues(iss : List[IssueDB]): List[String] ={
    iss.map(briefIssue)
  }

  def showIssue(iss: IssueDB): String = s"[${iss.key}](http://jira.tallium.com:8085/browse/${iss.key})\n" +
    s"STATUS: ${iss.status} \n" +
    s" ${iss.description.getOrElse("None description provided :(")}"
  def briefIssue(iss: IssueDB): String = s"[${iss.key}](http://jira.tallium.com:8085/browse/${iss.key}) ${iss.name}"

  def issueIsActive(iss: IssueDB): Boolean = {
    List("in progress", "open", "reopened").contains(iss.status.toLowerCase)
  }

  def millisToMinutes(milliseconds: Long): Int = (milliseconds / 60000).toInt
  def millisFormat(milliseconds: Long): String = s"${millisToMinutes(milliseconds) / 60}h ${millisToMinutes(milliseconds) % 60}m"
}
