import java.net.URI

import akka.actor.{ActorSystem, Props}
import com.atlassian.jira.rest.client.internal.async._

object Main extends App{

//  JirayaBot.run()

  Data.db.run(Data.setup)

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
//  println(restClient)
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
