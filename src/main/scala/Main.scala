import java.net.URI

import com.atlassian.jira.rest.client.internal.async._

object Main extends App{
//  JirayaBot.run()
  val URI =  new URI("http://jira.tallium.com:8085")

  val restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
    URI, "YaroslavK", "last:partizan"
  )
  val projectGEEKS = restClient.getSearchClient().searchJql("project=GEEKS")

  val issues = projectGEEKS.claim().getIssues()
//  println(projectGEEKS.claim())
//  issues.forEach(x => println(x.getKey()))
  issues.forEach(x => {
    println(x.getKey)
    val issuesClient = restClient.getIssueClient().getIssue(x.getKey)
    println(issuesClient.claim().getSummary)
  }
  )
//  val issue = restClient.getIssueClient().getIssue("GEEKS-992")
//  println(issue.claim().getDescription)
}
