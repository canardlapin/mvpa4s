package mvpa4s.testkit

import mvpa4s.AxisSignature
import mvpa4s.analysis.{CapabilityId, EstimandId, PlanId}

/** Fixture identities for downstream test suites. TEST SCOPE ONLY.
  *
  * Production code obtains signatures from identified axes and plan ids from
  * compiled analyses. These constructors mint identities from declared
  * components without that provenance, so they are named `unsafe` and this
  * artifact must never appear on a Compile classpath.
  */
object MvpaFixtures:
  /** A well-formed signature (64 lowercase hexadecimal digits). Throws otherwise. */
  def unsafeSignature(value: String): AxisSignature =
    AxisSignature.unsafe(value)

  /** The plan id the compiler would derive from these declared components. */
  def unsafePlanId(
      estimand: EstimandId,
      sourceAxes: Vector[AxisSignature],
      designAxis: AxisSignature,
      frameAxis: AxisSignature,
      sourceIdentity: String,
      designIdentity: String,
      frameIdentity: String,
      question: String,
      assumptions: Vector[String],
      preparation: Vector[String],
      reduction: String,
      estimandParameters: Vector[(String, String)],
      requiredCapabilities: Set[CapabilityId]
  ): PlanId =
    PlanId.derived(
      estimand,
      sourceAxes,
      designAxis,
      frameAxis,
      sourceIdentity,
      designIdentity,
      frameIdentity,
      question,
      assumptions,
      preparation,
      reduction,
      estimandParameters,
      requiredCapabilities
    )
