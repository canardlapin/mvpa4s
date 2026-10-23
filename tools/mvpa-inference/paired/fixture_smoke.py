#!/usr/bin/env python3
"""Replay one retained fixture on one current platform; no pilot streams."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tarfile
import time

from protocol import LIMITS, command, sha, source_locks, validate_rows, verify_sources, write_once
from probe import environment, lock_file
from resources import host_profile, monitor, shutdown

FIXTURE = 'rank-v2-resource-n80-p4-q6-intercept'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=('JVM', 'JS'), required=True)
    parser.add_argument('--output', type=Path, required=True, help='new receipt directory')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[3]
    locks = source_locks(root, 'current')
    status = subprocess.check_output(['git', 'status', '--porcelain', '--untracked-files=all', '--',
        'modules/mvpa4s-core', 'tools/mvpa-inference', 'tools/build/sbt-warm', *locks], cwd=root, text=True)
    if status.strip(): raise ValueError('source closure must be committed and clean')
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
    base = root/'docs/verification/umvpa-rank-v2-20261008'
    archive = base/'probe/inputs.tar.gz'
    identities = json.loads((root/'docs/verification/umvpa-rank-integration-20261008/historical/retained-input-sha256.json').read_text())
    cells = json.loads((base/'probe-manifest.json').read_text())['cells']
    matches = [c for c in cells if c['id'] == FIXTURE]
    if len(matches) != 1: raise ValueError('retained fixture identity missing or duplicated')
    case = matches[0]; name = FIXTURE + '.tsv'
    with tarfile.open(archive, 'r:gz') as tar:
        if set(tar.getnames()) != set(identities) or len(tar.getnames()) != len(identities):
            raise ValueError('unexpected retained archive inventory')
        data = tar.extractfile(name).read()
    if hashlib.sha256(data).hexdigest() != identities.get(name): raise ValueError('retained fixture bytes changed')
    profile = host_profile()
    if profile['available_bytes'] < LIMITS['required_headroom_bytes']: raise ValueError('host headroom insufficient')
    out = args.output.resolve(); out.mkdir(parents=True, exist_ok=False)
    input_path = out/name; input_path.write_bytes(data)
    case_list = out/'cases.txt'; case_list.write_text(str(input_path)+'\n')
    lock = out/'source-lock.json'; lock_file(lock, locks)
    output = out/'records.jsonl'; env = environment(root, commit, lock, case_list, output)
    write_once(out/'source.json', dict(commit=commit, locks=locks, input_sha256=sha(input_path),
                                       archive_sha256=sha(archive), fixture=FIXTURE, host=profile))
    with (out/'lifecycle.log').open('x') as lifecycle:
        shutdown(root, lifecycle, env)
        try:
            with (out/'worker.log').open('x') as log:
                receipt = monitor(root, command('current', args.platform, 'RankPairedPilotSuite'), env, log,
                                  LIMITS, time.monotonic()+LIMITS['cell_wall_seconds'], out/'no-other-provider')
            write_once(out/'process.json', receipt)
            rows = [json.loads(line) for line in output.read_text().splitlines()] if output.exists() else []
            failures = validate_rows(rows, case, 'current', commit, sha(lock),
                                     [dict(sha256=sha(input_path),root_seed64=case['root_seed64'])], phase='fixture')
            verify_sources(root, 'current', locks)
            if sha(input_path) != identities[name] or receipt['worker_exit'] or receipt['resource_refusal'] or failures:
                raise RuntimeError('fixture replay failed; receipts retained')
            write_once(out/'result.json', dict(status='passed', platform=args.platform, records=len(rows),
                                               fixture=FIXTURE, scientific_admission=False))
        finally:
            shutdown(root, lifecycle, env)

if __name__ == '__main__':
    main()
