import slick.basic.DatabaseConfig
import slick.jdbc.H2Profile.api._
import slick.jdbc.{H2Profile, JdbcProfile}

import scala.concurrent.ExecutionContext.Implicits.global
object Data {
  val db = Database.forConfig("db")

  case class UserDB(id: Long, chatid: Option[Long], firstName: Option[String])

  case class ProjectDB(id: Long, name: String, key: String)

  case class IssueDB(id: Long, key: Long, name: String, description: String, projectID: Long)

  //trait UsersTable { this: DbConfiguration =>
  class Users(tag: Tag) extends Table[UserDB](tag, "USERS") {
    // Columns
    def id = column[Long]("USER_ID", O.PrimaryKey, O.AutoInc)

    def chatid = column[Option[Long]]("USER_CHAT_ID", O.Length(10))

    def firstName = column[Option[String]]("USER_FIRST_NAME", O.Length(64))

    // Indexes
    def chatidIndex = index("USER_EMAIL_IDX", chatid, true)

    // Select
    def * = (id, chatid, firstName) <> (UserDB.tupled, UserDB.unapply)
  }

  val users = TableQuery[Users]

  class Projects(tag: Tag) extends Table[ProjectDB](tag, "PROJECTS") {
    // Columns
    def id = column[Long]("PROJECT_ID", O.PrimaryKey)

    def name = column[String]("PROJECT_NAME", O.Length(64))

    def key = column[String]("PROJECT_KEY", O.Length(10))

    // Select
    def * = (id, name, key) <> (ProjectDB.tupled, ProjectDB.unapply)
  }

  val projects = TableQuery[Projects]

  class Issues(tag: Tag) extends Table[IssueDB](tag, "ISSUES") {
    // Columns
    def id = column[Long]("ISSUE_ID", O.PrimaryKey)

    def chatid = column[Long]("ISSUE_KEY", O.Length(10))

    def name = column[String]("ISSUE_NAME", O.Length(64))

    def description = column[String]("ISSUE_DESCRIPTION", O.Length(4000))

    def projectID = column[Long]("PROJECT_ID")
    // Select
    def * = (id, chatid, name, description, projectID) <> (IssueDB.tupled, IssueDB.unapply)
    def project = foreignKey("PRO_FK", projectID, projects)(_.id)
  }

  val issues = TableQuery[Issues]

  def setup = DBIO.seq (
    (users.schema ++ projects.schema ++ issues.schema).create
  )

}