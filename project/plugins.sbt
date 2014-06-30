logLevel := Level.Warn

conflictWarning := ConflictWarning.disable

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")