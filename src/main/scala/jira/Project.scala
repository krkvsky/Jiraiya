package jira

import bot.UserClient
import models.ProjectDB
import models.Projects._

object Project {
  def getProjectFromJira(key: String, userClient: UserClient): ProjectDB = {
    userClient.rest.getProjectClient.getProject(key).claim()
  }

}
