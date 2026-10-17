package mvpa4s.relation

import gale.linalg.DMat
import munit.FunSuite
import multivar.core.{CoordinateEvidence, Lin, SemanticProvenance, SpaceRole, ValueId, ValueIdentity}
import mvpa4s.{AxisRef, EvidenceError}

/** The acquiring-provider capability, exercised from outside the `mvpa4s`
  * package's private surface: only the minting lease attaches or expires its
  * witness, and expiry is observed by every relation that carries it.
  */
class ScopeLeaseSuite extends FunSuite:
  private def right[A](value: Either[EvidenceError, A]): A =
    value.fold(error => fail(error.message), scala.Predef.identity)

  private def axis(name: String, role: SpaceRole, size: Int): AxisRef[String] =
    right(AxisRef.fromStableKeys(name, role, Vector.tabulate(size)(i => s"$name-$i"), "fixture", "unit", "raw"))

  private val effects = axis("effects", SpaceRole.Latent, 2)
  private val neural = axis("neural", SpaceRole.Observed, 1)
  private val table =
    Lin
      .fromDenseMatrix(
        DMat.dense(2, 1, Vector(1.0, 2.0)),
        CoordinateEvidence.dual(neural.evidence),
        CoordinateEvidence.primal(effects.evidence),
        ValueIdentity.source(ValueId.unsafe("lease-readout")),
        SemanticProvenance.source("scope-lease-fixture")
      )
      .fold(error => fail(error.message), scala.Predef.identity)
  private val source = RelationSource("run-1", "r", "readout", "none", "noise")
  private val estimable = Vector.fill(effects.size)(EffectEstimability.Estimable)

  test("a lease attaches its own witness and the relation observes expiry"):
    val lease = right(ScopeReplay.acquire("provider", "revision", "binding"))
    val relation = right(lease.attach(effects, neural, table, RelationOrigins(source, lease.access), estimable))
    assert(lease.replay.isActive)
    lease.expire()
    lease.expire()
    relation.origins.access match
      case RelationAccess.ScopedReplay(witness) => assert(!witness.isActive)
      case other => fail(other.toString)
    assert(lease.attach(effects, neural, table, RelationOrigins(source, lease.access), estimable).isLeft)

  test("a witness from an equal declaration cannot be transplanted onto another lease"):
    val first = right(ScopeReplay.acquire("provider", "revision", "binding"))
    val second = right(ScopeReplay.acquire("provider", "revision", "binding"))
    assertEquals(first.replay, second.replay, "declarations are equal")
    assert(first.attach(effects, neural, table, RelationOrigins(source, second.access), estimable).isLeft)
    assert(first.attach(effects, neural, table, RelationOrigins(source, RelationAccess.OneShot), estimable).isLeft)

  test("public relation construction refuses a scoped witness and blank scope declarations"):
    val lease = right(ScopeReplay.acquire("provider", "revision", "binding"))
    assert(Relation(effects, neural, table, RelationOrigins(source, lease.access), estimable).isLeft)
    assert(ScopeReplay.acquire(" ", "revision", "binding").isLeft)
