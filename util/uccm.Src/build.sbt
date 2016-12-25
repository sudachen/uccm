
import com.github.retronym.SbtOneJar._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(Seq(
      version := "1.0.0",
      organization := "com.github.uccm",
      scalaVersion := "2.12.1",
      licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))
    )),
    name := "uccm",
    oneJarSettings,
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0",
    libraryDependencies += "commons-lang" % "commons-lang" % "2.6"
  )
