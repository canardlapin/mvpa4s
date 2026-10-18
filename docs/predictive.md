# mvpa4s-predictive

Optional [Alder](https://github.com/canardlapin/alder) adapters for identified
mvpa4s evidence: training-only preparation, held-out prediction, validation
and tuning. The core does not depend on this module or on Alder.

```scala
import mvpa4s.predictive.*
```

## Admission

Predictive workflows use identified `Observations`, `MultiResponse` and
`Column` metadata. `AlderPredictiveAdmission.nativeTables` admits the native
values with their source identities, axes, read policy and materialization
budget; `.nativeMeasurement` additionally binds a typed spatial/basis leg.
`.materialized` binds caller-owned matrices. A `ValidationDesign` or
`LeaveOneGroupOutDesign` binds assessment to the same ordered sample axis.
Native IDs remain unique, including repeated-draw occurrences. Equal-sized
foreign axes are refused.

A `NativeAxisMapping` declares the native id of every ordinal on a sample
axis. Adapters that keep their own learner admit rows through
`mapping.identifiedRows`, which binds one example per native id in ordinal
order under the mapping's declared identity.

## Adapters

The method-owned adapters are `AlderSwiftCentroid`, `AlderCorrelationCentroid`,
`AlderRidgeLda`, `AlderRidgeRegression`, `AlderFeatureModel`,
`AlderCrossDecoding`, `AlderOperatorRidge` and `AlderPatternSelection`. Each
completes an actual Alder fit on explicitly selected training rows and returns
its named result with fit and validation receipts. Dense categorical and
feature-model heads accept repeated exact validation; each categorical row
records every contributing assessment unit and its training keys, with equal
weight per contribution. Operator ridge retains operator products and never
requests a dense trial-by-feature table.

Feature-model designs require ordered item keys matching the sample axis and
explicit neural-column names. Both directions retain training-only sample-SD
standardization, coefficients, repeated exact assessment means, metrics and the
optional prediction payload. Cross-domain heads require separate sample domains
and an exact feature correspondence; native read receipts are checked against
that correspondence. A materialized feature axis and source-only preparation
remain caller declarations. Probability normalization supplies no calibration
claim.

`AlderExposureReads` is the exposure-aware entry point for native tables. It
checks the read against an `ExposurePermit` before calling the operator, refuses
training-only requests (Alder has no constrained-row capability), and records
every approved read as whole-population exposure.

Adapters over kernels that live elsewhere stay with those kernels. ScalaFIM's
soft-LDA lifecycle, for example, composes these admission and validation
types with its fMRI single-fit kernel.

## Identity

Fingerprint, schema and component strings (for example
`scalafim.native-declared-map.v1`) are persisted identities and are unchanged
from the code's origin in ScalaFIM (`04065f5`).

## Tests

R parity expectations live in `NativePredictiveParitySuite`; numerical laws
and refusal cases live in `NativePredictiveLawsSuite` and the owning method
suites. All suites are shared and run on the JVM and Scala.js:

```sh
sbt predictiveJVM/test predictiveJS/test
```
