import java.net.URI

import Data._
import akka.actor.{ActorSystem, Props}
import com.atlassian.jira.rest.client.internal.async._
import com.atlassian.jira.rest.client.api.IssueRestClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.driver.SQLiteDriver.api._

import scala.concurrent.Await
import scala.util.{Failure, Success}

object Main extends App{
  JirayaBot.run()

//  Await.result(db.run(setup), Duration.Inf)

  //  Await.result(db.run(
//    DBIO.seq(
//      users += UserDB(None, 1, "my"),
//      projects += ProjectDB(None, "Geeksy.ml", "GEEKS"),
//      issues ++= Seq(
//        IssueDB(None, "GEEKS-992", "Let's do it", "alo", 1L),
//        IssueDB(None, "GEEKS-993", "Let's do it2", "alo", 1L)
//      ),
//      userissues += UserIssueDB(8, 1)
//    )
//  ), Duration.Inf)
//  val q1 = users.map(x => x)
//  val action = q1.result
//  val result = db.run(action)

//  println(Await.result(result, Duration.Inf))

//  println(result)

//  if (result.isCompleted) {
//    result.foreach(x => println(x))
//    val last = result.result(10 seconds)
//    println(last)
//  }

//  val system = ActorSystem("PingPongSystem")
//  val pong = system.actorOf(Props[Pong], name = "pong")
//  val ping = system.actorOf(Props(new Ping(pong)), name = "ping")
//
//  ping ! StartMessage

//  val URI =  new URI("http://jira.tallium.com:8085")

//  val restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
//    URI, "YaroslavK", "last:partizan"
//  )
//  try {
//    restClient.getSessionClient().getCurrentSession().claim()
//    println("A")
//  } catch {
//    case e: Exception => println("login failed")
//  }
//  new AsynchronousJiraRestClientFactory
//  val issue = restClient.getSearchClient().searchJql("key=desn-4918").claim().getIssues.iterator().next()
//  val iss = IssueDB(None, issue.getKey, issue.getSummary, issue.getDescription, 1L)
//  println(iss)
//  val restAuthClient = new AsynchronousJiraRestClientFactory().
//  val me = restClient.getUserClient().getUser("yaroslavk")
//  val projectGEEKS = restClient.getSearchClient().searchJql("project=GEEKS AND assignee=yaroslavk AND status = done")
//val myProjects = restClient.getSearchClient().searchJql("project in projectsWhereUserHasRole('Developers') and assignee=yaroslavk and updated < '-2w' order by updated desc")
//  println(myProjects.claim().getIssues().iterator().next())
//  restClient.getComponentClient()
//  issues.forEach(x => {
//    println(x.getKey)
//    println(x.getSummary)
//  }
//  println(me.claim())
//  val issue = restClient.getIssueClient().getIssue("GEEKS-992")
//  println(issue.claim().getDescription)
}
