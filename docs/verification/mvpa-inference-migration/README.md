# MVPA inference harness migration

The calibration and rank harness now lives in mvpa4s. ScalaFIM removed the 26 migrated tools at `f551218be`; its group oracle and spatial PyMVPA generator remain in ScalaFIM. `copied-inputs.json` records the original source revision, source and destination hashes, and copied historical evidence. Historical records retain their original source and runtime identities; copying them is not a rerun or scientific admission.

The current runner uses `coreJVM`/`coreJS` and `mvpa4s.inference`; the historical route retains its original checkout, projects, packages, TSV locks and wrapper. Fresh source locks bind the provider closure, wrapper, build files, and current multivar pin. The wrapper uses the worktree-specific sbt base and resource monitor.

At mvpa4s commit `ba73ed6077c0218ff55411938cdda83300623170`, predictive tests passed 108 JVM and 108 JS cases, focused inference tests passed 21 JVM and 21 JS cases, and the retained-fixture smoke ran one opt-in suite case and produced two validated method records on each platform. The outer commands exited successfully, including shutdown. [executed-checks.json](executed-checks.json) maps retained compressed raw logs, metadata, process and source receipts to their original paths and SHA-256 hashes. The paired Python tests passed 12 cases, calibration Python tests 25, wrapper tests 62 with one skipped, and the R population check passed with locale warnings.

An independent static review of the same commit found no migration-specific issues; its SHA-bound verdict and scope are retained in the execution bundle. Static review and the executed checks are separate evidence.

A fixture result marker is written before shutdown: count it as success only when the outer command also exits zero, establishing cleanup completed. The resource monitor's RSS cap covers owned JVMs and excludes Node; these checks do not establish a total JVM-plus-Node memory bound. The smoke is not a campaign or scientific admission.

The plan references `docs/audits/unified-mvpa-inference-calibration-v1.json.gz`, absent in the source checkout. The [evidence closure audit](evidence-closure-audit.md) describes the retained historical inputs and missing current admission evidence. Historical platform preflight has a retained discrepancy and is not passing; current confirmation remains unadmitted.

To repeat the fixture smoke on a committed, clean source closure with exclusive use of the worktree server, use fresh output directories:

```sh
python3 tools/mvpa-inference/paired/fixture_smoke.py --platform JVM --output /tmp/mvpa-fixture-smoke-jvm
python3 tools/mvpa-inference/paired/fixture_smoke.py --platform JS --output /tmp/mvpa-fixture-smoke-js
```
