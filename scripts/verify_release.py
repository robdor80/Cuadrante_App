#!/usr/bin/env python3
"""Read-only gate for the final RobStore APK. Requires Android Build Tools on PATH."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys


def run(*command):
    result = subprocess.run(command, capture_output=True, text=True, check=True)
    return result.stdout


def require(condition, message):
    if not condition:
        raise ValueError(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--certificate-sha256", required=True,
                        help="Trusted persistent signing certificate digest, obtained separately")
    parser.add_argument("--version-code", type=int, default=1)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    contract = json.loads((root / "robstore.json").read_text(encoding="utf-8"))
    require(type(contract["schemaVersion"]) is int and contract["schemaVersion"] == 1,
            "Expected RobStore schema v1")
    require(contract["packageName"] == "es.robertodorado.cuadrante", "Unexpected package")
    require(contract["apkAssetName"] == "cuadrante-v1.0.0.apk", "Unexpected asset name")
    icon = Path(contract["icon"])
    require(not icon.is_absolute() and ".." not in icon.parts, "Unsafe icon path")
    require((root / icon).read_bytes().startswith(b"\x89PNG\r\n\x1a\n"), "Missing PNG icon")
    require(args.apk.name == contract["apkAssetName"], "APK filename differs from contract")
    require(0 < args.apk.stat().st_size <= 512 * 1024 * 1024, "Invalid APK size")
    require(args.version_code > 0, "Invalid expected versionCode")
    expected = args.certificate_sha256.replace(":", "").lower()
    require(re.fullmatch(r"[0-9a-f]{64}", expected), "Invalid trusted certificate SHA-256")

    badging = run("aapt", "dump", "badging", str(args.apk))
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",
                        badging, re.MULTILINE)
    require(package is not None, "Cannot read APK identity")
    require(package.groups() == (contract["packageName"], str(args.version_code), "1.0.0"),
            "APK package/version does not match the approved release")
    require("application-debuggable" not in badging, "Debuggable APK is forbidden")
    require("sdkVersion:'26'" in badging and "targetSdkVersion:'36'" in badging,
            "Unexpected minimum/target SDK")
    run("zipalign", "-c", "-P", "16", "4", str(args.apk))
    certificates = run("apksigner", "verify", "--verbose", "--print-certs", str(args.apk))
    digests = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$",
                         certificates, re.MULTILINE)
    require([digest.lower() for digest in digests] == [expected],
            "Signing certificate differs from the trusted persistent certificate")
    require("CN=Android Debug" not in certificates, "Android Debug certificate is forbidden")
    with args.apk.open("rb") as apk:
        digest = hashlib.file_digest(apk, "sha256").hexdigest()
    print(certificates.rstrip())
    print(f"Verified {package.group(1)} {package.group(3)} ({package.group(2)})")
    print(f"{digest}  {args.apk.name}")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        print(f"Release verification FAILED: {error}", file=sys.stderr)
        sys.exit(1)
