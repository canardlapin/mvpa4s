import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import migration_boundary as boundary

class MigrationBoundaryTest(unittest.TestCase):
    def test_route(self):
        self.assertEqual(boundary.command('current', 'JVM', 'RankPairedPilotSuite'),
                         ['python3', 'tools/build/sbt-warm', 'coreJVM/testOnly mvpa4s.inference.RankPairedPilotSuite'])
        self.assertEqual(boundary.command('historical', 'JS', 'RankHistoricalPairedPilotSuite'),
                         ['python3', 'tools/build/sbt-warm', 'mvpaJS/testOnly *RankHistoricalPairedPilotSuite'])
        with self.assertRaises(ValueError): boundary.command('current', 'JVM', 'x;bad')

    def test_locks_refuse_empty_and_stale(self):
        with patch.object(boundary, 'source_locks', return_value={'x': 'abc'}):
            for locks in ({}, {'x': 'old'}, {'x': 'abc', 'y': 'extra'}):
                with self.assertRaises(ValueError): boundary.verify_sources('.', 'current', locks)
            boundary.verify_sources('.', 'current', {'x': 'abc'})

    def test_missing_source_group_refused(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(boundary.subprocess, 'check_output', return_value=''):
            with self.assertRaisesRegex(ValueError, 'empty required source group'):
                boundary.source_locks(Path(tmp), 'current')
