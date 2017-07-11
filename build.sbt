name := "Jiraya"

version := "1.0"

scalaVersion := "2.12.2"

resolvers += "releases" at "https://maven.atlassian.com/content/repositories/atlassian-public/"

libraryDependencies ++= Seq(
  "info.mukel" %% "telegrambot4s" % "3.0.0",
  "com.atlassian.jira" % "jira-rest-java-client-api" % "3.0.0",
  "com.atlassian.jira" % "jira-rest-java-client-core" % "3.0.0",
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0",
  "org.xerial" % "sqlite-jdbc" % "3.8.10.1"
)