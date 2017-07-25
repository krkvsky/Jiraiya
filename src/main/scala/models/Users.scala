package models

import slick.driver.SQLiteDriver.api._
import bot.UserClient
import jira.Issue._
import models.Issues._
import models.UserIssues._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class UserDB(id: Option[Long], chatid: Long, firstName: String)

class Users(tag: Tag) extends Table[UserDB](tag, "USERS") {
  //     Columns
  def id = column[Option[Long]]("USER_ID", O.PrimaryKey, O.AutoInc)

  def chatid = column[Long]("USER_CHAT_ID", O.Length(10))

  def firstName = column[String]("USER_FIRST_NAME", O.Length(64))

  // Indexes
  def chatidIndex = index("USER_EMAIL_IDX", chatid, true)

  // Select
  def * = (id, chatid, firstName) <> (UserDB.tupled, UserDB.unapply)
}

object Users {
  val users = TableQuery[Users]
  val db = Database.forConfig("db")

  def updateUserIssues(user: UserDB, userClient: UserClient): List[IssueDB] = {
    //    val userIssues = getIssuesByUser(user, userClient)
    println("START SYNCHRONIZE " + user.firstName)
    val userIssues = getIssuesByUserJiraFirst(user, userClient)
    val unfiltered = for(issue <- userIssues) yield getNewUserIssue(user.id.get, issue.id.get, userClient)
    println("END OF SYNCHRONIZE")
    unfiltered.filter(_.isDefined).map(j => getIssueById(j.get.issueId, userClient)).filter(j => j.isDefined).map(_.get)
  }

  def updateUserIssuesActor(user: UserDB, userClient: UserClient): List[IssueDB] = {
    //    val userIssues = getIssuesByUser(user, userClient)
    val userIssues = getIssuesByUserJira(user, userClient)
    val unfiltered = for(issue <- userIssues) yield getNewUserIssue(user.id.get, issue.id.get, userClient)
    unfiltered.filter(_.isDefined).map(j => getIssueById(j.get.issueId, userClient)).filter(j => j.isDefined).map(_.get)
  }

  def userFirst(chatid: Long): Boolean = {
    return !Await.result(db.run(users.filter(_.chatid === chatid).result), Duration.Inf).nonEmpty
  }

  def getUser(chatid: Long, name: String = ""): UserDB = {
    val user= Await.result(db.run(users.filter(_.chatid === chatid).result), Duration.Inf)
    if(user.nonEmpty)
      user.head
    else
      insertUser(UserDB(None, chatid, name))
  }

  def insertUser(user: UserDB): UserDB = {
    Await.result(db.run(
      users += user
    ), Duration.Inf)
    getUser(user.chatid)
  }

  def firstLaunch(chatid: Long, name: String, userClient: UserClient): UserDB = {
    val user = getUser(chatid, name)
    updateUserIssues(user, userClient)
    user
  }
}
