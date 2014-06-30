name := "chief-play"

version := "1.0"

scalaVersion := "2.10.4"

javaHome := Some(file("/usr/lib/jvm/java-7-oracle/"))

resolvers ++= Seq(
    "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
)

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
  "org.antlr" % "ST4" % "4.0.8",
  "org.jsoup" % "jsoup" % "1.7.3",
  "com.netflix.rxjava" % "rxjava-scala" % "0.19.1",
  "org.scalesxml" %% "scales-xml" % "0.6.0-M1",
  "org.pegdown" % "pegdown" % "1.4.1" % "test",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

testOptions in Test += Tests.Argument("-h", "target/test-reports")