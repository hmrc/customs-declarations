resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo

addSbtPlugin("uk.gov.hmrc"       %  "sbt-auto-build"        % "3.24.0")
addSbtPlugin("com.github.sbt"    %  "sbt-release"           % "1.0.15")
addSbtPlugin("org.playframework" %  "sbt-plugin"            % "3.0.8")
addSbtPlugin("uk.gov.hmrc"       %  "sbt-distributables"    % "2.6.0")
addSbtPlugin("net.virtual-void"  %  "sbt-dependency-graph"  % "0.10.0-RC1")
addSbtPlugin("org.scoverage"     %  "sbt-scoverage"         % "2.3.0")

addSbtPlugin("com.timushev.sbt"  %  "sbt-updates"           % "0.6.3")
addSbtPlugin("com.eed3si9n"      %  "sbt-buildinfo"         % "0.11.0")