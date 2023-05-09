import sbt._

object AppDependencies {

  private val testScope = "test,it"
  private val customs_api_common_verison = "1.57.1"

  val scalaTestPlusPlay     = "org.scalatestplus.play"                     %% "scalatestplus-play"      % "5.1.0"    % testScope
  val flexmark              = "com.vladsch.flexmark"                       %  "flexmark-all"            % "0.35.10"  % testScope
  val mockito               = "org.scalatestplus"                          %% "mockito-3-4"             % "3.2.10.0" % testScope
  val wireMock              = "com.github.tomakehurst"                     %  "wiremock-standalone"     % "2.27.2"   % testScope
  val customsApiCommon      = "uk.gov.hmrc"                                %% "customs-api-common"      % customs_api_common_verison   withSources()
  val customsApiCommonTests = "uk.gov.hmrc"                                %% "customs-api-common"      % customs_api_common_verison   % testScope classifier "tests"
  val playJsonJoda          = "com.typesafe.play"                          %% "play-json-joda"          % "2.9.4"
  val hmrcMongo             = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-play-28"      % "0.74.0"
  val hmrcMongoTest         = "uk.gov.hmrc.mongo"                          %% "hmrc-mongo-test-play-28" % "0.74.0"   % testScope
  val silencerPlugin        = compilerPlugin("com.github.ghik" %  "silencer-plugin"         % "1.7.11"   cross CrossVersion.full)
  val silencerLib           = "com.github.ghik"                            %  "silencer-lib"            % "1.7.11"   % Provided cross CrossVersion.full

}
