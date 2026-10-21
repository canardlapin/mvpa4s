# mvpa4s

Identified multivariate evidence for Scala 3 on the JVM and Scala.js: scientific
axes and repeated draws, observations with declared lineage and support,
relational and pattern analyses, measurement composition, and reusable
finite-family execution contracts.

mvpa4s builds on standalone providers and adds the MVPA-specific invariants they
do not own:

| Concern | Owner |
| --- | --- |
| Finite domains, alignment, source lineage | [locus4s](https://github.com/canardlapin/locus4s) core and `locus4s-provenance` |
| Typed reindexing, resampling designs | [Resample4s](https://github.com/canardlapin/resample4s) |
| Semantic multivariate algebra and inference | [Multivar](https://github.com/canardlapin/multivar) |
| Dense/sparse matrices and solvers | [Gale](https://github.com/canardlapin/gale) |
| Fit lifecycles, cross-fitting, fitted artifacts (predictive module only) | [Alder](https://github.com/canardlapin/alder) |

The core has no Alder, imaging, surface, atlas, dataset or fMRI-fit dependency.
Neuroimaging integrations live downstream (for example in ScalaFIM).

## Modules

| Artifact | Purpose |
| --- | --- |
| `mvpa4s-core` | axes, reindexing legs, evidence identity and origins, relations, pattern analyses, measurement and execution contracts |
| `mvpa4s-predictive` | optional [Alder](https://github.com/canardlapin/alder) adapters: training-only preparation, held-out prediction, validation and tuning; see the [predictive guide](docs/predictive.md) |
| `mvpa4s-locus` | optional locus-domain region/selection measurements, centered neighborhoods, scatter and exact volume support graphs; no imaging or surface dependency |
| `mvpa4s-testkit` | test-scope support for consumers: validated fixture identities and R parity reference data |

## Example

```scala
import multivar.core.SpaceRole
import mvpa4s.{AxisRef, ReindexingLeg}
import resample4s.core.{Draw, IndexSpace}

for
  trials <- AxisRef.fromStableKeys("trials", SpaceRole.Samples, Vector("t0", "t1", "t2"),
    "acquisition-order", "trial", "unscaled", Vector("source:run-1")).left.map(_.message)
  space <- IndexSpace.of(trials.size).left.map(_.toString)
  draw <- Draw.from(IArray(2, 0, 2), space).left.map(_.toString)
  leg <- ReindexingLeg.bind(trials, draw).left.map(_.message)
yield leg.ordinals.toVector // Vector(2, 0, 2): occurrence order is preserved
```

The [core guide](docs/core.md) covers the evidence model, kernels and pattern
interpretation. A compiled version of this and other public-API uses lives in
[`examples/consumer`](examples/consumer/src/main/scala/example/MvpaConsumer.scala).

## Identity and compatibility

Persisted scientific tags and digest framing are unchanged from the code's
origin in ScalaFIM, including strings that contain `scalafim`; changing them
would change every stored digest. Consumers that bind their own identities to
mvpa4s evidence frame them with `AxisDigest`.

Scoped replay is held through a lease: `ScopeReplay.acquire` returns a
`ScopeLease`, and only that lease can attach its live witness to a relation or
expire it. Acquisition itself is unauthenticated; the guarantee is that a
witness cannot be transplanted or outlive its lease, not that only a particular
provider can mint one.

## Development

Providers are immutable source pins in `build.sbt`. Multivar also needs its
pinned Gale and Resample4s artifacts:

```sh
tools/prepare-pinned-dependencies.sh
sbt mvpa4sCompileAll mvpa4sTestAll
```

For coordinated provider work, `-Dmvpa4s.<provider>.build=<path>` (for example
`-Dmvpa4s.locus4s.build=../locus4s`) substitutes a sibling checkout.

Fixture generators live under `tools/`; see
[`tools/r-parity/mvpa-fixtures.md`](tools/r-parity/mvpa-fixtures.md).

The code was extracted from ScalaFIM (`canardlapin/scalafim`): the core at
`33d8f46de63229920003ab17fbfbc9fe7f43b613` and the predictive module at
`04065f526e276b8ececf4daf8e3c14c6e66e6ff2`; earlier history lives there.

## License

Apache License 2.0; see [LICENSE](LICENSE).
