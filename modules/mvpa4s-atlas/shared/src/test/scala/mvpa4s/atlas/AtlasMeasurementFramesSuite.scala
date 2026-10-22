package mvpa4s.atlas

import scala.compiletime.testing.typeCheckErrors
import gale.linalg.DMat
import locus4s.{Bijection, DomainRecord, DomainRegistry, PartialSurjection, Selection, TotalMap}
import locus4s.data.{Field, VectorField}
import locus4s.provenance.{Provenance, ProvenanceId, SourceId}
import multivar.core.{SemanticSpace, SpaceRole, ValueId, ValueIdentity}
import mvpa4s.{AxisRef, EvidenceSource, Observations}
import mvpa4s.locus.*
import mvpa4s.measurement.*
import neuroatlas4s.*

class AtlasMeasurementFramesSuite extends munit.FunSuite:
  private def right[E, A](value: Either[E, A]): A = value.fold(error => fail(error.toString), identity)
  private val declaration = MeasurementFrameDeclaration("atlas-fixture", "v1", Vector.empty)
  private val sourceKeys = Vector("x3", "x0", "x5", "x2", "x1", "x4")
  private def axis = right(AxisRef.fromStableKeys("atlas-source", SpaceRole.Observed, sourceKeys, "sample-order", "BOLD", "raw"))

  private def atlas(keys: Vector[String] = Vector("parcel:A", "parcel:B"), labels: Vector[String] = Vector("A", "B"),
      members: Vector[Option[Int]] = Vector(Some(0), None, Some(1), Some(0), None, Some(1)),
      display: Vector[Int] = Vector(1, 0)): AtlasRealization =
    val ambient = right(DomainRegistry.empty.restore(right(DomainRecord.parse("atlas:test:samples", "atlas samples", 6))))
    val parcels = right(ambient.registry.restore(right(DomainRecord.parse("atlas:test:parcels", "atlas parcels", 2))))
    val assignment = right(PartialSurjection.fromOptionalTargetOrdinals(ambient.space, parcels.space, members))
    val keyField = right(VectorField.fromValues(parcels.space, keys))
    val regions = RegionIndex(Vector(AtlasRegionMetadata(RegionId(7), labels(0)), AtlasRegionMetadata(RegionId(9), labels(1))))
    val metadataField = right(VectorField.fromValues(parcels.space, regions.regions))
    val order = right(Selection.fromOrdinals(parcels.space, display))
    val atlasRef = AtlasRef.volume("toy", "MVPAContract", SpaceId.Custom, SpaceId.MNI152, confidence = Confidence.Exact)
    new AtlasRealization:
      type X = ambient.S
      type P = parcels.S
      val registry = parcels.registry
      val ref = atlasRef
      val provenance = AtlasProvenance.fromRef(atlasRef, regions)
      val parcelAssignment = assignment
      val parcelKeys = keyField
      val metadata = metadataField
      val displayOrder = order
      val networkAssignment = None

  private def correspondence(source: AxisRef[String])(realization: AtlasRealization): Bijection[realization.X, source.Locus] =
    // These explicit stable keys establish meaning; bijection certification alone proves cardinality/invertibility.
    val ordinals = Vector.tabulate(6)(i => sourceKeys.indexOf(s"x$i"))
    right(Bijection.fromTotalMap(right(TotalMap.fromTargetOrdinals(realization.parcelAssignment.from, source.locus, ordinals))))

  test("permuted atlas parcels measure literal values and scatter back with background unvisited"):
    val source = axis
    val realization = atlas()
    val samples = correspondence(source)(realization)
    val adapted = right(AtlasMeasurementFrames.parcels(source, declaration)(realization)(samples))
    val subjects = right(AxisRef.fromStableKeys("atlas-subjects", SpaceRole.Samples, Vector("s1", "s2"), "subject", "BOLD", "raw"))
    val id = SourceId.unsafe("atlas-evidence")
    val evidence = right(EvidenceSource(id, Provenance.source(ProvenanceId.unsafe("atlas-root"), id)))
    val data = right(Observations.fromDense(subjects, source,
      DMat.dense(2, 6, Vector(40.0, 10.0, 60.0, 30.0, 20.0, 50.0, 400.0, 100.0, 600.0, 300.0, 200.0, 500.0)),
      ValueIdentity.source(ValueId.unsafe("atlas-values")), evidence))
    val visitor = new MeasurementVisitor[subjects.Id, source.Id, String, SpatialMeasurementRendition[source.Locus], DMat]:
      def visit[L <: SemanticSpace](entry: PackedMeasurementEntry[source.Id, String, SpatialMeasurementRendition[source.Locus]] { type Local = L }, measured: MeasuredObservations[subjects.Id, L]): Either[MeasurementFailure, DMat] =
        Right(right(measured.patterns(DMat.eye(measured.localAxis.size))))
    val result = adapted.frame.traverse(1)(Right(MeasurementResource(data)(())))(visitor)
    assertEquals(result.error, None)
    assertEquals(result.value.map(_.descriptor.id.value), Vector("parcel:A", "parcel:B"))
    assertEquals(adapted.displayOrder.map(_.value), Vector("parcel:B", "parcel:A"))
    assertEquals(result.value.map(_.rendition.support.ordinalsInDomainOrder.toVector), Vector(Vector(0, 1), Vector(2, 3)))
    val expected = Vector(Vector(40.0, 10.0, 400.0, 100.0), Vector(60.0, 30.0, 600.0, 300.0))
    result.value.zip(expected).foreach: (outcome, values) =>
      val actual = outcome.value.toOption.get
      for row <- 0 until 2; column <- 0 until 2 do
        assertEqualsDouble(actual(row, column), values(row * 2 + column), 0.0)
    val outcomes = result.value.zip(Vector(10.0, 20.0)).map: (outcome, value) =>
      SpatialLocalOutcome(outcome.descriptor.id, outcome.rendition.support, 1.0, Right(value))
    val onSource = right(SpatialMeasurementScatter.scatter(source.locus, outcomes, SpatialScatterAlgebra.weightedMeanDouble))
    val onAtlas = onSource.pullback(samples.toTotalMap)
    def numeric(cell: SpatialScatterCell[Double]): Option[Double] = cell match
      case SpatialScatterCell.Unvisited => None
      case SpatialScatterCell.Aggregated(value, _, _, failures) =>
        assertEquals(failures, Vector.empty)
        Some(value)
      case SpatialScatterCell.LocalFailure(_) => fail("unexpected local failure")
    assertEquals(onSource.toVector.map(numeric), Vector(Some(10.0), Some(10.0), Some(20.0), Some(20.0), None, None))
    assertEquals(onAtlas.toVector.map(numeric), Vector(Some(10.0), None, Some(20.0), Some(10.0), None, Some(20.0)))
    assert(onAtlas.space.sameRuntimeOwnerAs(realization.parcelAssignment.from))

  test("labels and display order are presentation; exact membership remains scientific identity"):
    val source = axis
    val base = atlas()
    val renamed = atlas(labels = Vector("unknown", "a different label"), display = Vector(0, 1))
    val changed = atlas(members = Vector(Some(1), None, Some(0), Some(0), None, Some(1)))
    val first = right(AtlasMeasurementFrames.parcels(source, declaration)(base)(correspondence(source)(base)))
    val second = right(AtlasMeasurementFrames.parcels(source, declaration)(renamed)(correspondence(source)(renamed)))
    val third = right(AtlasMeasurementFrames.parcels(source, declaration)(changed)(correspondence(source)(changed)))
    assertEquals(first.frame.identity, second.frame.identity)
    assertNotEquals(first.displayOrder, second.displayOrder)
    assertNotEquals(first.frame.identity, third.frame.identity)

  test("invalid and duplicate complete parcel keys are refused without repair"):
    val source = axis
    for key <- Vector("", " leading", "control\n") do
      val realization = atlas(keys = Vector(key, "parcel:B"))
      val actual = AtlasMeasurementFrames.parcels(source, declaration)(realization)(correspondence(source)(realization))
      assertEquals(actual.left.toOption, Some(AtlasMeasurementFrameError.InvalidParcelKey(key, MeasurementError.InvalidMeasurementId(key))))
    val duplicate = atlas(keys = Vector("parcel:A", "parcel:A"))
    val result = AtlasMeasurementFrames.parcels(source, declaration)(duplicate)(correspondence(source)(duplicate))
    assertEquals(result.left.toOption, Some(AtlasMeasurementFrameError.Spatial(SpatialMeasurementFrameError.DuplicateId(MeasurementId.unsafe("parcel:A")))))

  test("foreign live correspondence endpoints are refused even when descriptors and sizes match"):
    val source = axis
    val foreignSource = axis
    val realization = atlas()
    val foreignAtlas = atlas()
    val wrongFrom = correspondence(source)(foreignAtlas).asInstanceOf[Bijection[realization.X, source.Locus]]
    val wrongTo = correspondence(foreignSource)(realization).asInstanceOf[Bijection[realization.X, source.Locus]]
    for (mapping, context) <- Vector(wrongFrom -> "atlas sample correspondence", wrongTo -> "source sample correspondence") do
      val result = AtlasMeasurementFrames.parcels(source, declaration)(realization)(mapping)
      assert(result.left.toOption.exists:
        case AtlasMeasurementFrameError.WrongRuntimeOwner(actual, _, _) => actual == context
        case _ => false
      )

  test("foreign parcel fields and display domains are rejected before reading values"):
    val source = axis
    val base = atlas()
    val foreign = atlas()
    for malformed <- Vector("keys", "metadata", "display") do
      val poisoned = new AtlasRealization:
        type X = base.X
        type P = base.P
        val registry = base.registry
        val ref = base.ref
        val provenance = base.provenance
        val parcelAssignment = base.parcelAssignment
        val parcelKeys =
          if malformed == "keys" then
            Field.view(foreign.parcelDomain)(_ => throw new AssertionError("foreign keys read")).asInstanceOf[Field[P, String]]
          else base.parcelKeys
        val metadata =
          if malformed == "metadata" then
            Field.view(foreign.parcelDomain)(_ => throw new AssertionError("foreign metadata read")).asInstanceOf[Field[P, AtlasRegionMetadata]]
          else base.metadata
        val displayOrder = if malformed == "display" then foreign.displayOrder.asInstanceOf[Selection[P]] else base.displayOrder
        val networkAssignment = None
      val result = AtlasMeasurementFrames.parcels(source, declaration)(poisoned)(correspondence(source)(poisoned))
      assert(result.left.toOption.exists:
        case AtlasMeasurementFrameError.WrongRuntimeOwner(_, _, _) => true
        case _ => false
      )

  test("correspondence requires a bijection in the declared direction"):
    val source = axis
    val realization = atlas()
    val manyToOne = right(TotalMap.fromTargetOrdinals(realization.parcelAssignment.from, source.locus, Vector(0, 0, 2, 3, 4, 5)))
    assert(Bijection.fromTotalMap(manyToOne).isLeft)
    val errors = typeCheckErrors("""import locus4s.Bijection
import mvpa4s.AxisRef
import mvpa4s.atlas.AtlasMeasurementFrames
import mvpa4s.measurement.MeasurementFrameDeclaration
import neuroatlas4s.AtlasRealization
def invalid[K](source: AxisRef[K], declaration: MeasurementFrameDeclaration)(atlas: AtlasRealization)(reversed: Bijection[source.Locus, atlas.X]) =
  AtlasMeasurementFrames.parcels(source, declaration)(atlas)(reversed)
""")
    assertEquals(errors.length, 1)
    assert(errors.head.lineContent.contains("AtlasMeasurementFrames.parcels"))
