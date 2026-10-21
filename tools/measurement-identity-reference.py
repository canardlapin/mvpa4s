#!/usr/bin/env python3
"""Independent persisted-identity witness for the ScalaFIM U4 extraction.

Python hashlib, UTF-8 length framing and little-endian signed 32-bit counts.
No production Scala code is called. Mirrors the *persisted format* at ScalaFIM
694c711c9, not package names. Output feeds SpatialIdentityGoldenSuite.
"""
import hashlib
import json
import struct


def integer(value):
    return struct.pack('<i', value)


def string(value):
    encoded = value.encode('utf-8')
    return integer(len(encoded)) + encoded


def declaration(key, revision, parameters):
    fields = [string('scalafim-measurement-frame/v1'), string(key),
              string(revision), integer(len(parameters))]
    fields.extend(string(name) + string(value) for name, value in sorted(parameters))
    return hashlib.sha256(b''.join(fields)).hexdigest()


axis_fields = ['scalafim.mvpa.axis.v1', 'padded-volume', 'observed',
               'voxel-order', 'psc', 'raw']
signature = hashlib.sha256(
    b''.join(map(string, axis_fields)) + integer(0) + integer(12) +
    b''.join(string(f'v{i}') for i in range(12))).hexdigest()
caller = declaration('spatial-fixture', 'v1', [('radius', '1.5')])
result = {'sourceRevision': '694c711c91d1b09ddbb31d08345796e8c544b1c7',
          'coordinateSignature': signature, 'callerFingerprint': caller}
for kind in ['volume', 'surface']:
    ids = [f'{kind}:{signature}:02', f'{kind}:{signature}:10']
    parameters = [('caller-declaration', caller)] + [
        ('spatial-support', f'{len(id)}:{id}:2:{left}:{right}')
        for id, left, right in [(ids[0], 0, 2), (ids[1], 9, 10)]]
    result[kind] = {'ids': ids, 'parameters': parameters,
                    'fingerprint': declaration('spatial-fixture', 'v1', parameters)}
print(json.dumps(result, indent=2))
