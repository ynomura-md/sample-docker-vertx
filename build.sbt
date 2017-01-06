val vertxVersion = "3.3.3"
lazy val root = (project in file("."))
    .enablePlugins(sbtdocker.DockerPlugin)
    .enablePlugins(com.typesafe.sbt.packager.archetypes.JavaAppPackaging)
    .settings(
      organization := "jp.supership",
      name := "y-nomura",
      version := "1.0.0-SNAPSHOT",
      scalaVersion := "2.11.8",
      mainClass in (Compile, run) := Some("io.vertx.core.Starter"),
      libraryDependencies ++= Seq(
        "io.vertx" % "vertx-web" % vertxVersion,
        "io.vertx" % "vertx-hazelcast" % vertxVersion,
        "io.vertx" % "vertx-redis-client" % vertxVersion,
        "io.vertx" % "vertx-web-templ-handlebars" % vertxVersion,
        "io.vertx" % "vertx-codegen" % vertxVersion % "provided" changing()),
      mainClass in assembly := Some("io.vertx.core.Starter"),
      mappings in Universal := {
        val universalMappings = (mappings in Universal).value
        val fatJar = (assembly in Compile).value
        val filtered = universalMappings filter {
          case (file, name) if (!name.endsWith(".jar")) => true
          case (file, name) if (name.contains("bcpkix-jdk15on-") || name.contains("bcprov-jdk15on-")) => true
          case _ => false
        }
        filtered :+ (fatJar -> ("lib/" + fatJar.getName))
      },
      mainClass in assembly := Some("io.vertx.core.Starter"),
      assemblyExcludedJars in assembly := {
        val cp = (fullClasspath in assembly).value
        cp filter { cp =>
          cp.data.getName.contains("asciidoctorj") ||
          cp.data.getName.contains("jruby-complete") ||
          cp.data.getName.contains("bcpkix-jdk15on-") ||
          cp.data.getName.contains("bcprov-jdk15on-")
        }
      },
      assemblyMergeStrategy in assembly := {
        // // case PathList("org", "datanucleus", xs @ _*)             => MergeStrategy.discard
        case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
        case m if m.toLowerCase.matches("meta-inf.*\\.sf")      => MergeStrategy.discard
        case m if m.toLowerCase.matches("meta-inf.*\\.rsa")      => MergeStrategy.discard
        case m if m.toLowerCase.matches("meta-inf.*\\.dsa")      => MergeStrategy.discard
        case m if m.toLowerCase.endsWith("index.list")      => MergeStrategy.discard
        case m if m == "META-INF/services/io.vertx.core.spi.launcher.CommandFactory" => MergeStrategy.concat
        // case "log4j.properties"                                  => MergeStrategy.discard
        case m if m.toLowerCase.startsWith("meta-inf/io.netty.versions.properties") => MergeStrategy.last
        // case "reference.conf"                                    => MergeStrategy.concat
        case _                                                   => MergeStrategy.first
      },
      assemblyJarName in assembly := "y-nomura.jar",

      dockerfile in docker := {
        new Dockerfile {
          from("java:8-jre-alpine")
          env("VERTICLE_FILE" -> "y-nomura.jar", 
          "VERTICLE_HOME" -> "/usr/vericles")
          copy(assembly.value, "$VERTICLE_HOME/")
          workDir("$VERTICLE_HOME")
          entryPoint("sh", "-c")
          cmd("java -jar $VERTICLE_FILE run ava.HelloVerticle")
        }
      })
