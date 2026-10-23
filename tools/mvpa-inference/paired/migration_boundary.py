"""Provider routing and source custody for the migrated rank harness.

Historical identities are deliberately separate: prior receipts are immutable.
"""
from pathlib import Path
import hashlib
import re
import subprocess

CURRENT_MULTIVAR = '7afdeea290fdcc80b81a4126aa303f93a5348c0e'
HISTORICAL_MULTIVAR = 'ab811e257dd67f77e8c3b70cb1ea600f274429a3'


def command(provider, platform, suite):
    if provider not in ('current', 'historical') or platform not in ('JVM', 'JS'):
        raise ValueError('unknown provider or platform')
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9]*Suite', suite):
        raise ValueError('invalid suite')
    if provider == 'historical':
        return ['python3', 'tools/build/sbt-warm', f'mvpa{platform}/testOnly *{suite}']
    return ['python3', 'tools/build/sbt-warm', f'core{platform}/testOnly mvpa4s.inference.{suite}']


def source_locks(root, provider):
    root = Path(root)
    if provider == 'current':
        groups = ['modules/mvpa4s-core', 'tools/mvpa-inference', 'build.sbt',
                  'project/build.properties', 'project/plugins.sbt', '.jvmopts', 'tools/build/sbt-warm',
                  'docs/plans/unified-mvpa-rank-population-v1.md', 'docs/plans/unified-mvpa-rank-metric-bindings-v1.md',
                  'docs/verification/umvpa-rank-v2-20261008/probe-manifest.json',
                  'docs/verification/umvpa-rank-integration-20261008/paired-inventory-proposal.json',
                  'docs/verification/umvpa-rank-integration-20261008/historical/retained-input-sha256.json']
    elif provider == 'historical':
        groups = ['modules/mvpa', 'modules/scenario-testkit', 'tools/mvpa-inference',
                  'tools/build/sbt-warm', 'build.sbt', '.jvmopts',
                  'project/build.properties', 'project/plugins.sbt']
    else:
        raise ValueError('unknown provider')
    result = {}
    for group in groups:
        files = subprocess.check_output(['git', 'ls-files', '--', group], cwd=root, text=True).splitlines()
        if not files:
            raise ValueError('empty required source group: ' + group)
        for name in files:
            p = root / name
            if p.suffix not in ('.gz', '.bundle'):
                result[name] = hashlib.sha256(p.read_bytes()).hexdigest()
    pins = re.findall(r'lazy val multivarRevision\s*=\s*"([0-9a-f]{40})"', (root/'build.sbt').read_text())
    expected = CURRENT_MULTIVAR if provider == 'current' else HISTORICAL_MULTIVAR
    if pins != [expected]:
        raise ValueError('provider pin changed or missing')
    return dict(sorted(result.items()))


def verify_sources(root, provider, locks):
    if not isinstance(locks, dict) or not locks or source_locks(root, provider) != locks:
        raise ValueError('empty, stale or incomplete source lock')
