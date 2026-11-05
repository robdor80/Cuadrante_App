#!/usr/bin/env python3
"""Read-only verification of an RS-05 signed candidate and its generated contract."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
from check_rs05_compatibility import source_identity


def run(*command):
    return subprocess.run(list(map(str, command)), capture_output=True, text=True, check=True).stdout


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify(apk, contract, certificate_sha256, version_code, version_name, build_tools=None):
    require(type(contract['schemaVersion']) is int and contract['schemaVersion'] == 1, 'Expected RS-05 schema v1')
    require(contract['channel'] == 'release', 'Expected Release contract')
    require(contract['packageName'] == 'es.robertodorado.cuadrante', 'Unexpected package')
    require(contract['apkAssetName'] == 'Android-release.apk' and apk.name == contract['apkAssetName'], 'Unexpected RS-05 asset name')
    require(type(contract['versionCode']) is int and contract['versionCode'] == version_code and version_code > 1, 'Unexpected versionCode')
    require(contract['versionName'] == version_name and re.fullmatch(r'\d+\.\d+\.\d+', version_name), 'Unexpected versionName')
    require(0 < apk.stat().st_size <= 512 * 1024 * 1024, 'Invalid APK size')
    expected = certificate_sha256.replace(':', '').lower()
    require(re.fullmatch(r'[0-9a-f]{64}', expected), 'Invalid trusted certificate SHA-256')
    require(isinstance(contract['sha256'], str) and re.fullmatch(r'[0-9a-f]{64}', contract['sha256']), 'Invalid APK SHA-256')
    with apk.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    require(digest == contract['sha256'], 'APK SHA-256 differs from generated contract')
    def tool(name): return str(Path(build_tools)/name) if build_tools else name
    badging = run(tool('aapt'), 'dump', 'badging', apk)
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.MULTILINE)
    require(package is not None, 'Cannot read APK identity')
    require(package.groups() == (contract['packageName'], str(version_code), version_name), 'APK package/version differs from approved source and contract')
    require('application-debuggable' not in badging, 'Debuggable APK is forbidden')
    require("sdkVersion:'26'" in badging and "targetSdkVersion:'36'" in badging, 'Unexpected minimum/target SDK')
    run(tool('zipalign'), '-c', '-P', '16', '4', apk)
    certificates = run(tool('apksigner'), 'verify', '--verbose', '--print-certs', apk)
    digests = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$', certificates, re.MULTILINE)
    require([value.lower() for value in digests] == [expected], 'Signing certificate differs from trusted historical certificate')
    require('Verified using v2 scheme (APK Signature Scheme v2): true' in certificates, 'APK v2 signature required')
    require('CN=Android Debug' not in certificates, 'Android Debug certificate is forbidden')
    return f'Verified {package.group(1)} {version_name} ({version_code})\n{digest}  {apk.name}'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--contract', type=Path, required=True, help='robstore-update.json from the RS-05 prepare candidate')
    parser.add_argument('--certificate-sha256', required=True, help='Trusted historical public certificate digest, obtained separately')
    parser.add_argument('--build-tools', type=Path, help='Optional Android Build Tools directory; defaults to PATH')
    args = parser.parse_args()
    code, name = source_identity()
    print(verify(args.apk, json.loads(args.contract.read_text(encoding='utf-8')), args.certificate_sha256, code, name, args.build_tools))


if __name__ == '__main__':
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError, subprocess.CalledProcessError) as error:
        print(f'Release verification FAILED: {error}', file=sys.stderr)
        sys.exit(1)
