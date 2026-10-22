package mvpa4s.locus

import locus4s.{CenteredNeighborhoodSystem, Relation, Selection}
import multivar.core.SpaceRole
import mvpa4s.AxisRef
import mvpa4s.measurement.MeasurementFrameDeclaration

class SurfaceMeasurementFramesSuite extends munit.FunSuite:
  private def right[E, A](value: Either[E, A]): A = value.fold(error => fail(error.toString), identity)
  private def axis(name: String): AxisRef[String] =
    right(AxisRef.fromStableKeys(name, SpaceRole.Observed, Vector("a", "b", "c", "d"), "voxel-order", "psc", "raw"))
  private val declaration = MeasurementFrameDeclaration("spatial-fixture", "v1", Vector("radius" -> "1.5"))

  test("empty centered surface patches are an explicit frame error"):
    val neural = axis("spatial-empty-surface")
    val centers = right(Selection.fromOrdinals(neural.locus, Vector.empty))
    val centered = right(CenteredNeighborhoodSystem.fromSelection(centers, Relation.empty(centers.positions, neural.locus)))
    val result = SpatialMeasurementFrames.surfacePatches(neural, declaration, centered)
    assertEquals(result.left.toOption, Some(SpatialMeasurementFrameError.EmptyFrame))

  test("surface adapter preserves frozen padded ambient IDs and frame fingerprint"):
    val neural = right(AxisRef.fromStableKeys("padded-volume", SpaceRole.Observed,
      Vector.tabulate(12)(i => s"v$i"), "voxel-order", "psc", "raw"))
    val centers = right(Selection.fromOrdinals(neural.locus, Vector(10, 2)))
    val rows = Vector(Vector(9, 10), Vector(0, 2))
    val relation = right(Relation.fromOrdinalRows(centers.positions, neural.locus, rows))
    val patches = right(CenteredNeighborhoodSystem.fromSelection(centers, relation))
    val frame = right(SpatialMeasurementFrames.surfacePatches(neural, declaration, patches))
    // tools/measurement-identity-reference.py in mvpa4s calculates these
    // literals using Python hashlib and the pre-extraction wire grammar.
    assertEquals(frame.identity.declaration.parameters, Vector(
      "caller-declaration" -> "79d44c13d3dfbb797d3063d0162901cf7cdf966c3378dc6f266682bbad3409f6",
      "spatial-support" -> "75:surface:fbf27b6ab264f01ae87912eaed4727be135e05581e07f73317a90334e52b6362:02:2:0:2",
      "spatial-support" -> "75:surface:fbf27b6ab264f01ae87912eaed4727be135e05581e07f73317a90334e52b6362:10:2:9:10"
    ))
    assertEquals(frame.identity.declaration.fingerprint, "3eac9fb3bedd5f6a1048b4d62b94d881f95e2834486ee60435b9dd822cb1bf81")

  test("surface neighborhoods reject a forged same-sized foreign live owner"):
    val source = axis("same-surface-owner")
    val foreign = axis("same-surface-owner")
    val centers = right(Selection.fromOrdinals(foreign.locus, Vector(2)))
    val rows = right(Relation.fromOrdinalRows(centers.positions, foreign.locus, Vector(Vector(0, 2))))
    val neighborhoods = right(CenteredNeighborhoodSystem.fromSelection(centers, rows))
      .asInstanceOf[CenteredNeighborhoodSystem[centers.I, source.Locus]]
    val actual = SpatialMeasurementFrames.surfacePatches(source, declaration, neighborhoods)
    assert(actual.left.toOption.exists:
      case SpatialMeasurementFrameError.WrongRuntimeOwner("surface patch centers", _, _) => true
      case _ => false
    )
