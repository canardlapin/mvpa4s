package mvpa4s.predictive

import alder.kernel.{DataFingerprint, Example}
import gale.linalg.{DVec, DoubleLinearOperator, MutableDVec}
import multivar.core.SpaceRole
import mvpa4s.*
import resample4s.core.{DigestAlgorithm, Labels}
import resample4s.designs.FixedPartitions

class AlderOperatorLifecycleSuite extends munit.FunSuite:
  given DigestAlgorithm = DigestAlgorithm.fnv1a64
  private def right[A](value: Either[?, A]): A = value.fold(error => fail(error.toString), identity)
  private final case class Row(nativeId: Long, ordinal: Int)
  private val access = AlderOperatorLifecycle.RowAccess[Row](_.nativeId, _.ordinal)
  private val keys = Vector.tabulate(6)(i => s"sample:$i")
  private val axis = right(AxisRef.fromStableKeys("lifecycle-samples", SpaceRole.Samples, keys, "trial", "none", "one"))
  private val mapping = right(NativeAxisMapping.fromAxis(axis, keys.indices.map(i => 900L + i).toVector, DataFingerprint.external("lifecycle-suite")))

  private def root(change: (Long, Int) => Row = Row.apply) =
    right(mapping.identifiedRows((id, ordinal) => Example(change(id, ordinal), Array(ordinal.toDouble), ())))

  test("an earlier target error wins over a later invalid row and stops target decoding"):
    val rows = root((id, ordinal) => Row(id, if ordinal == 1 then 99 else ordinal))
    val train = right(rows.training(rows.ids))
    val seen = Vector.newBuilder[Int]
    val result = AlderOperatorLifecycle.foreachTrainingRow(train.data, mapping, access)(
      (id, ordinal) => s"row:$id:$ordinal")((ordinal, _) =>
      seen += ordinal
      Left(s"target:$ordinal")
    )
    assertEquals(result, Left("target:0"))
    assertEquals(seen.result(), Vector(0))

  test("invalid row identity is refused before its target is decoded"):
    val rows = root((id, ordinal) => Row(id + 1, ordinal))
    val train = right(rows.training(rows.ids))
    var decoded = 0
    val result = AlderOperatorLifecycle.foreachTrainingRow(train.data, mapping, access)(
      (id, ordinal) => s"row:$id:$ordinal")((_, _) =>
      decoded += 1
      Right(())
    )
    assertEquals(result, Left("row:900:0"))
    assertEquals(decoded, 0)

  test("stable audit keys follow the selected Fit rows in their supplied order"):
    val rows = root()
    val train = right(rows.training(Vector(905L, 902L, 904L)))
    assertEquals(AlderOperatorLifecycle.stableKeys(train.data, mapping, access), Vector("sample:5", "sample:2", "sample:4"))

  test("the first failed fold stops traversal without touching a later fit"):
    val rows = root()
    val partitions = right(FixedPartitions.once(right(Labels.retained(IArray.unsafeFromArray(Array(0, 0, 1, 1, 2, 2))))))
    val design = right(ValidationDesign.bind(axis, partitions, ScientificSeed.fromLong(19L)))
    val seen = Vector.newBuilder[Int]
    val result = AlderOperatorLifecycle.foreachFold(rows, mapping, design)(
      _.toString, _.toString)((fold, _, split) =>
      seen += fold
      assertEquals(split.train.data.size, 4L)
      assertEquals(split.test.data.size, 2L)
      Left(s"fit:$fold")
    )
    assertEquals(result, Left("fit:0"))
    assertEquals(seen.result(), Vector(0))

  test("public admission preserves shape-before-budget precedence without reading features"):
    final class Poison extends DoubleLinearOperator:
      val rows = 6
      val cols = 2
      var calls = 0
      def applyTo(input: DVec, output: MutableDVec): Unit =
        calls += 1
        fail("admission read operator features")
    val poison = new Poison
    val operator = right(PatternOperator.fromOperator(6, Vector(FeatureIndex(0), FeatureIndex(1)), poison, PatternOperatorProvenance.composed))
    val budget = right(MaterializationBudget(1))
    val targets = Vector.fill(5)(0.0)
    AlderOperatorRidge.admit(operator, targets, mapping, budget) match
      case Left(AlderOperatorRidgeError.Admission(AlderPredictiveAdmissionError.MatrixShapeMismatch("targets", 5, 1, 6, 1))) => ()
      case other => fail(s"changed ridge admission precedence: $other")
    AlderSoftLda.admit(operator, targets, mapping, budget) match
      case Left(AlderSoftLdaError.Admission(AlderPredictiveAdmissionError.MatrixShapeMismatch("targets", 5, 1, 6, 1))) => ()
      case other => fail(s"changed soft LDA admission precedence: $other")
    val complete = Vector.fill(6)(0.0)
    assertEquals(AlderOperatorRidge.admit(operator, complete, mapping, budget).left.toOption, Some(AlderOperatorRidgeError.Budget(6L, 1L)))
    assertEquals(AlderSoftLda.admit(operator, complete, mapping, budget).left.toOption, Some(AlderSoftLdaError.Budget(6L, 1L)))
    assertEquals(poison.calls, 0)
