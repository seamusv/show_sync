name := "show_sync"

version := "0.1"

scalaVersion := "2.12.10"

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
  val catsVersion = "2.0.0"
  val circeVersion = "0.12.2"
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

    "org.typelevel" %% "cats-core" % catsVersion,
    "org.typelevel" %% "cats-effect" % catsVersion,
    "org.typelevel" %% "cats-kernel" % catsVersion,
    "org.typelevel" %% "cats-macros" % catsVersion,

    "com.nequissimus" %% "zio-slf4j" % "0.3.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "dev.zio" %% "zio-test" % zioVersion % "test",
    "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
  )
}

enablePlugins(JavaAppPackaging)

addCommandAlias("dist", "; clean; compile; test; universal:packageBin")

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
