ThisBuild / scalaVersion := "3.3.1"
ThisBuild / resolvers   ++= Seq(
  // needed because we're using a SNAPSHOT version of zio-redis
  "Sonatype Snapshots"   at "https://oss.sonatype.org/content/repositories/snapshots/"
)

val backend = project.settings(
  fork := true,
  name := "redis-benchmark",

  libraryDependencies ++= Seq(
    "dev.zio" %% "zio-test" % "2.0.17" % Test,
    "dev.zio" %% "zio-test-sbt" % "2.0.17" % Test,
    "dev.zio" %% "zio-test-magnolia" % "2.0.17" % Test,
    "redis.clients" % "jedis" % "5.1.0",
    "com.github.sideeffffect" %% "zio-testcontainers" % "0.4.1" % Test,
    "com.redis.testcontainers" % "testcontainers-redis" % "1.6.4" % Test,
    // We need a SNAPSHOT version of zio-redis because the current stable release doesn't
    // support the AsyncRedis API
    "dev.zio" %% "zio-redis" % "0.2.0+69-4eabf601-SNAPSHOT" % Test,
  ),
  scalacOptions ++= Seq(
    "-Ykind-projector:underscores",
    "-deprecation",
    "-feature",
    "-indent",
    "-new-syntax",
    "-source:future", // allows destructuring in for comprehensions
    "-unchecked",
    "-Wunused:all",
    "-Wvalue-discard",
    "-Wnonunit-statement"
  ),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
)