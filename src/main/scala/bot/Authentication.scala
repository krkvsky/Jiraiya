package bot

import java.net.URI

import akka.actor.{ActorRef, PoisonPill, Props}
import bot.Actors.Validator
import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import info.mukel.telegrambot4s.api.RequestHandler
import info.mukel.telegrambot4s.api.declarative.Action
import info.mukel.telegrambot4s.models.{Message, User}

import scala.collection.mutable

case class Issue(name: String)
case object StartMessage
case class UserClient(user: User, rest: JiraRestClient)

trait Authentication {
  val allowed: mutable.Set[(UserClient, ActorRef)] = scala.collection.mutable.Set[(UserClient, ActorRef)]()
  val URI =  new URI("http://jira.tallium.com:8085")
  def atomic[T](f: => T): T = allowed.synchronized { f }
  def login(user: User, username: String, password: String, request: RequestHandler, source: Long): Option[UserClient] = atomic {
    val restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
      URI, username, password
    )
    try {
      // here check if in database, and if not, insert into db
      val valid = JirayaBot.mySystem.actorOf(Props(new Validator(request)), "validator"+user.id+org.joda.time.DateTime.now().getMillis)
      val uc = UserClient(user, restClient)
      valid ! (source, uc)
      restClient.getSessionClient.getCurrentSession.claim()
      allowed += ((uc, valid))
      Some(uc)
    } catch {
      case _: Exception => None
    }
  }

  def logout(user: User): Unit = atomic {
    allowed.foreach {
      x => if(x._1.user.equals(user)) x._2 ! PoisonPill; allowed -= x
    }
  }

  def isAuthenticated(user: User): Option[UserClient] = atomic { allowed.find(x => x._1.user.equals(user)).map(_._1) }

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
