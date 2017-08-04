package models

import slick.driver.SQLiteDriver.api._
import Users.users
import Issues._
import jira.Issue._
import bot.UserClient

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class UserIssueDB(userId: Long, issueId: Long)

class UserIssues(tag: Tag) extends Table[UserIssueDB](tag, "USER_ISSUES"){
  def userId = column[Long]("USER_ID")
  def issueId = column[Long]("ISSUE_ID")

  def * = (userId, issueId) <> (UserIssueDB.tupled, UserIssueDB.unapply)
  def user = foreignKey("U_FK", userId, users)(_.id.get)
  def issue = foreignKey("I_FK", issueId, issues)(_.id.get)
}

object UserIssues {
  val userissues = TableQuery[UserIssues]
  val db = Database.forConfig("db")

  def getUserIssue(userId: Long, issueId: Long): UserIssueDB = {
    val userIssue = Await.result(db.run(userissues.filter(x => x.userId === userId && x.issueId === issueId).result), Duration.Inf)
    if(userIssue.nonEmpty)
      userIssue.head
    else
      insertUserIssue(UserIssueDB(userId, issueId))
  }

  def getChangedIssues(userId: Long, issueId: Long, userClient: UserClient): (Boolean, Option[UserIssueDB])= {
    val userIssue = Await.result(db.run(userissues.filter(x => x.userId === userId && x.issueId === issueId).result), Duration.Inf)
    if(userIssue.nonEmpty) {
      val issue = getIssueById(issueId, userClient).get
      val jiraIssue = getIssueFromJira(issue.key, userClient)
      if(jiraIssue.updatedAt == issue.updatedAt) {
        println("not new and updated")
        (false, None)
      }
      else{
        val action = issues.filter(x => x.id === issueId).update(jiraIssue)
//        val action = q.update(jiraIssue)
        Await.result(db.run(action), Duration.Inf)
        println("updated " + issueId)
        (false, Some(UserIssueDB(userId, issueId)))
      }
    }
    else {
      println("new " + issueId)
      (true, Some(insertUserIssue(UserIssueDB(userId, issueId))))
    }
  }


  def getNewUserIssue(userId: Long, issueId: Long, userClient: UserClient): Option[UserIssueDB] = {
    val userIssue = Await.result(db.run(userissues.filter(x => x.userId === userId && x.issueId === issueId).result), Duration.Inf)
    if(userIssue.nonEmpty) {
      val issue = getIssueById(issueId, userClient).get
      val jiraIssue = getIssueFromJira(issue.key, userClient)
      if(jiraIssue.updatedAt == issue.updatedAt) {
        println("not new and updated")
        None
      }
      else{
        val iss = issues.filter(x => x.id === issueId)
        val q = for { i <- iss } yield i.updatedAt
        val action = q.update(jiraIssue.updatedAt)
        Await.result(db.run(action), Duration.Inf)
        println("updated " + issueId)
        Some(UserIssueDB(userId, issueId))
      }
    }
    else {
      println("new " + issueId)
      Some(insertUserIssue(UserIssueDB(userId, issueId)))
    }
  }

  def insertUserIssue(userIssue: UserIssueDB): UserIssueDB = {
    Await.result(db.run(
      userissues += userIssue
    ), Duration.Inf)
    getUserIssue(userIssue.userId, userIssue.issueId)
  }

}
