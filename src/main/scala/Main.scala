import java.net.URI
import java.util

import akka.actor.{ActorSystem, Props}
import bot.JirayaBot
import com.atlassian.jira.rest.client.internal.async._
import com.atlassian.jira.rest.client.api.IssueRestClient
import com.atlassian.jira.rest.client.api.domain.input.{FieldInput, IssueInput, TransitionInput, WorklogInput}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.driver.SQLiteDriver.api._

import scala.concurrent.Await
import scala.util.{Failure, Success}
import collection.JavaConverters._
import scala.collection.JavaConverters
import models.Issues._
import models.Users._
import models.Projects._
import models.Issues._
import models.IssueWorking._
import models.UserIssues._

object Main extends App{
//  val db = Database.forConfig("db")
//  def setup = DBIO.seq (
//    (users.schema ++ projects.schema ++ issues.schema ++ issueworking.schema ++ userissues.schema).create
//  )
//  Await.result(db.run(setup), Duration.Inf)

  JirayaBot.run()
//  val ur =  new URI("http://jira.tallium.com:8085")

//  val restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
//    ur, "YaroslavK", "last:partizan"
//  )
//  var set = new java.util.HashSet[String]()
//  set.add("*all")
//
//  val temp = restClient.getSearchClient.searchJql(s"assignee=yaroslavk AND (status = Open OR status = Reopened OR status = 'In Progress') order by created", 100, 0, set).claim().getIssues.iterator().asScala.toList
//    .map(x => x.getStatus)
//  println(temp)
//  println(temp.length)

  //  println(temp.head.getField("progress").getValue.toString.drop(12).take(5))
//
//  val dt = new org.joda.time.DateTime()
//  println("%s/%s/%s".format(dt.year().get(), dt.monthOfYear().getAsString, dt.dayOfMonth().get()))
//  var set = new java.util.HashSet[String]()
//  set.add("*all")
//
//  val iss = restClient.getSearchClient.searchJql(s"key=WSH-59", Int.MaxValue, 0, set).claim().getIssues.iterator().next()
//  val transitions = restClient.getIssueClient.getTransitions(iss.getTransitionsUri)
//  println(transitions.claim().iterator().asScala.toList)
//  val tranId = transitions.claim().iterator().asScala.toList.filter(x =>
//    x.getName.toLowerCase == "in review"
//      ||
//    x.getName.toLowerCase == "in testing"
//  ).toList.head.getId
//  restClient.getIssueClient.transition(iss, new TransitionInput(tranId))

//  val fields = iss.getFields().iterator().asScala.map(x => new FieldInput(x.getId, x.getValue))
//  val x = IssueInput.createWithFields(fields.next())
//  restClient.getIssueClient.transition()
//  println(iss.getTransitionsUri)
//val setup = DBIO.seq (
//  (users.schema ++ projects.schema ++ issues.schema ++ userissues.schema ++ issueworking.schema).create
//)


//  val setup = DBIO.seq (
//    (users.schema ++ projects.schema ++ issues.schema ++ userissues.schema ++ issueworking.schema).create
//  )
}
