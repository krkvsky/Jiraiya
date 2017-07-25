package jira

import bot.{UserClient, Util}
import com.atlassian.jira.rest.client.api.domain.input.WorklogInput
import models.{IssueWorkingDB, UserDB}
import org.joda.time.DateTime
import models.Issues._

/**
  * Created by yarik on 25.07.17.
  */
object Worklog {

  def addWorklog(current: IssueWorkingDB, startAt: Long, workToLog: Long, key: String, userClient: UserClient)  = {
    val issue = Issue.getOriginalIssueFromJira(getIssueById(current.issueId, userClient).get.key, userClient)
    val worklogURI = issue.getWorklogUri
    val worklog = WorklogInput.create(issue.getSelf, null, new DateTime(startAt), Util.millisToMinutes(workToLog), null)
    userClient.rest.getIssueClient.addWorklog(worklogURI, worklog)
  }

  def getWorklogByUser(user: UserDB, userClient: UserClient) = {
    userClient.rest.getIssueClient
  }

}
