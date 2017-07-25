import java.net.URI

import akka.actor.{ActorSystem, Props}
import bot.JirayaBot
import com.atlassian.jira.rest.client.internal.async._
import com.atlassian.jira.rest.client.api.IssueRestClient
import com.atlassian.jira.rest.client.api.domain.input.WorklogInput
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.driver.SQLiteDriver.api._

import scala.concurrent.Await
import scala.util.{Failure, Success}
import collection.JavaConverters._

object Main extends App{
//  Await.result(db.run(setup), Duration.Inf)
  JirayaBot.run()
//  val ur =  new URI("http://jira.tallium.com:8085")

//  val restClient = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
//    ur, "YaroslavK", "last:partizan"
//  )
//val setup = DBIO.seq (
//  (users.schema ++ projects.schema ++ issues.schema ++ userissues.schema ++ issueworking.schema).create
//)


//  val setup = DBIO.seq (
//    (users.schema ++ projects.schema ++ issues.schema ++ userissues.schema ++ issueworking.schema).create
//  )
}
