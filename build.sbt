import play.Project._

name := "PlayChief"

version := "1.0"

resolvers += "Scales Repo" at "http://scala-scales.googlecode.com/svn/repo"

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0" % "compile",
  "org.antlr" % "ST4" % "4.0.7" % "compile",
  "org.jsoup" % "jsoup" % "1.7.3" % "compile",
  "com.netflix.rxjava" % "rxjava-scala" % "0.15.1",
  "org.scalesxml" %% "scales-xml" % "0.6.0-M1",
  "org.pegdown" % "pegdown" % "1.4.1" % "test",
  "org.scalatest" %% "scalatest" % "2.0" % "test"
)

playScalaSettings

testOptions in Test += Tests.Argument("-h", "target/test-reports")