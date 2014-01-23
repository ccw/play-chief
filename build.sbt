import play.Project._

name := "PlayChief"

version := "1.0"

resolvers += "Scales Repo" at "http://scala-scales.googlecode.com/svn/repo"

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.0" % "compile"

libraryDependencies += "org.antlr" % "ST4" % "4.0.7" % "compile"

libraryDependencies += "org.jsoup" % "jsoup" % "1.7.3" % "compile"

libraryDependencies += "com.netflix.rxjava" % "rxjava-scala" % "0.15.1"

libraryDependencies += "org.scalesxml" %% "scales-xml" % "0.4.5"

playScalaSettings

testOptions in Test += Tests.Argument("-h", "target/test-reports")