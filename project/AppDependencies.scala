import sbt._

object AppDependencies {

  private val testScope = "test,it"

  val boostrapVersion = "8.5.0"

  val bootstrapBackendPlay30    = "uk.gov.hmrc"                                %% "bootstrap-backend-play-30" % boostrapVersion
  val xmlResolver               = "xml-resolver"                               % "xml-resolver"               % "1.2"
  val cats                      = "org.typelevel"                              %% "cats-core"                 % "2.10.0"
  val hmrcMongo                 = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-play-30"        % "1.8.0"

  val bootstrapBackendTestPlay30    = "uk.gov.hmrc"                                %% "bootstrap-test-play-30"    % boostrapVersion % testScope
  val mockito                       = "org.mockito"                                %% "mockito-scala-scalatest"   % "1.17.29"       % testScope
  val wireMock                      = "org.wiremock"                               %  "wiremock-standalone"       % "3.5.2"         % testScope
  val hmrcMongoTest                 = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-test-play-30"   % "1.8.0"         % testScope
  val hmrcBootstrapTest             = "uk.gov.hmrc"                                %% "bootstrap-test-play-30"    % "8.5.0"         % testScope
  val scalaTestPlusMockito          = "org.scalatestplus"                          %% "scalatestplus-mockito"     % "1.0.0-M2"      % testScope
  val jackson                       = "com.fasterxml.jackson.module"               %% "jackson-module-scala"      % "2.15.0"        % testScope

}
