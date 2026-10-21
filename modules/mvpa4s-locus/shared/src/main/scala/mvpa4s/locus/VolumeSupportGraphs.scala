package mvpa4s.locus

import mvpa4s.AxisRef
import mvpa4s.pattern.*

enum SupportWeightPolicy:
  case UnitWeights
  case InversePhysicalLength

object VolumeSupportGraphs:
  /** Six-neighbour volume graph from exact integer grid coordinates. Gaps and
    * distinct components remain disconnected. Physical step lengths come from
    * the three affine column norms, so oblique grids retain correct edge units.
    */
  def volume[P](axis: AxisRef[P], coordinates: Vector[(Int, Int, Int)],
      physicalStepLengths: Vector[Double], coordinateUnits: String, gridIdentity: String,
      weights: SupportWeightPolicy = SupportWeightPolicy.UnitWeights): Either[SpatialSupportError, SupportGraph] =
    if coordinates.size != axis.size || coordinates.distinct.size != coordinates.size ||
        coordinates.exists((i, j, k) => i < 0 || j < 0 || k < 0 || i == Int.MaxValue || j == Int.MaxValue || k == Int.MaxValue) ||
        physicalStepLengths.size != 3 || physicalStepLengths.exists(v => !v.isFinite || v <= 0.0) || coordinateUnits.trim.isEmpty then
      Left(SpatialSupportError.Invalid("volume mapping/physical steps/units are invalid"))
    else
      val selected = coordinates.zipWithIndex.toMap
      val edges = Vector.newBuilder[SupportEdge]
      coordinates.zipWithIndex.foreach: (coordinate, ordinal) =>
        val (i, j, k) = coordinate
        Vector((i + 1, j, k), (i, j + 1, k), (i, j, k + 1)).zipWithIndex.foreach: (next, dimension) =>
          selected.get(next).foreach: adjacent =>
            val weight = weights match
              case SupportWeightPolicy.UnitWeights => 1.0
              case SupportWeightPolicy.InversePhysicalLength => 1.0 / physicalStepLengths(dimension)
            edges += SupportEdge(ordinal, adjacent, weight)
      SupportGraph(axis.descriptor, edges.result(), SupportTopology.VolumeFaceNeighbours(gridIdentity), units(weights, coordinateUnits))

  private def units(policy: SupportWeightPolicy, coordinateUnits: String): String = policy match
    case SupportWeightPolicy.UnitWeights => "dimensionless"
    case SupportWeightPolicy.InversePhysicalLength => s"1/$coordinateUnits"
