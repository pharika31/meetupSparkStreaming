name := "meetup"

version := "0.1"

scalaVersion := "2.10.4"

val sparkVersion = "1.3.0"

externalResolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

libraryDependencies ++= Seq(
  ("org.apache.spark" %% "spark-core" % sparkVersion % "compile").
    exclude("org.apache.spark", "spark-network-common_2.10").
    exclude("org.apache.spark", "spark-network-shuffle_2.10"),
  // avoid an ivy bug
  "org.apache.spark" %% "spark-network-common" % sparkVersion % "compile",
  "org.apache.spark" %% "spark-network-shuffle" % sparkVersion % "compile",
  ("org.apache.spark" %% "spark-streaming" % sparkVersion % "compile").
    exclude("org.apache.spark", "spark-core_2.10"),
  ("org.apache.spark" %% "spark-streaming-kafka" % sparkVersion).
    exclude("org.apache.spark", "spark-core_2.10"),
  ("org.scalikejdbc" %% "scalikejdbc" % "2.2.1").
    exclude("org.slf4j", "slf4j-api"),
  ("org.postgresql" % "postgresql" % "9.3-1101-jdbc4").
    exclude("org.slf4j", "slf4j-api"),
  "com.typesafe" % "config" % "1.2.1",
  "org.twitter4j" % "twitter4j-core" % "4.0.0",
  "org.twitter4j" % "twitter4j-stream" % "4.0.0",
  "org.apache.spark" % "spark-sql_2.10" % sparkVersion,
  "org.scala-lang" % "scala-reflect" % "1.3.0", // scala version
  "org.json4s" %% "json4s-native" % "3.2.10",
  "org.json4s" %% "json4s-ext" % "3.2.10",
  "com.datastax.spark" %% "spark-cassandra-connector" % "1.3.0-M1",
  "com.lambdaworks" % "jacks_2.10" % "2.2.3",
  "com.propensive" % "rapture-io" % "0.7.2",
  "net.liftweb" % "lift-json_2.10" % "2.5-M4"

)