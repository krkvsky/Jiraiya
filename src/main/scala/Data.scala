import com.atlassian.jira.rest.client.api.domain.BasicIssue
import slick.driver.SQLiteDriver.api._
import slick.jdbc.{JdbcProfile, SQLiteProfile}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import com.atlassian.jira.rest.client.api.domain._
import collection.JavaConverters._


object Data {
  val db = Database.forConfig("db")

//  def firstLaunch = {
//     get all issues and extract projects from them to db
//
//     save all issues to db
//
//     save all userissues to db
//  }
//

  // completed
  def getIssue(key: String, userClient: UserClient): IssueDB = {
    val issue = Await.result(db.run(issues.filter(_.key === key).result), Duration.Inf)
    if(issue.nonEmpty)
      issue.head
    else {
      insertIssue(getIssueFromJira(key, userClient))
    }
  }

  implicit def issue2IssueDB(issue: com.atlassian.jira.rest.client.api.domain.Issue): IssueDB ={
    IssueDB(None, issue.getKey, issue.getSummary, issue.getDescription, 1L)
  }

  def getIssueFromJira(key: String, userClient: UserClient): IssueDB = {
    userClient.rest.getSearchClient.searchJql(s"key=$key").claim().getIssues.iterator().next()
  }

  def insertIssue(issue: IssueDB): IssueDB = {
    Await.result(db.run(
      issues += issue
    ), Duration.Inf)
    issue
  }
//
  def getIssuesByProject(projectKey: String, userClient: UserClient): List[IssueDB] = {
    val issues = userClient.rest.getSearchClient.searchJql(s"project=$projectKey").claim().getIssues.iterator().asScala.toList
    for(issue <- issues) yield issue2IssueDB(issue)
  }

  def getProject(key: String, userClient: UserClient): ProjectDB = {
    val project = Await.result(db.run(projects.filter(_.key === key).result), Duration.Inf)
    if(project.nonEmpty)
      project.head
    else
      insertProject(getProjectFromJira(key, userClient))
  }

  implicit def project2ProjectDB(project: com.atlassian.jira.rest.client.api.domain.Project): ProjectDB ={
    ProjectDB(None, project.getName, project.getKey)
  }

  def getProjectFromJira(key: String, userClient: UserClient): ProjectDB = {
    userClient.rest.getProjectClient.getProject(key).claim()
  }

  def insertProject(project: ProjectDB): ProjectDB = {
    Await.result(db.run(
      projects += project
    ), Duration.Inf)
    project
  }

  def getUser(chatid: Long, name: String): UserDB = {
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
    user
  }

//  def updateUserIssue = {
//
//  }
//     get all issues
//
//     compare with saved
//
//     if new - extract projects
//
//     save to db new projects and issues and push to chat
//
//  }

  case class UserDB(id: Option[Long], chatid: Long, firstName: String)

  case class ProjectDB(id: Option[Long], name: String, key: String)

  case class IssueDB(id: Option[Long], key: String, name: String, description: String, projectID: Long)

  case class UserIssueDB(userId: Long, issueId: Long)
//  trait UsersTable { this: DbConfiguration =>
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

  val users = TableQuery[Users]

  class Projects(tag: Tag) extends Table[ProjectDB](tag, "PROJECTS") {
    // Columns
    def id = column[Option[Long]]("PROJECT_ID", O.PrimaryKey)

    def name = column[String]("PROJECT_NAME", O.Length(64))

    def key = column[String]("PROJECT_KEY", O.Length(10))

    // Select
    def * = (id, name, key) <> (ProjectDB.tupled, ProjectDB.unapply)
  }

  val projects = TableQuery[Projects]

  class Issues(tag: Tag) extends Table[IssueDB](tag, "ISSUES") {
    // Columns
    def id = column[Option[Long]]("ISSUE_ID", O.PrimaryKey)

    def key = column[String]("ISSUE_KEY", O.Length(10))

    def name = column[String]("ISSUE_NAME", O.Length(64))

    def description = column[String]("ISSUE_DESCRIPTION", O.Length(4000))

    def projectID = column[Long]("PROJECT_ID")
    // Select
    def * = (id, key, name, description, projectID) <> (IssueDB.tupled, IssueDB.unapply)
    def project = foreignKey("PRO_FK", projectID, projects)(_.id.get)
  }

  val issues = TableQuery[Issues]

  class UserIssues(tag: Tag) extends Table[UserIssueDB](tag, "USER_ISSUES"){
    def userId = column[Long]("USER_ID")
    def issueId = column[Long]("ISSUE_ID")

    def * = (userId, issueId) <> (UserIssueDB.tupled, UserIssueDB.unapply)
    def user = foreignKey("U_FK", userId, users)(_.id.get)
    def issue = foreignKey("I_FK", issueId, issues)(_.id.get)

  }

  val userissues = TableQuery[UserIssues]

  val setup = DBIO.seq (
    (users.schema ++ projects.schema ++ issues.schema ++ userissues.schema).create
  )

}