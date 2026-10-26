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
ThisBuild / licenses := List(
  "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
)

// Providers are immutable source pins. Each `mvpa4s.<provider>.build` property
// is an explicit sibling-checkout override for coordinated development; a
// consuming build forwards its own overrides here so one provider revision
// resolves throughout the graph.
def pinned(provider: String, revision: String): URI =
  sys.props
    .get(s"mvpa4s.$provider.build")
    .map(path => file(path).getCanonicalFile.toURI)
    .getOrElse(uri(s"https://github.com/canardlapin/$provider.git#$revision"))

lazy val galeRevision = "779ed3d8169cdd9f4eb9ab10b1ca9defadc27715"
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
lazy val locus4sDataJVM = ProjectRef(locus4sBuild, "locus4s-dataJVM")
lazy val locus4sDataJS = ProjectRef(locus4sBuild, "locus4s-dataJS")
lazy val locus4sProvenanceJVM = ProjectRef(locus4sBuild, "locus4s-provenanceJVM")
lazy val locus4sProvenanceJS = ProjectRef(locus4sBuild, "locus4s-provenanceJS")

// Multivar inference must share the same Resample4s classes as the MVPA core.
// Its Gale and Resample4s artifacts are prepared by
// tools/prepare-pinned-dependencies.sh.
lazy val multivarRevision = "7afdeea290fdcc80b81a4126aa303f93a5348c0e"
lazy val multivarBuild = {
  System.setProperty("multivar.resample4s.build.uri", resample4sBuild.toString)
  pinned("multivar", multivarRevision)
}
lazy val multivarJVM = ProjectRef(multivarBuild, "coreJVM")
lazy val multivarJS = ProjectRef(multivarBuild, "coreJS")
lazy val multivarInferenceJVM = ProjectRef(multivarBuild, "inferenceJVM")
lazy val multivarInferenceJS = ProjectRef(multivarBuild, "inferenceJS")

// Alder owns fit lifecycles, stage-aware cross-fitting and fitted artifacts;
// only the optional predictive module depends on it. Alder's own provider
// overrides take checkout paths, so path overrides given here are forwarded.
// Its Gale pin must equal galeRevision; tools/prepare-pinned-dependencies.sh
// checks this.
lazy val alderRevision = "7618c715002b3912f856c4d44468ae173fce4dec"
lazy val alderBuild = {
  Seq("resample4s", "gale", "linop4s").foreach { provider =>
    sys.props
      .get(s"mvpa4s.$provider.build")
      .foreach(System.setProperty(s"alder.$provider.build", _))
  }
  pinned("alder", alderRevision)
}
lazy val alderKernelJVM = ProjectRef(alderBuild, "kernelJVM")
lazy val alderKernelJS = ProjectRef(alderBuild, "kernelJS")
lazy val alderDataJVM = ProjectRef(alderBuild, "dataJVM")
lazy val alderDataJS = ProjectRef(alderBuild, "dataJS")
lazy val alderModelsLinearJVM = ProjectRef(alderBuild, "modelsLinearJVM")
lazy val alderModelsLinearJS = ProjectRef(alderBuild, "modelsLinearJS")
lazy val alderRidgeGaleJVM = ProjectRef(alderBuild, "ridgeGaleJVM")
lazy val alderRidgeGaleJS = ProjectRef(alderBuild, "ridgeGaleJS")
lazy val alderTuneJVM = ProjectRef(alderBuild, "tuneJVM")
lazy val alderTuneJS = ProjectRef(alderBuild, "tuneJS")
lazy val alderMetricsJVM = ProjectRef(alderBuild, "metricsJVM")
lazy val alderMetricsJS = ProjectRef(alderBuild, "metricsJS")

// Optional imaging integrations share the caller's explicit provider overrides.
// They add no dependency edge from core or locus to an imaging runtime.
def forwardImagingOverrides(owner: String): Unit =
  Seq("locus4s", "neuroim4s", "neurosurf4s", "mesh4s", "image4s", "spatial4s",
    "reframe4s", "gale", "graph4s", "zarr4s", "ravel", "intaglio").foreach { provider =>
    sys.props.get(s"mvpa4s.$provider.build").foreach(System.setProperty(s"$owner.$provider.build", _))
  }

lazy val neurosurf4sRevision = "a8a1191a57af2bf60f8ded6b14210f5025f1887a"
lazy val neurosurf4sBuild = {
  forwardImagingOverrides("neurosurf4s")
  pinned("neurosurf4s", neurosurf4sRevision)
}
lazy val neurosurf4sCoreJVM = ProjectRef(neurosurf4sBuild, "coreJVM")
lazy val neurosurf4sCoreJS = ProjectRef(neurosurf4sBuild, "coreJS")

lazy val neuroatlas4sRevision = "567c78e0e4e93f19d675159103c03009b7634c22"
lazy val neuroatlas4sBuild = {
  forwardImagingOverrides("neuroatlas4s")
  pinned("neuroatlas4s", neuroatlas4sRevision)
}
lazy val neuroatlas4sCoreJVM = ProjectRef(neuroatlas4sBuild, "coreJVM")
lazy val neuroatlas4sCoreJS = ProjectRef(neuroatlas4sBuild, "coreJS")

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

// Optional Alder adapters: training-only preparation, held-out prediction,
// validation and tuning over mvpa4s evidence.
lazy val predictive =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("modules/mvpa4s-predictive"))
    .dependsOn(core, testkit % Test)
    .settings(commonSettings)
    .settings(
      name := "mvpa4s-predictive",
      description := "Alder predictive adapters for identified mvpa4s evidence."
    )
    .jvmConfigure(
      _.dependsOn(
        alderKernelJVM,
        alderDataJVM,
        alderModelsLinearJVM,
        alderRidgeGaleJVM,
        alderTuneJVM,
        alderMetricsJVM
      )
    )
    .jsConfigure(
      _.dependsOn(
        alderKernelJS,
        alderDataJS,
        alderModelsLinearJS,
        alderRidgeGaleJS,
        alderTuneJS,
        alderMetricsJS
      )
    )
    .jsSettings(jsSettings)

lazy val predictiveJVM = predictive.jvm
lazy val predictiveJS = predictive.js

// Optional finite-domain measurement and scatter adapters. Imaging and surface
// composition remains downstream.
lazy val locus =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("modules/mvpa4s-locus"))
    .dependsOn(core)
    .settings(commonSettings)
    .settings(
      name := "mvpa4s-locus",
      description := "Locus support measurements, centered neighborhoods, scatter and volume support graphs."
    )
    .jvmConfigure(_.dependsOn(locus4sDataJVM))
    .jsConfigure(_.dependsOn(locus4sDataJS))
    .jsSettings(jsSettings)

lazy val locusJVM = locus.jvm
lazy val locusJS = locus.js

// Optional anatomical mesh support; no volume, atlas or rendering dependency.
lazy val surface = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/mvpa4s-surface"))
  .dependsOn(locus)
  .settings(commonSettings)
  .settings(name := "mvpa4s-surface")
  .jvmConfigure(_.dependsOn(neurosurf4sCoreJVM))
  .jsConfigure(_.dependsOn(neurosurf4sCoreJS))
  .jsSettings(jsSettings)

// Atlas assignments are adapted through a certified sample correspondence.
// Only the runtime-independent atlas semantic core is required.
lazy val atlas = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/mvpa4s-atlas"))
  .dependsOn(locus)
  .settings(commonSettings)
  .settings(name := "mvpa4s-atlas")
  .jvmConfigure(_.dependsOn(neuroatlas4sCoreJVM))
  .jsConfigure(_.dependsOn(neuroatlas4sCoreJS))
  .jsSettings(jsSettings)

lazy val imagingConsumer = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("examples/imaging-consumer"))
  .dependsOn(surface, atlas)
  .settings(commonSettings)
  .settings(name := "mvpa4s-imaging-consumer", publish / skip := true)
  .jsSettings(jsSettings)

// An external consumer: it lives outside the `mvpa4s` package, so it compiles
// only against the public API and cannot reach package-private members.
lazy val consumer =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("examples/consumer"))
    .dependsOn(core, predictive)
    .settings(commonSettings)
    .settings(name := "mvpa4s-consumer-example", publish / skip := true)
    .jsSettings(jsSettings)

lazy val root =
  project
    .in(file("."))
    .aggregate(core.jvm, core.js, testkit.jvm, testkit.js, predictive.jvm, predictive.js, locus.jvm, locus.js, surface.jvm, surface.js, atlas.jvm, atlas.js, imagingConsumer.jvm, imagingConsumer.js, consumer.jvm, consumer.js)
    .settings(name := "mvpa4s-root", publish / skip := true)

addCommandAlias("mvpa4sCompileAll", ";root/compile;root/Test/compile")
addCommandAlias("mvpa4sTestAll", ";coreJVM/test;coreJS/test;testkitJVM/test;testkitJS/test;predictiveJVM/test;predictiveJS/test;locusJVM/test;locusJS/test;consumerJVM/test;consumerJS/test;mvpa4sImagingTestAll")

addCommandAlias("mvpa4sImagingTestAll", ";surfaceJVM/test;surfaceJS/test;atlasJVM/test;atlasJS/test;imagingConsumerJVM/test;imagingConsumerJS/test")
