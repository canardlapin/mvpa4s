package mvpa4s.locus

import locus4s.{CenteredNeighborhoodSystem, Relation, Selection}
import multivar.core.SpaceRole
import mvpa4s.AxisRef
import mvpa4s.measurement.MeasurementFrameDeclaration

/** Literals computed independently with Python hashlib and little-endian
  * length framing, before extraction from ScalaFIM 694c711c9. */
class SpatialIdentityGoldenSuite extends munit.FunSuite:
  private def right[A](value: Either[?, A]): A = value.fold(e => fail(e.toString), identity)

  test("persisted ambient IDs, support manifests and frame fingerprint survive extraction"):
    val axis = right(AxisRef.fromStableKeys("padded-volume", SpaceRole.Observed,
      Vector.tabulate(12)(i => s"v$i"), "voxel-order", "psc", "raw"))
    val centers = right(Selection.fromOrdinals(axis.locus, Vector(10, 2)))
    val rows = right(Relation.fromOrdinalRows(centers.positions, axis.locus, Vector(Vector(9, 10), Vector(0, 2))))
    val neighborhoods = right(CenteredNeighborhoodSystem.fromSelection(centers, rows))
    val caller = MeasurementFrameDeclaration("spatial-fixture", "v1", Vector("radius" -> "1.5"))
    val frame = right(SpatialMeasurementFrames.volumeNeighborhoods(axis, caller, neighborhoods))
    assertEquals(axis.descriptor.coordinateSignature.value, "fbf27b6ab264f01ae87912eaed4727be135e05581e07f73317a90334e52b6362")
    assertEquals(caller.fingerprint, "79d44c13d3dfbb797d3063d0162901cf7cdf966c3378dc6f266682bbad3409f6")
    assertEquals(frame.identity.declaration.parameters, Vector(
      "caller-declaration" -> "79d44c13d3dfbb797d3063d0162901cf7cdf966c3378dc6f266682bbad3409f6",
      "spatial-support" -> "74:volume:fbf27b6ab264f01ae87912eaed4727be135e05581e07f73317a90334e52b6362:02:2:0:2",
      "spatial-support" -> "74:volume:fbf27b6ab264f01ae87912eaed4727be135e05581e07f73317a90334e52b6362:10:2:9:10"
    ))
    assertEquals(frame.identity.declaration.fingerprint, "4de491ccb15bced087a2aa677a3f7a66453a110b845640a849b3c3a5f9acf683")
