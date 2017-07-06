name := "Jiraya"

version := "1.0"

scalaVersion := "2.12.2"

resolvers += "releases" at "https://maven.atlassian.com/content/repositories/atlassian-public/"

libraryDependencies ++= Seq(
  "info.mukel" %% "telegrambot4s" % "3.0.0",
  "com.atlassian.jira" % "jira-rest-java-client-api" % "3.0.0",
  "com.atlassian.jira" % "jira-rest-java-client-core" % "3.0.0"
)