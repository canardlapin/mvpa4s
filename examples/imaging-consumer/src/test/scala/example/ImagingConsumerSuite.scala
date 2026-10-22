package example

import gale.linalg.DMat
import locus4s.{Bijection, DomainRecord, DomainRegistry, PartialSurjection, Selection, TotalMap}
import locus4s.data.VectorField
import locus4s.provenance.{Provenance, ProvenanceId, SourceId}
import multivar.core.{SemanticSpace, SpaceRole, ValueId, ValueIdentity}
import mvpa4s.{AxisRef, EvidenceSource, Observations}
import mvpa4s.atlas.AtlasMeasurementFrames
import mvpa4s.locus.{SpatialMeasurementFrames, SpatialMeasurementRendition, SupportWeightPolicy}
import mvpa4s.measurement.*
import mvpa4s.surface.SurfaceSupportGraphs
import neuroatlas4s.*
import neurosurf4s.surface.{TriangleMesh, VertexId}

/** Independent package using public compile-scope providers only, without ScalaFIM or testkits. */
class ImagingConsumerSuite extends munit.FunSuite:
  private def right[E, A](value: Either[E, A]): A = value.fold(error => fail(error.toString), identity)

  test("a public surface graph preserves the explicitly selected vertex order and physical units"):
    val source = right(AxisRef.fromStableKeys("selected-vertices", SpaceRole.Observed, Vector("v1", "v0"), "vertex-order", "BOLD", "raw"))
    val mesh = TriangleMesh.fromRows(Vector(Vector(0.0, 0.0, 0.0), Vector(2.0, 0.0, 0.0), Vector(0.0, 3.0, 0.0)), Vector((0, 1, 2)))
    val graph = right(SurfaceSupportGraphs.surface(source, mesh, Vector(VertexId(1), VertexId(0)), "mm", SupportWeightPolicy.InversePhysicalLength))
    assertEquals(graph.edges.size, 1)
    assertEquals((graph.edges.head.left, graph.edges.head.right), (0, 1))
    assertEqualsDouble(graph.edges.head.weight, 0.5, 1e-12)
    assertEquals(graph.weightUnits, "1/mm")

  test("public atlas and surface measurements share evidence without sharing domain owners implicitly"):
    val source = right(AxisRef.fromStableKeys("permuted-samples", SpaceRole.Observed, Vector("v2", "v0", "v1"), "sample-order", "BOLD", "raw"))
    val ambient = right(DomainRegistry.empty.restore(right(DomainRecord.parse("consumer:atlas:samples", "atlas samples", 3))))
    val parcels = right(ambient.registry.restore(right(DomainRecord.parse("consumer:atlas:parcels", "atlas parcels", 2))))
    val assignment = right(PartialSurjection.fromOptionalTargetOrdinals(ambient.space, parcels.space, Vector(Some(0), None, Some(1))))
    val keys = right(VectorField.fromValues(parcels.space, Vector("A", "B")))
    val regions = RegionIndex(Vector(AtlasRegionMetadata(RegionId(1), "left"), AtlasRegionMetadata(RegionId(2), "right")))
    val metadataField = right(VectorField.fromValues(parcels.space, regions.regions))
    val order = right(Selection.fromOrdinals(parcels.space, Vector(1, 0)))
    val atlasRef = AtlasRef.volume("toy", "PublicConsumer", SpaceId.Custom, SpaceId.MNI152)
    val atlas = new AtlasRealization:
      type X = ambient.S
      type P = parcels.S
      val registry = parcels.registry
      val ref = atlasRef
      val provenance = AtlasProvenance.fromRef(atlasRef, regions)
      val parcelAssignment = assignment
      val parcelKeys = keys
      val metadata = metadataField
      val displayOrder = order
      val networkAssignment = None
    val mapping = right(Bijection.fromTotalMap(right(TotalMap.fromTargetOrdinals(ambient.space, source.locus, Vector(1, 2, 0)))))
    val declaration = MeasurementFrameDeclaration("public-atlas", "v1", Vector.empty)
    val adapted = right(AtlasMeasurementFrames.parcels(source, declaration)(atlas)(mapping))
    val subjects = right(AxisRef.fromStableKeys("subjects", SpaceRole.Samples, Vector("s1"), "subject-order", "BOLD", "raw"))
    val id = SourceId.unsafe("consumer-evidence")
    val evidence = right(EvidenceSource(id, Provenance.source(ProvenanceId.unsafe("consumer-root"), id)))
    val observations = right(Observations.fromDense(subjects, source, DMat.dense(1, 3, Vector(30.0, 10.0, 20.0)), ValueIdentity.source(ValueId.unsafe("consumer-values")), evidence))
    val visitor = new MeasurementVisitor[subjects.Id, source.Id, String, SpatialMeasurementRendition[source.Locus], Double]:
      def visit[L <: SemanticSpace](entry: PackedMeasurementEntry[source.Id, String, SpatialMeasurementRendition[source.Locus]] { type Local = L }, measured: MeasuredObservations[subjects.Id, L]): Either[MeasurementFailure, Double] =
        Right(right(measured.patterns(DMat.eye(measured.localAxis.size)))(0, 0))
    val result = adapted.frame.traverse(1)(Right(MeasurementResource(observations)(())))(visitor)
    assertEquals(result.error, None)
    assertEquals(result.value.map(_.value), Vector(Right(10.0), Right(30.0)))
    assertEquals(adapted.displayOrder.map(_.value), Vector("B", "A"))
    // The same supports also compose as centered surface neighborhoods through locus alone.
    val centers = right(Selection.fromOrdinals(source.locus, Vector(1, 0)))
    val rows = right(locus4s.Relation.fromOrdinalRows(centers.positions, source.locus, Vector(Vector(1), Vector(0))))
    val neighborhoods = right(locus4s.CenteredNeighborhoodSystem.fromSelection(centers, rows))
    val surface = right(SpatialMeasurementFrames.surfacePatches(source, declaration, neighborhoods))
    val surfaceResult = surface.traverse(1)(Right(MeasurementResource(observations)(())))(visitor)
    assertEquals(surfaceResult.error, None)
    assertEquals(surfaceResult.value.map(_.value), Vector(Right(30.0), Right(10.0)))
