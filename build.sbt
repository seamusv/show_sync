name := "show_sync"

version := "0.1"

scalaVersion := "2.12.9"

scalacOptions := Seq(
  "-feature",
  "-deprecation",
  "-explaintypes",
  "-unchecked",
  "-Xfuture",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:existentials",
  "-Ypartial-unification",
  "-Xfatal-warnings",
  "-Xlint:-infer-any,_",
  "-Ywarn-value-discard",
  "-Ywarn-numeric-widen",
  "-Ywarn-extra-implicit",
  "-Ywarn-unused:_",
  "-Ywarn-inaccessible",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-opt:l:inline"
)

libraryDependencies ++= {
  val circeVersion = "0.12.1"
  val http4sVersion = "0.20.11"
  val zioVersion = "1.0.0-RC14"

  Seq(
    "dev.zio" %% "zio" % zioVersion,
    "dev.zio" %% "zio-streams" % zioVersion,
    "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC5",

    "org.http4s" %% "http4s-circe" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,

    "commons-net" % "commons-net" % "3.6",
    "com.hierynomus" % "sshj" % "0.27.0",

    "com.github.pureconfig" %% "pureconfig" % "0.12.1",

    "com.github.fracpete" % "rsync4j-all" % "3.1.2-16",

    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-generic-extras" % circeVersion,
    "io.circe" %% "circe-jawn" % circeVersion,
    "io.circe" %% "circe-numbers" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,

    "com.nequissimus" %% "zio-slf4j" % "0.3.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  )
}

enablePlugins(JavaAppPackaging)