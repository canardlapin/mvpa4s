package example

import locus4s.Region
import multivar.core.SpaceRole
import mvpa4s.AxisRef
import mvpa4s.locus.*
import mvpa4s.measurement.{MeasurementFrameDeclaration, MeasurementId}

/** A consumer outside the provider package uses only the public boundary. */
class LocusConsumerSuite extends munit.FunSuite:
  private def right[A](value: Either[?, A]): A = value.fold(e => fail(e.toString), identity)

  test("public locus measurements and scatter compose without imaging dependencies"):
    val axis = right(AxisRef.fromStableKeys("external-volume", SpaceRole.Observed,
      Vector("a", "b", "c"), "voxel", "psc", "raw"))
    val support = right(Region.fromOrdinals(axis.locus, Vector(0, 1)))
    val id = MeasurementId.unsafe("region")
    val frame = right(SpatialMeasurementFrames.regions(axis,
      MeasurementFrameDeclaration("external", "v1", Vector.empty),
      Vector(SpatialMeasurementSite(id, SpatialMeasurementSupport.Regional(support)))))
    assertEquals(frame.identity.source, axis.descriptor)
    assertEquals(SpatialMeasurementFrames.centeredId(axis, CenteredMeasurementKind.Volume,
      axis.locus.indexAtValidatedOrdinal(1)).value,
      s"volume:${axis.descriptor.coordinateSignature.value}:1")
    val field = right(SpatialMeasurementScatter.scatter(axis.locus,
      Vector(SpatialLocalOutcome(id, support, 2.0, Right(6.0))), SpatialScatterAlgebra.weightedMeanDouble))
    field(axis.locus.indexAtValidatedOrdinal(0)) match
      case SpatialScatterCell.Aggregated(value, _, denominator, _) =>
        assertEqualsDouble(value, 6.0, 0.0)
        assertEqualsDouble(denominator, 2.0, 0.0)
      case other => fail(other.toString)
    assertEquals(field(axis.locus.indexAtValidatedOrdinal(2)), SpatialScatterCell.Unvisited)
    val graph = right(VolumeSupportGraphs.volume(axis,
      Vector((0, 0, 0), (1, 0, 0), (3, 0, 0)), Vector(2.0, 3.0, 4.0), "mm", "external-grid"))
    assertEquals(graph.edges.map(e => (e.left, e.right)), Vector((0, 1)))
