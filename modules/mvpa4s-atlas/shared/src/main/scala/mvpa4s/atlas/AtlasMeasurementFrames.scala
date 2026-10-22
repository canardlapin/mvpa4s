package mvpa4s.atlas

import locus4s.Bijection
import multivar.core.SemanticSpace
import mvpa4s.AxisRef
import mvpa4s.locus.*
import mvpa4s.measurement.{MeasurementError, MeasurementFrame, MeasurementFrameDeclaration, MeasurementId}
import neuroatlas4s.AtlasRealization

/** Scientific traversal is ID-sorted. Atlas display order is separate presentation metadata. */
final case class AtlasMeasurementFrame[N <: SemanticSpace, K, S](
    frame: MeasurementFrame[N, K, SpatialMeasurementRendition[S]],
    displayOrder: Vector[MeasurementId]
)

enum AtlasMeasurementFrameError:
  case WrongRuntimeOwner(context: String, expected: String, actual: String)
  case InvalidParcelKey(key: String, cause: MeasurementError)
  case Spatial(cause: SpatialMeasurementFrameError)

  def message: String = this match
    case WrongRuntimeOwner(context, expected, actual) => s"$context belongs to $actual, expected live domain owner $expected"
    case InvalidParcelKey(key, cause) => s"atlas parcel '$key': ${cause.message}"
    case Spatial(cause) => cause.message

object AtlasMeasurementFrames:
  /** Adapt an admitted atlas assignment without inferring correspondence from size or order.
    * `samples` maps atlas samples to the source axis, including unassigned/background samples.
    * Only assigned parcel fibers become measurements. Scatter uses the existing locus adapter;
    * pulling its field back through `samples.toTotalMap` restores atlas sample order.
    */
  def parcels[K](source: AxisRef[K], declaration: MeasurementFrameDeclaration)(atlas: AtlasRealization)(
      samples: Bijection[atlas.X, source.Locus]
  ): Either[AtlasMeasurementFrameError, AtlasMeasurementFrame[source.Id, K, source.Locus]] =
    val owners = Vector(
      ("atlas sample correspondence", atlas.parcelAssignment.from, samples.from),
      ("source sample correspondence", source.locus, samples.to),
      ("atlas parcel keys", atlas.parcelDomain, atlas.parcelKeys.space),
      ("atlas parcel metadata", atlas.parcelDomain, atlas.metadata.space),
      ("atlas display order", atlas.parcelDomain, atlas.displayOrder.space)
    )
    owners.find((_, expected, actual) => !expected.sameRuntimeOwnerAs(actual)) match
      case Some((context, expected, actual)) =>
        Left(AtlasMeasurementFrameError.WrongRuntimeOwner(context, expected.toString, actual.toString))
      case None =>
        val sites = Vector.newBuilder[SpatialMeasurementSite[source.Locus]]
        val ids = Vector.newBuilder[MeasurementId]
        val parcels = atlas.parcelDomain.indices
        while parcels.hasNext do
          val parcel = parcels.next()
          val key = atlas.parcelKeys(parcel)
          MeasurementId(key) match
            case Left(error) => return Left(AtlasMeasurementFrameError.InvalidParcelKey(key, error))
            case Right(id) =>
              ids += id
              sites += SpatialMeasurementSite(
                id,
                SpatialMeasurementSupport.Regional(samples.toTotalMap.image(atlas.parcelAssignment.fiber(parcel))),
                None,
                Some(atlas.metadata(parcel).label)
              )
        val parcelIds = ids.result()
        SpatialMeasurementFrames.regions(source, declaration, sites.result())
          .left.map(AtlasMeasurementFrameError.Spatial.apply)
          .map(frame => AtlasMeasurementFrame(frame, atlas.displayOrder.indices.map(p => parcelIds(p.ordinal)).toVector))
