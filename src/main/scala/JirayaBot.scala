import com.atlassian.jira.rest.client.api.domain.Issue
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative._
import info.mukel.telegrambot4s.methods.{ParseMode, SendMessage}
import info.mukel.telegrambot4s.models.Message
import info.mukel.telegrambot4s.Implicits._

import scala.concurrent.Future

object JirayaBot extends TelegramBot with Polling with Commands  with Authentication{
  def token = "393149916:AAFf7YWkfwhUa2PlXRPXPk-anHmYvusFyco"

  onCommand("/login") { implicit msg =>
    val (username: String, password: String) = msg.text.getOrElse("").split(" ") match {
      case Array(_, y, z) => (y, z)
      case _ => ("", "")
    }
    if(username != ""){
      val repl = msg.from.map[String](u => login(u, username, password)).getOrElse("")
      reply(repl)
    } else {
      reply("Correct format is '/login username password'")
    }

  }

  onCommand("/logout") { implicit msg =>
    msg.from.foreach(logout)
    reply("You cannot access /secret anymore. Bye bye!" )
  }

  onCommand("/project") { implicit msg =>
    authenticatedOrElse{
      admin => {
        val project: String = msg.text.get.split(" ") match {//getOrElse("").split(" ") match {
          case Array(_, y) => y
          case _ => ""
        }
        if(project != ""){
          val projectIssues = admin.rest.getSearchClient().searchJql(s"project=$project and assignee=${admin.rest.getSessionClient().getCurrentSession.claim().getUsername}").claim().getIssues
          //        println(myProjects.claim().getIssues().iterator().next().toString)

          projectIssues.forEach(x => {
            val time = x.getStatus
            println(time)
            val message = s"[${x.getKey}](http://jira.tallium.com:8085/browse/${x.getKey})\n" +
              s"`${x.getDescription}`"
            reply(message, Some(ParseMode.Markdown))

          })
        } else {
          reply("Correct format is '/project PROJECTNAME'\n" +
            "Example: /project GEEKS")
        }
      }
    } { user =>
      reply(s"${user.firstName}, you must /login first.")
    }

  }

  onCommand("/secret") { implicit msg =>
    authenticatedOrElse {
      admin => {
        val myProjects = admin.rest.getSearchClient().searchJql(s"project in projectsWhereUserHasRole('Developers') and assignee=${admin.rest.getSessionClient().getCurrentSession.claim().getUsername} and updated < '-2w' order by updated desc")
//        println(myProjects.claim().getIssues().iterator().next().toString)
        val issue = myProjects.claim().getIssues().iterator().next()
        reply(issue.getKey + ":\n" + issue.getDescription)
      }
    } /* or else */ {
      user =>
        reply(s"${user.firstName}, you must /login first.")
    }
  }


//  def f: Unit = Future {
//    Thread.sleep(2000)
//    request(SendMessage(257888125L, "saske"))
//    f
//  }
}

