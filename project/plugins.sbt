resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo

addSbtPlugin("uk.gov.hmrc"       %  "sbt-auto-build"        % "3.22.0")
addSbtPlugin("com.github.sbt"    %  "sbt-release"           % "1.0.15")
addSbtPlugin("org.playframework" %  "sbt-plugin"            % "3.0.3")
addSbtPlugin("uk.gov.hmrc"       %  "sbt-distributables"    % "2.5.0")
addSbtPlugin("net.virtual-void"  %  "sbt-dependency-graph"  % "0.10.0-RC1")
addSbtPlugin("org.scoverage"     %  "sbt-scoverage"         % "2.2.1")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always