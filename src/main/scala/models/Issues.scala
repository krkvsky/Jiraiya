package models

import slick.driver.SQLiteDriver.api._
import Projects._
import UserIssues._
import bot.UserClient
import jira.Issue._
import bot.Util._

import scala.concurrent.Await
import scala.concurrent.duration.Duration


case class IssueDB(id: Option[Long], key: String, name: String, status: String, updatedAt: String, description: Option[String], projectID: Long)

class Issues(tag: Tag) extends Table[IssueDB](tag, "ISSUES") {
  // Columns
  def id = column[Option[Long]]("ISSUE_ID", O.PrimaryKey, O.AutoInc)

  def key = column[String]("ISSUE_KEY", O.Length(10))

  def name = column[String]("ISSUE_NAME", O.Length(64))

  def status = column[String]("ISSUE_STATUS", O.Length(100))

  def description = column[Option[String]]("ISSUE_DESCRIPTION", O.Length(4000))

  def updatedAt = column[String]("ISSUE_UPDATED_AT")

  def projectID = column[Long]("PROJECT_ID")
  // Select
  def * = (id, key, name, status, updatedAt, description, projectID) <> (IssueDB.tupled, IssueDB.unapply)
  def project = foreignKey("PRO_FK", projectID, projects)(_.id.get)
}


object Issues {
  val issues = TableQuery[Issues]
  val db = Database.forConfig("db")

  implicit def issue2DB(i: (com.atlassian.jira.rest.client.api.domain.Issue, UserClient)): IssueDB = {
    val project = getProject(i._1.getProject.getKey, i._2)
    IssueDB(None, i._1.getKey, i._1.getSummary, i._1.getStatus.getName, i._1.getUpdateDate.toString, Option(i._1.getDescription), project.id.get)
  }


  def getIssue(key: String, userClient: UserClient): IssueDB = {
    val issue = Await.result(db.run(issues.filter(_.key === key).result), Duration.Inf)
    if(issue.nonEmpty)
      issue.head
    else {
      insertIssue(getIssueFromJira(key, userClient), userClient)
    }
  }

  def getIssuesByUser(user: UserDB, userClient: UserClient): List[IssueDB] = {
    Await.result(db.run(userissues.filter(_.userId === user.id).result), Duration.Inf).map(x => getIssueById(x.issueId, userClient))
      .filter(x => x.isDefined)
      .map(x => x.get).filterNot(x => issueIsResolved(x)).toList
  }

  def getIssueById(id: Long, userClient: UserClient): Option[IssueDB] = {
    Await.result(db.run(issues.filter(_.id === id).result), Duration.Inf).headOption
  }



  def insertIssue(issue: IssueDB, userClient: UserClient): IssueDB = {
    Await.result(db.run(
      issues += issue
    ), Duration.Inf)
    getIssue(issue.key, userClient)
  }




}
