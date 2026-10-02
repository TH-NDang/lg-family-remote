#!/usr/bin/env python3
import os
from pathlib import Path
from xml.etree import ElementTree as ET

paths = list(Path('app/build/test-results').glob('**/TEST-*.xml'))
counts = {key: 0 for key in ('tests', 'failures', 'errors', 'skipped')}
for path in paths:
    root = ET.parse(path).getroot()
    for key in counts:
        counts[key] += int(root.get(key, '0'))
text = '# Android verification\n\n'
text += f"JVM tests: {counts['tests']}; failures: {counts['failures']}; errors: {counts['errors']}; skipped: {counts['skipped']}.\n\n"
if not paths:
    text += '**No test result files were produced. Tests are not verified.**\n\n'
text += 'APK compilation and JVM tests are not physical LG TV compatibility tests.\n'
text += 'Preview APK: temporary debug signature. Family APK: requires a private signing secret.\n'
with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as handle:
    handle.write(text)
print(text)
