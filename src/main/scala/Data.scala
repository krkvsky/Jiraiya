import java.util

import com.atlassian.jira.rest.client.api.domain.BasicIssue
import slick.driver.SQLiteDriver.api._
import slick.jdbc.{JdbcProfile, SQLiteProfile}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import com.atlassian.jira.rest.client.api.domain._
import com.atlassian.jira.rest.client.api.domain.input.WorklogInput
import org.joda.time.DateTime

import collection.JavaConverters._


object Data {
  val db = Database.forConfig("db")

  def firstLaunch(chatid: Long, name: String, userClient: UserClient): UserDB = {
    val user = getUser(chatid, name)
    updateUserIssues(user, userClient)
    user
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

      val issue = getOriginalIssueFromJira(getIssueById(curr.issueId, userClient).get.key, userClient)
      val worklogURI = issue.getWorklogUri
      val worklog = WorklogInput.create(issue.getSelf, null, new DateTime(previous), Util.millisToMinutes(workToLog), null)
      userClient.rest.getIssueClient.addWorklog(worklogURI, worklog)
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
      println("Start finish")
      val workToLog = System.currentTimeMillis() - previous
      val issue = getOriginalIssueFromJira(getIssueById(curr.issueId, userClient).get.key, userClient)
      val worklogURI = issue.getWorklogUri
      val worklog = WorklogInput.create(issue.getSelf, null, new DateTime(previous), Util.millisToMinutes(workToLog), null)
      println("End finish")
      userClient.rest.getIssueClient.addWorklog(worklogURI, worklog)
      val newTime = curr.time + workToLog
      Await.result(
        db.run(
          issueworking.filter(x => x.issueId === curr.issueId && x.userId === curr.userId).map(x => (x.time, x.finished)).update(newTime, true)
        ), Duration.Inf
      )
      newTime
    }else
      0
  }



  def getIssueById(id: Long, userClient: UserClient): Option[IssueDB] = {
    Await.result(db.run(issues.filter(_.id === id).result), Duration.Inf).headOption
  }

  implicit def issue2DB(i: (com.atlassian.jira.rest.client.api.domain.Issue, UserClient)): IssueDB = {
    val project = getProject(i._1.getProject.getKey, i._2)
    IssueDB(None, i._1.getKey, i._1.getSummary, i._1.getStatus.getName, i._1.getUpdateDate.toString, Option(i._1.getDescription), project.id.get)
  }

  def getIssueFromJira(key: String, userClient: UserClient): IssueDB = {
    var set = new java.util.HashSet[String]()
    set.add("*all")
    (userClient.rest.getSearchClient.searchJql(s"key=$key", Int.MaxValue, 0, set).claim().getIssues.iterator().next(),userClient)
  }

  def getOriginalIssueFromJira(key: String, userClient: UserClient) = {
    var set = new java.util.HashSet[String]()
    set.add("*all")
    userClient.rest.getSearchClient.searchJql(s"key=$key", Int.MaxValue, 0, set).claim().getIssues.iterator().next()
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

    var set = new java.util.HashSet[String]()
    set.add("*all")

    userClient.rest.getSearchClient.searchJql(s"project=$projectKey order by created", 100, 0, set).claim().getIssues.iterator().asScala.toList.map(x => getIssue(x.getKey, userClient))
  }

  def getIssuesByUserJira(user: UserDB, userClient: UserClient, filters: String = ""): List[IssueDB] = {
//    userClient.rest.getSearchClient.searchJql(s"assignee=${user.firstName} $filters").claim().getIssues.iterator().asScala.toList.map(x => issue2DB(x, userClient))

    var set = new java.util.HashSet[String]()
    set.add("*all")

    userClient.rest.getSearchClient.searchJql(s"assignee=${user.firstName} order by created", Int.MaxValue, 0, set).claim().getIssues.iterator().asScala.toList.map(x => getIssue(x.getKey, userClient))
  }

  def getIssuesByUserJiraFirst(user: UserDB, userClient: UserClient, filters: String = ""): List[IssueDB] = {
    //    userClient.rest.getSearchClient.searchJql(s"assignee=${user.firstName} $filters").claim().getIssues.iterator().asScala.toList.map(x => issue2DB(x, userClient))

    var set = new java.util.HashSet[String]()
    set.add("*all")

    userClient.rest.getSearchClient.searchJql(s"assignee=${user.firstName} order by created", 100, 0, set).claim().getIssues.iterator().asScala.toList.map(x => getIssue(x.getKey, userClient))
  }

  def getIssuesByUsername(username: String, userClient: UserClient, filters: String = ""): List[IssueDB] = {
    var set = new java.util.HashSet[String]()
    set.add("*all")

    userClient.rest.getSearchClient.searchJql(s"assignee=$username order by created", 100, 0, set).claim().getIssues.iterator().asScala.toList.map(x => getIssue(x.getKey, userClient))
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


  case class UserDB(id: Option[Long], chatid: Long, firstName: String)

  case class ProjectDB(id: Option[Long], name: String, key: String)

  case class IssueDB(id: Option[Long], key: String, name: String, status: String, updatedAt: String, description: Option[String], projectID: Long)

  case class IssueWorkingDB(userId: Long,  issueId: Long, startedAt: Long, continuedAt:Long, time: Long, paused: Boolean, finished: Boolean)

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

    def status = column[String]("ISSUE_STATUS", O.Length(100))

    def description = column[Option[String]]("ISSUE_DESCRIPTION", O.Length(4000))

    def updatedAt = column[String]("ISSUE_UPDATED_AT")

    def projectID = column[Long]("PROJECT_ID")
    // Select
    def * = (id, key, name, status, updatedAt, description, projectID) <> (IssueDB.tupled, IssueDB.unapply)
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

  val issueworking= TableQuery[IssueWorking]


  val setup = DBIO.seq (
    (users.schema ++ projects.schema ++ issues.schema ++ userissues.schema ++ issueworking.schema).create
  )

}