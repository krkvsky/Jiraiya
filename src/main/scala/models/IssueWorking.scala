package models

import com.atlassian.jira.rest.client.api.domain.input.WorklogInput
import org.joda.time.DateTime
import slick.driver.SQLiteDriver.api._
import Users.users
import Issues._
import jira.Worklog._
import jira.Issue._
import bot.{UserClient, Util}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


/**
  * Created by yarik on 25.07.17.
  */
case class IssueWorkingDB(userId: Long,  issueId: Long, startedAt: Long, continuedAt:Long, time: Long, paused: Boolean, finished: Boolean)

class IssueWorking(tag: Tag) extends Table[IssueWorkingDB](tag, "ISSUE_WORKING"){
  def userId = column[Long]("USER_ID")
  def issueId = column[Long]("ISSUE_ID")
  def startedAt = column[Long]("STARTED_AT")
  def continuedAt = column[Long]("CONTINUED_AT")
  def time = column[Long]("TIME")
  def paused = column[Boolean]("PAUSED")
  def finished = column[Boolean]("FINISHED")

  def * = (userId, issueId, startedAt, continuedAt, time, paused, finished) <> (IssueWorkingDB.tupled, IssueWorkingDB.unapply)
  def user = foreignKey("U_FK", userId, users)(_.id.get)
  def issue = foreignKey("I_FK", issueId, issues)(_.id.get)

}

object IssueWorking {
  val issueworking= TableQuery[IssueWorking]
  val db = Database.forConfig("db")

  def getIssueWorkingOn(user: UserDB, userClient: UserClient): Option[IssueWorkingDB] = {
    Await.result(db.run(issueworking.filter(x => x.userId === user.id && !x.paused && !x.finished).result), Duration.Inf).headOption
  }

  def getIssueWorkingOnPaused(user: UserDB, userClient: UserClient): Option[IssueWorkingDB] = {
    Await.result(db.run(issueworking.filter(x => x.userId === user.id && x.paused && !x.finished).result), Duration.Inf).headOption
  }


  def startIssueWorkingOn(issue: IssueDB, user: UserDB, userClient: UserClient) = {
    Await.result(db.run(
      issueworking += IssueWorkingDB(user.id.getOrElse(0), issue.id.getOrElse(0), System.currentTimeMillis(), 0, 0, false, false)
    ), Duration.Inf)
  }

  def pauseIssueWorkingOn(user: UserDB, userClient: UserClient) = {
    println("pausing")
    val current = getIssueWorkingOn(user, userClient)
    println(current)
    if(current.isDefined) {
      val curr = current.get
      val previous = if(curr.continuedAt != 0) curr.continuedAt else curr.startedAt
      println("Start pause")
      val workToLog = System.currentTimeMillis() - previous
      addWorklog(curr, previous, workToLog, userClient)
//      val issue = getOriginalIssueFromJira(getIssueById(curr.issueId, userClient).get.key, userClient)
//      val worklogURI = issue.getWorklogUri
//      val worklog = WorklogInput.create(issue.getSelf, null, new DateTime(previous), Util.millisToMinutes(workToLog), null)
//      userClient.rest.getIssueClient.addWorklog(worklogURI, worklog)
      println("end pause")
      val newTime = curr.time + workToLog
      Await.result(
        db.run(
          issueworking.filter(x => x.issueId === curr.issueId && x.userId === curr.userId).map(x => (x.paused, x.time)).update(true, newTime)
        ), Duration.Inf
      )
    }
  }

  def resumeIssueWorkingOn(current: IssueWorkingDB, user: UserDB, userClient: UserClient) = {
    Await.result(
      db.run(
        issueworking.filter(x => x.issueId === current.issueId && x.userId === current.userId).map(x => (x.paused, x.continuedAt)).update(false, System.currentTimeMillis())
      ), Duration.Inf
    )
  }

  def finishIssueWorkingOn(user: UserDB, userClient: UserClient): Long = {
    val currentGo = getIssueWorkingOn(user, userClient)
    val currentPaused = getIssueWorkingOnPaused(user, userClient)
    val current = if(currentGo.isDefined) currentGo else currentPaused

    if(current.isDefined) {
      val curr = current.get
      val previous = if(curr.continuedAt != 0) curr.continuedAt else curr.startedAt
      val workToLog = System.currentTimeMillis() - previous
      val key = addWorklog(curr, previous, workToLog, userClient)
//      val issue = getOriginalIssueFromJira(getIssueById(curr.issueId, userClient).get.key, userClient)
//      val worklogURI = issue.getWorklogUri
//      val worklog = WorklogInput.create(issue.getSelf, null, new DateTime(previous), Util.millisToMinutes(workToLog), null)
//      userClient.rest.getIssueClient.addWorklog(worklogURI, worklog)
      val newTime = curr.time + workToLog
      Await.result(
        db.run(
          issueworking.filter(x => x.issueId === curr.issueId && x.userId === curr.userId).map(x => (x.time, x.finished)).update(newTime, true)
        ), Duration.Inf
      )
      moveIssueToTesting(key, userClient)
      newTime
    }else
      0
  }

}
