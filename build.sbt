import AppDependencies._
import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline
import org.scalastyle.sbt.ScalastylePlugin._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.PublishingSettings._
import uk.gov.hmrc.gitstamp.GitStampPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

import scala.language.postfixOps

name := "customs-declarations"
scalaVersion := "2.12.10"
targetJvm := "jvm-1.8"

lazy val allResolvers = resolvers ++= Seq(
  Resolver.bintrayRepo("hmrc", "releases"),
  Resolver.jcenterRepo
)

lazy val ComponentTest = config("component") extend Test
lazy val CdsIntegrationTest = config("it") extend Test

val testConfig = Seq(ComponentTest, CdsIntegrationTest, Test)

def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

lazy val testAll = TaskKey[Unit]("test-all")
lazy val allTest = Seq(testAll := (test in ComponentTest)
  .dependsOn((test in CdsIntegrationTest).dependsOn(test in Test)).value)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .enablePlugins(SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .enablePlugins(SbtArtifactory)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    unitTestSettings,
    integrationTestSettings,
    componentTestSettings,
    playPublishingSettings,
    allTest,
    scoverageSettings,
    allResolvers
  )
  .settings(majorVersion := 0)

def onPackageName(rootPackage: String): String => Boolean = {
  testName => testName startsWith rootPackage
}

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      testOptions in Test := Seq(Tests.Filter(onPackageName("unit"))),
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      unmanagedSourceDirectories in Test := Seq((baseDirectory in Test).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val integrationTestSettings =
  inConfig(CdsIntegrationTest)(Defaults.testTasks) ++
    Seq(
      testOptions in CdsIntegrationTest := Seq(Tests.Filters(Seq(onPackageName("integration"), onPackageName("component")))),
      testOptions in CdsIntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in CdsIntegrationTest := false,
      parallelExecution in CdsIntegrationTest := false,
      addTestReportOption(CdsIntegrationTest, "int-test-reports"),
      testGrouping in CdsIntegrationTest := forkedJvmPerTestConfig((definedTests in Test).value, "integration", "component")
    )

lazy val componentTestSettings =
  inConfig(ComponentTest)(Defaults.testTasks) ++
    Seq(
      testOptions in ComponentTest := Seq(Tests.Filter(onPackageName("component"))),
      testOptions in ComponentTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in ComponentTest := false,
      parallelExecution in ComponentTest := false,
      addTestReportOption(ComponentTest, "component-reports")
    )

lazy val commonSettings: Seq[Setting[_]] = scalaSettings ++
  SbtDistributablesPlugin.publishingSettings ++
  defaultSettings() ++
  gitStampSettings

lazy val playPublishingSettings: Seq[sbt.Setting[_]] = Seq(credentials += SbtCredentials) ++
  publishAllArtefacts

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
      "<empty>"
      ,"uk\\.gov\\.hmrc\\.customs\\.declaration\\.model\\..*"
      ,"uk\\.gov\\.hmrc\\.customs\\.declaration\\.views\\..*"
      ,".*(Reverse|AuthService|BuildInfo|Routes).*"
    ).mkString(";"),
  coverageMinimum := 96,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

val compileDependencies = Seq(customsApiCommon, circuitBreaker, simpleReactiveMongo, playJsonJoda)

val testDependencies = Seq(hmrcTest, scalaTestPlusPlay, wireMock, mockito, customsApiCommonTests, reactiveMongoTest)

unmanagedResourceDirectories in Compile += baseDirectory.value / "public"
(managedClasspath in Runtime) += (packageBin in Assets).value

libraryDependencies ++= compileDependencies ++ testDependencies

// Task to create a ZIP file containing all WCO XSDs for each version, under the version directory
val zipWcoXsds = taskKey[Pipeline.Stage]("Zips up all WCO declaration XSDs and example messages")

zipWcoXsds := { mappings: Seq[PathMapping] =>
  val targetDir = WebKeys.webTarget.value / "zip"
  val zipFiles: Iterable[java.io.File] =
    ((resourceDirectory in Assets).value / "api" / "conf")
      .listFiles
      .filter(_.isDirectory)
      .map { dir =>
        val wcoXsdPaths = Path.allSubpaths(dir / "schemas")
        val exampleMessagesFilter = new SimpleFileFilter(_.getPath.contains("/example_messages/"))
        val exampleMessagesPaths = Path.selectSubpaths(dir / "examples", exampleMessagesFilter)
        val zipFile = targetDir / "api" / "conf" / dir.getName / "wco-declaration-schemas.zip"
        IO.zip(wcoXsdPaths ++ exampleMessagesPaths, zipFile)
        println(s"Created zip $zipFile")
        zipFile
      }
  zipFiles.pair(Path.relativeTo(targetDir)) ++ mappings
}

pipelineStages := Seq(zipWcoXsds)

evictionWarningOptions in update := EvictionWarningOptions.default.withWarnTransitiveEvictions(false)