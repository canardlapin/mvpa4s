package example

import alder.kernel.{DataFingerprint, Example}
import gale.linalg.DMat
import gale.optim.{FirstOrderConfig, FirstOrderTolerance}
import locus4s.provenance.{Provenance, ProvenanceId, SourceId}
import multivar.core.{CoordinateEvidence, Lin, SemanticProvenance, SpaceRole, ValueId, ValueIdentity}
import mvpa4s.{
  AxisDigest,
  AxisRef,
  EvidenceError,
  EvidenceOrigins,
  EvidenceSource,
  FeatureModelMetrics,
  FeatureModelPrediction,
  FeaturePredictionDirection,
  ReindexingLeg,
  StandardizedRidgeMap
}
import mvpa4s.pattern.{
  ConvergencePolicy,
  ResidualCovarianceFitPolicy,
  StructuredPatternConfig,
  SupportPenalty,
  TwoStagePatternFitPolicy
}
import mvpa4s.predictive.NativeAxisMapping
import mvpa4s.relation.{EffectEstimability, RelationOrigins, RelationSource, ScopeReplay}
import resample4s.core.{Draw, IndexSpace, Injection}

/** Compiles only against public mvpa4s API: this package sees no
  * package-private member, so a narrowed boundary breaks this build.
  */
object MvpaConsumer:
  def trials: Either[EvidenceError, AxisRef[String]] =
    AxisRef.fromStableKeys(
      "trials",
      SpaceRole.Samples,
      Vector("t0", "t1", "t2"),
      "acquisition-order",
      "trial",
      "unscaled",
      Vector("source:run-1")
    )

  /** A repeated draw keeps occurrence order; a unique selection refuses repeats. */
  def resample(axis: AxisRef[String]): Either[String, Vector[Int]] =
    for
      space <- IndexSpace.of(axis.size).left.map(_.toString)
      _ <- Either.cond(Injection.from(IArray(2, 0, 2), space).isLeft, (), "repeat admitted as unique")
      draw <- Draw.from(IArray(2, 0, 2), space).left.map(_.toString)
      leg <- ReindexingLeg.bind(axis, draw).left.map(_.message)
    yield leg.ordinals.toVector

  /** Lineage enters evidence through the shared locus4s-provenance contract. */
  def source: Either[EvidenceError, EvidenceSource] =
    val id = SourceId.unsafe("consumer-run")
    EvidenceSource(id, Provenance.source(ProvenanceId.unsafe("consumer-run:root"), id))

  /** Downstream identities frame through the canonical digest writer. */
  def bindingDigest(axis: AxisRef[String]): String =
    AxisDigest.sha256Hex: writer =>
      writer.string("example.consumer-binding.v1")
      writer.string(axis.descriptor.stableKey)

  /** An acquiring provider mints and expires its own scoped replay. */
  def scopedLifetime: Either[EvidenceError, Boolean] =
    ScopeReplay.acquire("example-provider", "v1", "binding").map: lease =>
      lease.expire()
      lease.replay.isActive

  /** A scoped relation is attached through the minting lease only. */
  def scopedRelation(effects: AxisRef[String], neural: AxisRef[String]): Either[String, Boolean] =
    for
      lease <- ScopeReplay.acquire("example-provider", "v1", "binding").left.map(_.message)
      table <- Lin
        .fromDenseMatrix(
          DMat.dense(effects.size, neural.size, Vector.fill(effects.size * neural.size)(1.0)),
          CoordinateEvidence.dual(neural.evidence),
          CoordinateEvidence.primal(effects.evidence),
          ValueIdentity.source(ValueId.unsafe("example-readout")),
          SemanticProvenance.source("example")
        )
        .left
        .map(_.toString)
      origins = RelationOrigins(RelationSource("run-1", "r", "readout", "none", "noise"), lease.access)
      relation <- lease
        .attach(effects, neural, table, origins, Vector.fill(effects.size)(EffectEstimability.Estimable))
        .left
        .map(_.message)
    yield
      lease.expire()
      relation.origins.access == lease.access

  /** A fitted ridge map exposes predictions, metrics and immutable standardization. */
  def ridge(source: DMat, target: DMat): Either[String, (Vector[Double], Double)] =
    for
      model <- StandardizedRidgeMap.fit(source, target, 1.0).left.map(_.toString)
      predicted <- model.predict(source).left.map(_.toString)
      metrics <- FeatureModelMetrics
        .compute(
          FeatureModelPrediction(
            FeaturePredictionDirection.FeaturesToPatterns,
            Vector.tabulate(source.rows)(row => s"item-$row"),
            Vector.tabulate(target.cols)(col => s"target-$col"),
            predicted,
            target
          )
        )
        .left
        .map(_.toString)
    yield (model.sourceMeans, metrics.mse)

  /** Origins reindex with derived values and frame into consumer digests. */
  def originsDigest(axis: AxisRef[String]): String =
    val origins = EvidenceOrigins.Unknown.reindexOutput(axis.descriptor, ValueIdentity.source(ValueId.unsafe("derived")))
    AxisDigest.sha256Hex: writer =>
      writer.string(origins.identityDigest)
      origins.writeFramed(writer)

  /** A pattern-fit policy binds into consumer identities by its canonical digest. */
  def policyIdentity: Either[String, String] =
    for
      penalty <- SupportPenalty(0.0, 0.0).left.map(_.toString)
      tolerance <- FirstOrderTolerance.from(1e-10, 1e-10).left.map(_.toString)
      inner <- FirstOrderConfig.from(100000, tolerance).left.map(_.toString)
      structured <- StructuredPatternConfig(penalty, maximumOuterIterations = 250, inner = inner,
        stationarityTolerance = 1e-7, objectiveTolerance = 1e-9, maximumWorkspaceCells = 10000000L,
        maximumOperatorColumns = 100000L).left.map(_.toString)
      covariance <- ResidualCovarianceFitPolicy(1, maximumIterations = 5000, tolerance = 1e-9,
        diagonalMomentTolerance = 1e-7, centeringTolerance = 1e-10, convergence = ConvergencePolicy.Refuse)
        .left
        .map(_.toString)
    yield TwoStagePatternFitPolicy(structured, covariance, 10000000L).identity

  /** A downstream learner binds its own row type to declared native ids. */
  def predictiveRows(axis: AxisRef[String]): Either[String, Vector[Long]] =
    for
      mapping <- NativeAxisMapping
        .fromAxis(axis, Vector(900L, 901L, 902L), DataFingerprint.external("consumer"))
        .left.map(_.toString)
      rows <- mapping.identifiedRows((id, ordinal) => Example(ordinal, Array(id.toDouble), ())).left.map(_.toString)
    yield rows.ids
