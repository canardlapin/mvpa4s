package mvpa4s.locus

import multivar.core.SpaceRole
import munit.FunSuite
import mvpa4s.AxisRef
import mvpa4s.pattern.SupportTopology

class VolumeSupportGraphsSuite extends FunSuite:
  private def right[A](value: Either[?, A]): A = value.fold(e => fail(e.toString), identity)
  private def axis(n: Int) = right(AxisRef.fromStableKeys("anatomy", SpaceRole.Observed,
    Vector.tabulate(n)(i => s"v$i"), "vertex", "BOLD", "native"))

  test("volume uses face adjacency only and preserves anisotropic physical weights"):
    val coordinates = Vector((0, 0, 0), (1, 0, 0), (0, 1, 0), (0, 0, 1), (2, 2, 0))
    val graph = right(VolumeSupportGraphs.volume(axis(5), coordinates, Vector(2.0, 3.0, 4.0), "mm", "grid", SupportWeightPolicy.InversePhysicalLength))
    assertEquals(graph.edges.map(e => (e.left, e.right)), Vector((0, 1), (0, 2), (0, 3)))
    graph.edges.zip(Vector(0.5, 1.0 / 3.0, 0.25)).foreach((e, expected) => assertEqualsDouble(e.weight, expected, 1e-12))
    assert(VolumeSupportGraphs.volume(axis(5), coordinates, Vector(0.0, 3.0, 4.0), "mm", "grid").isLeft)

  test("anatomical adapters reject duplicate mappings"):
    assert(VolumeSupportGraphs.volume(axis(2), Vector((0, 0, 0), (0, 0, 0)), Vector(1.0, 1.0, 1.0), "mm", "grid").isLeft)
