# MVPA inference harness migration

`copied-inputs.json` records the ScalaFIM source revision, input hashes, destination hashes, and copied evidence bytes. Historical records are copied unchanged and are not receipts for the new provider. The original ScalaFIM harness remains until the native fixture smoke passes. The group oracle and spatial PyMVPA generator remain in ScalaFIM.

The current route uses `coreJVM`/`coreJS` and `mvpa4s.inference`; the historical route retains its original checkout, projects, packages, TSV locks and wrapper. Fresh source locks include the current provider closure, wrapper, build files, and current multivar pin; copied historical receipts are never reissued. The wrapper uses the existing worktree-specific sbt base and resource monitor.

After committing the complete source closure and with no other command using this worktree server, run separately:

```sh
python3 tools/mvpa-inference/paired/fixture_smoke.py --platform JVM --output /tmp/mvpa-fixture-smoke-jvm
python3 tools/mvpa-inference/paired/fixture_smoke.py --platform JS --output /tmp/mvpa-fixture-smoke-js
```

Use new output directories. The smoke checks the retained fixture digest, source locks, process limits and both actual method records. It is not a campaign or scientific admission. The original plan references `docs/audits/unified-mvpa-inference-calibration-v1.json.gz`, absent in the source checkout; manifest-referenced evidence closure still needs audit. Historical platform preflight has a retained discrepancy and is not passing.
