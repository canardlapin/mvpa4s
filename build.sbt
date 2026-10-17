import org.scalajs.linker.interface.ModuleKind
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import sbtcrossproject.CrossPlugin.autoImport.*
import sbtcrossproject.CrossType
import scalajscrossproject.ScalaJSCrossPlugin.autoImport.*

ThisBuild / organization := "io.github.canardlapin"
ThisBuild / scalaVersion := "3.7.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / homepage := Some(url("https://github.com/canardlapin/mvpa4s"))

// Providers are immutable source pins. Each `mvpa4s.<provider>.build` property
// is an explicit sibling-checkout override for coordinated development; a
// consuming build forwards its own overrides here so one provider revision
// resolves throughout the graph.
def pinned(provider: String, revision: String): URI =
  sys.props
    .get(s"mvpa4s.$provider.build")
    .map(path => file(path).getCanonicalFile.toURI)
    .getOrElse(uri(s"https://github.com/canardlapin/$provider.git#$revision"))

lazy val galeRevision = "6f7a1f9509a07c2b3f5b956332d39a1bc5b64c3c"
lazy val galeBuild = pinned("gale", galeRevision)
lazy val galeCoreJVM = ProjectRef(galeBuild, "coreJVM")
lazy val galeCoreJS = ProjectRef(galeBuild, "coreJS")

lazy val resample4sRevision = "6bc4172a966c92f1b06811eac64ac2bada9fef9b"
lazy val resample4sBuild = pinned("resample4s", resample4sRevision)
lazy val resample4sCoreJVM = ProjectRef(resample4sBuild, "coreJVM")
lazy val resample4sCoreJS = ProjectRef(resample4sBuild, "coreJS")
lazy val resample4sDesignsJVM = ProjectRef(resample4sBuild, "designsJVM")
lazy val resample4sDesignsJS = ProjectRef(resample4sBuild, "designsJS")

lazy val locus4sRevision = "e32b0aeba5c4d19c457b2329272b95b26c061943"
lazy val locus4sBuild = pinned("locus4s", locus4sRevision)
lazy val locus4sCoreJVM = ProjectRef(locus4sBuild, "locus4s-coreJVM")
lazy val locus4sCoreJS = ProjectRef(locus4sBuild, "locus4s-coreJS")
lazy val locus4sProvenanceJVM = ProjectRef(locus4sBuild, "locus4s-provenanceJVM")
lazy val locus4sProvenanceJS = ProjectRef(locus4sBuild, "locus4s-provenanceJS")

// Multivar inference must share the same Resample4s classes as the MVPA core.
// Its Gale and Resample4s artifacts are prepared by
// tools/prepare-pinned-dependencies.sh.
lazy val multivarRevision = "2bc14d14ba42a678fbbe169293153eddaba6175c"
lazy val multivarBuild = {
  System.setProperty("multivar.resample4s.build.uri", resample4sBuild.toString)
  pinned("multivar", multivarRevision)
}
lazy val multivarJVM = ProjectRef(multivarBuild, "coreJVM")
lazy val multivarJS = ProjectRef(multivarBuild, "coreJS")
lazy val multivarInferenceJVM = ProjectRef(multivarBuild, "inferenceJVM")
lazy val multivarInferenceJS = ProjectRef(multivarBuild, "inferenceJS")

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xmax-inlines:64",
    "-release:17"
  ),
  Test / fork := false,
  libraryDependencies += "org.scalameta" %%% "munit" % "1.2.1" % Test
)

lazy val jsSettings = Seq(
  scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
  Test / jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()
)

lazy val core =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("modules/mvpa4s-core"))
    .settings(commonSettings)
    .settings(
      name := "mvpa4s-core",
      description := "Identified multivariate evidence, relational and pattern analysis, and measurement composition.",
      libraryDependencies += "com.lihaoyi" %%% "ujson" % "4.1.0" % Test
    )
    .jvmConfigure(
      _.dependsOn(
        galeCoreJVM,
        locus4sCoreJVM,
        locus4sProvenanceJVM,
        multivarJVM,
        multivarInferenceJVM,
        resample4sCoreJVM,
        resample4sDesignsJVM
      )
    )
    .jsConfigure(
      _.dependsOn(
        galeCoreJS,
        locus4sCoreJS,
        locus4sProvenanceJS,
        multivarJS,
        multivarInferenceJS,
        resample4sCoreJS,
        resample4sDesignsJS
      )
    )
    .jsSettings(jsSettings)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

// Test support for downstream suites: validated fixture identities and the R
// parity reference data (checked against the core by this module's own tests).
// Consumers depend on it in Test scope only.
lazy val testkit =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("modules/mvpa4s-testkit"))
    .dependsOn(core)
    .settings(commonSettings)
    .settings(
      name := "mvpa4s-testkit",
      description := "Fixture identities and numerical parity reference data for tests of mvpa4s consumers."
    )
    .jsSettings(jsSettings)

// An external consumer: it lives outside the `mvpa4s` package, so it compiles
// only against the public API and cannot reach package-private members.
lazy val consumer =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("examples/consumer"))
    .dependsOn(core)
    .settings(commonSettings)
    .settings(name := "mvpa4s-consumer-example", publish / skip := true)
    .jsSettings(jsSettings)

lazy val root =
  project
    .in(file("."))
    .aggregate(core.jvm, core.js, testkit.jvm, testkit.js, consumer.jvm, consumer.js)
    .settings(name := "mvpa4s-root", publish / skip := true)

addCommandAlias("mvpa4sCompileAll", ";root/compile;root/Test/compile")
addCommandAlias("mvpa4sTestAll", ";coreJVM/test;coreJS/test;testkitJVM/test;testkitJS/test;consumerJVM/test;consumerJS/test")
