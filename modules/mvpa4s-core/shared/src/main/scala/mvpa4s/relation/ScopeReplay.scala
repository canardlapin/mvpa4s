package mvpa4s.relation

import multivar.core.Table
import mvpa4s.{AxisRef, EvidenceError}

/** Permission for stable forward and adjoint applications during one acquired
  * source scope. Contains only immutable declaration metadata and expiration
  * state, never a reader, operator or resource callback. Providers must also
  * guard every application against resource closure.
  */
final class ScopeReplay private[mvpa4s] (
    val owner: String, val revision: String, val sourceBinding: String
):
  require(owner.trim.nonEmpty && revision.trim.nonEmpty && sourceBinding.trim.nonEmpty)
  private var active = true
  def isActive: Boolean = synchronized(active)
  private[mvpa4s] def expire(): Unit = synchronized:
    active = false

  // Dependency identity is immutable even after expiration. Separate scopes
  // with the same declaration still have independent lifetime state.
  override def equals(other: Any): Boolean = other match
    case that: ScopeReplay => owner == that.owner && revision == that.revision && sourceBinding == that.sourceBinding
    case _ => false
  override def hashCode(): Int = (owner, revision, sourceBinding).hashCode

object ScopeReplay:
  /** Mint a scope for the provider that acquired the source. Acquisition is not
    * authenticated; what is guaranteed is that only the returned lease can
    * attach its live witness to relations or expire it, while observers of a
    * relation see the inert `ScopeReplay` alone.
    */
  def acquire(owner: String, revision: String, sourceBinding: String): Either[EvidenceError, ScopeLease] =
    if owner.trim.isEmpty || revision.trim.isEmpty || sourceBinding.trim.isEmpty then
      Left(EvidenceError.InvalidSource("scoped replay owner, revision and source binding must be non-empty"))
    else Right(new ScopeLease(new ScopeReplay(owner, revision, sourceBinding)))

/** The acquiring provider's capability over one scoped replay. */
final class ScopeLease private[relation] (val replay: ScopeReplay):
  def access: RelationAccess = RelationAccess.ScopedReplay(replay)

  /** Expire the witness when the acquired source scope closes. Idempotent. */
  def expire(): Unit = replay.expire()

  /** Attach this lease's live witness. A witness minted by another lease, even
    * with an equal declaration, cannot be transplanted.
    */
  def attach[EK, NK](
      effects: AxisRef[EK],
      neural: AxisRef[NK],
      estimate: Table[effects.Id, neural.Id],
      origins: RelationOrigins,
      estimability: Vector[EffectEstimability]
  ): Either[EvidenceError, Relation[effects.Id, neural.Id]] =
    origins.access match
      case RelationAccess.ScopedReplay(witness) if witness eq replay =>
        Relation.fromAcquired(effects, neural, estimate, origins, estimability)
      case RelationAccess.ScopedReplay(_) =>
        Left(EvidenceError.InvalidSource("scoped replay witness belongs to another lease"))
      case _ =>
        Left(EvidenceError.InvalidSource("lease attachment requires this lease's scoped replay access"))
