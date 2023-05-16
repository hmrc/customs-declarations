import sbt._

object AppDependencies {

  private val testScope = "test,it"

  val scalaTestPlusPlay     = "org.scalatestplus.play"                     %% "scalatestplus-play"      % "5.1.0"    % testScope
  val flexmark              = "com.vladsch.flexmark"                       %  "flexmark-all"            % "0.35.10"  % testScope
  val mockito               = "org.scalatestplus"                          %% "mockito-3-4"             % "3.2.10.0" % testScope
  val wireMock              = "com.github.tomakehurst"                     %  "wiremock-standalone"     % "2.27.2"   % testScope
  val customsApiCommon      = "uk.gov.hmrc"                                %% "customs-api-common"      % "1.59.0"   withSources()
  val customsApiCommonTests = "uk.gov.hmrc"                                %% "customs-api-common"      % "1.59.0"   % testScope classifier "tests"
  val hmrcMongo             = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-play-28"      % "1.2.0"
  val hmrcMongoTest         = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-test-play-28" % "1.2.0"   % testScope
  val hmrcBootstrapTest         = "uk.gov.hmrc"                                %% "bootstrap-test-play-28"  % "7.15.0" % testScope

}
