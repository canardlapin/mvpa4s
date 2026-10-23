package mvpa4s.predictive

import alder.data.{FixedCoverage, FixedHoldout, FixedSelectionError, IdentifiedRows}
import alder.kernel.*
import mvpa4s.*
import resample4s.core.{Coverage, UnitKey}

/** Common row/role mechanics. Scientific policy and persisted identities stay
  * in the concrete adapters; callbacks run in the original traversal order. */
private[predictive] object AlderOperatorLifecycle:
  final case class RowAccess[R](nativeId: R => Long, ordinal: R => Int)

  def foreachFold[S <: multivar.core.SemanticSpace, K, R, E](
      root: IdentifiedRows[Example[R, Array[Double], Unit]],
      mapping: NativeAxisMapping,
      design: ValidationDesign[S, K, Coverage.ExactOnce]
  )(
      evidenceError: EvidenceError => E,
      selectionError: FixedSelectionError => E
  )(
      consume: (Int, UnitKey, FixedHoldout[Example[R, Array[Double], Unit]]) => Either[E, Unit]
  ): Either[E, Unit] =
    var fold = 0
    var failure = Option.empty[E]
    while fold < design.keys.length && failure.isEmpty do
      val result = design.at(design.keys(fold)).left.map(evidenceError).flatMap: unit =>
        val trainIds = unit.analysis.ordinals.toVector.map(mapping.nativeIds)
        val testIds = unit.assessment.ordinals.toVector.map(mapping.nativeIds)
        root.fixedHoldout(trainIds, testIds, FixedCoverage.Exhaustive)
          .left.map(selectionError)
          .flatMap(split => consume(fold, unit.key, split))
      failure = result.swap.toOption
      fold += 1
    failure.toLeft(())

  def foreachTrainingRow[U <: Use.Fit, R, E](
      data: Data[U, Example[R, Array[Double], Unit]],
      mapping: NativeAxisMapping,
      access: RowAccess[R]
  )(
      mismatch: (Long, Int) => E
  )(
      consume: (Int, Array[Double]) => Either[E, Unit]
  ): Either[E, Unit] =
    var failure = Option.empty[E]
    data.foreachRow: (id, example) =>
      if failure.isEmpty then
        val ordinal = access.ordinal(example.input)
        if ordinal < 0 || ordinal >= mapping.nativeIds.length || id.value != access.nativeId(example.input) || id.value != mapping.nativeIds(ordinal) then
          failure = Some(mismatch(id.value, ordinal))
        else failure = consume(ordinal, example.target).swap.toOption
    failure.toLeft(())

  def stableKeys[R](
      data: Data[?, Example[R, Array[Double], Unit]],
      mapping: NativeAxisMapping,
      access: RowAccess[R]
  ): Vector[String] =
    data.foldRows(Vector.empty[String])((out, _, example) => out :+ mapping.entriesByOrdinal(access.ordinal(example.input)).stableKey)

  def budget[E](required: Long, budget: MaterializationBudget)(error: (Long, Long) => E): Either[E, Unit] =
    if required <= budget.maximumCells then Right(()) else Left(error(required, budget.maximumCells))
