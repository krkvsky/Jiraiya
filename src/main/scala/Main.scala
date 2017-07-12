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
//  Await.result(db.run(setup), Duration.Inf)
  JirayaBot.run()
//  val URI =  new URI("http://jira.tallium.com:8085")

//  val restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
//    URI, "YaroslavK", "last:partizan"
//  )
//  val user = new info.mukel.telegrambot4s.models.User(1, "a")
//  firstLaunch(257888125L, "yaroslavk", UserClient(user, restClient))
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
//  val projectGEEKS = restClient.getSearchClient().searchJql("assignee=yaroslavk", 1, 1, Set("nice", "nice"))
//  var c = 0
//  projectGEEKS.claim().getIssues.forEach(x => c += 1)
//  println(c)
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
