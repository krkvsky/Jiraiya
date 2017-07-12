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

  def firstLaunch(chatid: Long, name: String, userClient: UserClient): Unit = {
    val user = getUser(chatid, name)
    updateUserIssues(user, userClient)
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
      .map(x => x.get).toList
  }

  def getIssueById(id: Long, userClient: UserClient): Option[IssueDB] = {
    Await.result(db.run(issues.filter(_.id === id).result), Duration.Inf).headOption
  }

  implicit def issue2DB(i: (com.atlassian.jira.rest.client.api.domain.Issue, UserClient)): IssueDB = {
    val project = getProject(i._1.getProject.getKey, i._2)
    IssueDB(None, i._1.getKey, i._1.getSummary, Option(i._1.getDescription), project.id.get)
  }

  def getIssueFromJira(key: String, userClient: UserClient): IssueDB = {
    (userClient.rest.getSearchClient.searchJql(s"key=$key, 'maxResults': 1").claim().getIssues.iterator().next(),userClient)
  }

  def insertIssue(issue: IssueDB, userClient: UserClient): IssueDB = {
    Await.result(db.run(
      issues += issue
    ), Duration.Inf)
    getIssue(issue.key, userClient)
  }
//
  def getIssuesByProject(projectKey: String, userClient: UserClient): List[IssueDB] = {
//    userClient.rest.getSearchClient.searchJql(s"project=$projectKey").claim().getIssues.iterator().asScala.toList.map(x => issue2DB(x, userClient))
    userClient.rest.getSearchClient.searchJql(s"project=$projectKey, 'maxResults': 1").claim().getIssues.iterator().asScala.toList.map(x => getIssue(x.getKey, userClient))
  }

  def getIssuesByUserJira(user: UserDB, userClient: UserClient, filters: String = ""): List[IssueDB] = {
//    userClient.rest.getSearchClient.searchJql(s"assignee=${user.firstName} $filters").claim().getIssues.iterator().asScala.toList.map(x => issue2DB(x, userClient))
    userClient.rest.getSearchClient.searchJql(s"assignee=${user.firstName}, 'maxResults': 1 $filters").claim().getIssues.iterator().asScala.toList.map(x => getIssue(x.getKey, userClient))
  }

  def getIssuesByUsername(username: String, userClient: UserClient, filters: String = ""): List[IssueDB] = {
    userClient.rest.getSearchClient.searchJql(s"assignee=$username, 'maxResults': 1 $filters").claim().getIssues.iterator().asScala.toList.map(x => getIssue(x.getKey, userClient))
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

  implicit def project2ProjectDB(project: com.atlassian.jira.rest.client.api.domain.Project): ProjectDB ={
    ProjectDB(None, project.getName, project.getKey)
  }

  def getProjectFromJira(key: String, userClient: UserClient): ProjectDB = {
    userClient.rest.getProjectClient.getProject(key).claim()
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

  def getUserIssue(userId: Long, issueId: Long): UserIssueDB = {
    val userIssue = Await.result(db.run(userissues.filter(x => x.userId === userId && x.issueId === issueId).result), Duration.Inf)
    if(userIssue.nonEmpty)
      userIssue.head
    else
      insertUserIssue(UserIssueDB(userId, issueId))
  }


  def getNewUserIssue(userId: Long, issueId: Long): Option[UserIssueDB] = {
    val userIssue = Await.result(db.run(userissues.filter(x => x.userId === userId && x.issueId === issueId).result), Duration.Inf)
    if(userIssue.nonEmpty)
      None
    else
      Some(insertUserIssue(UserIssueDB(userId, issueId)))
  }

  def insertUserIssue(userIssue: UserIssueDB): UserIssueDB = {
    Await.result(db.run(
      userissues += userIssue
    ), Duration.Inf)
    getUserIssue(userIssue.userId, userIssue.issueId)
  }

  def updateUserIssues(user: UserDB, userClient: UserClient): List[IssueDB] = {
//    val userIssues = getIssuesByUser(user, userClient)
    val userIssues = getIssuesByUserJira(user, userClient)
    val unfiltered = for(issue <- userIssues) yield getNewUserIssue(user.id.get, issue.id.get)
    unfiltered.filter(_.isDefined).map(j => getIssueById(j.get.issueId, userClient)).filter(j => j.isDefined).map(_.get)
  }

  case class UserDB(id: Option[Long], chatid: Long, firstName: String)

  case class ProjectDB(id: Option[Long], name: String, key: String)

  case class IssueDB(id: Option[Long], key: String, name: String, description: Option[String], projectID: Long)

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
    def id = column[Option[Long]]("PROJECT_ID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("PROJECT_NAME", O.Length(64))

    def key = column[String]("PROJECT_KEY", O.Length(10))

    // Select
    def * = (id, name, key) <> (ProjectDB.tupled, ProjectDB.unapply)
  }

  val projects = TableQuery[Projects]

  class Issues(tag: Tag) extends Table[IssueDB](tag, "ISSUES") {
    // Columns
    def id = column[Option[Long]]("ISSUE_ID", O.PrimaryKey, O.AutoInc)

    def key = column[String]("ISSUE_KEY", O.Length(10))

    def name = column[String]("ISSUE_NAME", O.Length(64))

    def description = column[Option[String]]("ISSUE_DESCRIPTION", O.Length(4000))

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