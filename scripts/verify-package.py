#!/usr/bin/env python3
"""Ensure the executable JAR contains the exact frontend build and backend CSS."""
import argparse
from pathlib import Path
from zipfile import ZipFile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('jar', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
dist = root / 'frontend/dist'
if any(not (dist / file).is_file() for file in ('index.html', 'home.html', 'home-zh.html', 'robots.txt')):
    parser.error('frontend/dist is missing; run mvn clean package first')
with ZipFile(args.jar) as jar:
    names = set(jar.namelist())
    prefix = 'BOOT-INF/classes/static/'
    expected = set()
    for file in dist.rglob('*'):
        if file.is_file():
            name = prefix + file.relative_to(dist).as_posix()
            expected.add(name)
            if name not in names or jar.read(name) != file.read_bytes():
                raise SystemExit(f'Missing or stale frontend resource: {name}')
    unexpected = {name for name in names if name.startswith(prefix + 'assets/') and not name.endswith('/')} - expected
    if unexpected:
        raise SystemExit(f'Stale generated assets remain: {sorted(unexpected)}')
    for stylesheet in ('guides.css',):
        if prefix + 'css/' + stylesheet not in names:
            raise SystemExit(f'Backend static css/{stylesheet} is missing')
print(f'Verified {args.jar}: {len(expected)} frontend files match the current build; backend CSS preserved')
