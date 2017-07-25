package models

import slick.driver.SQLiteDriver.api._
import bot.UserClient
import jira.Project._
import models.Issues._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class ProjectDB(id: Option[Long], name: String, key: String)

class Projects(tag: Tag) extends Table[ProjectDB](tag, "PROJECTS") {
  // Columns
  def id = column[Option[Long]]("PROJECT_ID", O.PrimaryKey, O.AutoInc)

  def name = column[String]("PROJECT_NAME", O.Length(64))

  def key = column[String]("PROJECT_KEY", O.Length(10))

  // Select
  def * = (id, name, key) <> (ProjectDB.tupled, ProjectDB.unapply)
}


object Projects {
  val projects = TableQuery[Projects]
  val db = Database.forConfig("db")

  implicit def project2ProjectDB(project: com.atlassian.jira.rest.client.api.domain.Project): ProjectDB ={
    ProjectDB(None, project.getName, project.getKey)
  }


  def getProjectsByUser(user: UserDB, userClient: UserClient): List[ProjectDB] = {
    getIssuesByUser(user, userClient).map(x => getProjectById(x.projectID, userClient)).distinct
  }

  def insertProject(project: ProjectDB, userClient: UserClient): ProjectDB = {
    val x = Await.result(db.run(
      projects += project
    ), Duration.Inf)
    getProject(project.key, userClient)
  }

  def getProject(key: String, userClient: UserClient): ProjectDB = {
    val project = Await.result(db.run(projects.filter(_.key === key).result), Duration.Inf)
    if(project.nonEmpty)
      project.head
    else
      insertProject(getProjectFromJira(key, userClient), userClient)
  }

  def getProjectById(id: Long, userClient: UserClient): ProjectDB = {
    Await.result(db.run(projects.filter(_.id === id).result), Duration.Inf).head
  }

  def getProjectByKey(key: String): ProjectDB = {
    Await.result(db.run(projects.filter(_.key === key).result), Duration.Inf).head
  }

}
