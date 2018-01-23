import AppDependencies._
import org.scalastyle.sbt.ScalastylePlugin._
import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.PublishingSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning

import scala.language.postfixOps

name := "customs-declarations"

targetJvm := "jvm-1.8"

lazy val allResolvers = resolvers ++= Seq(
  Resolver.bintrayRepo("hmrc", "releases"),
  Resolver.jcenterRepo
)

lazy val AcceptanceTest = config("acceptance") extend Test
lazy val CdsIntegrationTest = config("it") extend Test

val testConfig = Seq(AcceptanceTest, CdsIntegrationTest, Test)

def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

lazy val testAll = TaskKey[Unit]("test-all")
lazy val allTest = Seq(testAll := (test in AcceptanceTest)
  .dependsOn((test in CdsIntegrationTest).dependsOn(test in Test)).value)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .enablePlugins(SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    unitTestSettings,
    integrationTestSettings,
    acceptanceTestSettings,
    playSettings,
    playPublishingSettings,
    allTest,
    scoverageSettings,
    allResolvers
  )

def onPackageName(rootPackage: String): (String => Boolean) = {
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
      testOptions in CdsIntegrationTest := Seq(Tests.Filters(Seq(onPackageName("integration"), onPackageName("acceptance")))),
      testOptions in CdsIntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in CdsIntegrationTest := false,
      parallelExecution in CdsIntegrationTest := false,
      addTestReportOption(CdsIntegrationTest, "int-test-reports"),
      testGrouping in CdsIntegrationTest := forkedJvmPerTestConfig((definedTests in Test).value, "integration", "acceptance")
    )

lazy val acceptanceTestSettings =
  inConfig(AcceptanceTest)(Defaults.testTasks) ++
    Seq(
      testOptions in AcceptanceTest := Seq(Tests.Filter(onPackageName("acceptance"))),
      testOptions in AcceptanceTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in AcceptanceTest := false,
      parallelExecution in AcceptanceTest := false,
      addTestReportOption(AcceptanceTest, "acceptance-reports")
    )

lazy val commonSettings: Seq[Setting[_]] = scalaSettings ++
  publishingSettings ++
  defaultSettings() ++
  gitStampSettings

lazy val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq("uk.gov.hmrc.customs.api.common.domain._")
)

lazy val playPublishingSettings: Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++
  Seq(credentials += SbtCredentials) ++
  publishAllArtefacts

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
      "<empty>"
      ,"Reverse.*"
      ,"uk\\.gov\\.hmrc\\.customs\\.declaration\\.model\\..*"
      ,"uk\\.gov\\.hmrc\\.customs\\.declaration\\.views\\..*"
      ,".*(AuthService|BuildInfo|Routes).*"
    ).mkString(";"),
  coverageMinimum := 95,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

val compileDependencies = Seq(microserviceBootStrap, authClient, xmlResolver, customsApiCommon)


val testDependencies = Seq(hmrcTest, scalaTest, pegDown,
  scalaTestPlusPlay, wireMock, mockito, customsApiCommonTests)

unmanagedResourceDirectories in Compile += baseDirectory.value / "public"

libraryDependencies ++= compileDependencies ++ testDependencies

// Task to create a ZIP file containing all WCO XSDs for each version, under the version directory
lazy val zipWcoXsds = taskKey[Unit]("Zips up all WCO declaration XSD's and example messages")
zipWcoXsds := {
  (baseDirectory.value / "public" / "api" / "conf")
    .listFiles()
    .filter(_.isDirectory)
    .foreach { dir =>
      val wcoXsdPaths = Path.allSubpaths(dir / "schemas" / "wco")
      val exampleMessagesFilter = new SimpleFileFilter(_.getPath.contains("/example_messages/"))
      val exampleMessagesPaths = Path.selectSubpaths(dir / "examples", exampleMessagesFilter)
      val zipFile = dir / "wco-declaration-schemas.zip"
      IO.zip(wcoXsdPaths ++ exampleMessagesPaths, zipFile)
    }
}

// default package task depends on packageBin which we override here to also invoke the custom ZIP task
packageBin in Compile := {
  zipWcoXsds.value
  (packageBin in Compile).value
}

evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
