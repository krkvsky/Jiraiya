package jira

import bot.UserClient
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput
import models._
import models.Issues._

import collection.JavaConverters._


/**
  * Created by yarik on 25.07.17.
  */
object Issue {
  def getIssueFromJira(key: String, userClient: UserClient): IssueDB = {
    var set = new java.util.HashSet[String]()
    set.add("*all")
    (userClient.rest.getSearchClient.searchJql(s"key=$key", Int.MaxValue, 0, set).claim().getIssues.iterator().next(), userClient)
  }



  def moveIssueToTesting(key: String, userClient: UserClient) = {
    var set = new java.util.HashSet[String]()
    set.add("*all")

    val iss = userClient.rest.getSearchClient.searchJql(s"key=OTHR-372", Int.MaxValue, 0, set).claim().getIssues.iterator().next()
    val transitions = userClient.rest.getIssueClient.getTransitions(iss.getTransitionsUri)
    val tranId = transitions.claim().iterator().asScala.toList.filter(x =>
      x.getName.toLowerCase == "in review"
        ||
        x.getName.toLowerCase == "in testing"
    ).toList.head.getId
    userClient.rest.getIssueClient.transition(iss, new TransitionInput(tranId))
  }


  def getOriginalIssueFromJira(key: String, userClient: UserClient) = {
    var set = new java.util.HashSet[String]()
    set.add("*all")
    userClient.rest.getSearchClient.searchJql(s"key=$key", Int.MaxValue, 0, set).claim().getIssues.iterator().next()

  }

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

  def updateIssueStatus(key: String, userClient: UserClient) = {

  }

}