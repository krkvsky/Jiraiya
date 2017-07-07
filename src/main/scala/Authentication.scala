import java.net.URI

import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import info.mukel.telegrambot4s.models.{Message, User}
import info.mukel.telegrambot4s.api.declarative._

case class UserClient(user: User, rest: JiraRestClient)

trait Authentication {
  val allowed = scala.collection.mutable.Set[UserClient]()
  val URI =  new URI("http://jira.tallium.com:8085")
  def atomic[T](f: => T): T = allowed.synchronized { f }
  def login(user: User, username: String, password: String): String = atomic {
    val restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
      URI, username, password
    )
    try {
      restClient.getSessionClient().getCurrentSession().claim()
      allowed += UserClient(user, restClient)
      "login successful"
    } catch {
      case e: Exception => "login failed"
    }
  }
  def logout(user: User) = atomic {
    allowed.foreach(x => if(x.user.equals(user)) allowed -= x)
  }
  def isAuthenticated(user: User): Option[UserClient] = atomic { allowed.find(x => x.user.equals(user)) }

  def authenticatedOrElse(ok: Action[UserClient])(noAccess: Action[User])(implicit msg: Message): Unit = {
    msg.from.foreach {
      user =>
        val userClient = isAuthenticated(user)
        if (userClient.isDefined)
          ok(userClient.get)
        else
          noAccess(user)
    }
  }

  def authenticated(ok: Action[UserClient])(implicit msg: Message): Unit = {
    msg.from.foreach {
      user =>
        val userClient = isAuthenticated(user)
        if (userClient.isDefined)
          ok(userClient.get)
    }
  }
}