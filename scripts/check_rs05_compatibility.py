#!/usr/bin/env python3
"""Publication and persisted identity guards. No signing secrets are accessed."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'es.robertodorado.cuadrante'


def require(ok, message):
    if not ok: raise ValueError(message)


def source_identity(root=ROOT):
    gradle = (root/'app/build.gradle.kts').read_text()
    require(re.findall(r'applicationId\s*=\s*"([^"]+)"', gradle) == [PACKAGE], 'Release package changed or overridden')
    require(re.findall(r'applicationIdSuffix\s*=\s*"([^"]+)"', gradle) == ['.debug'] and
            re.findall(r'versionNameSuffix\s*=\s*"([^"]+)"', gradle) == ['-DEV'] and
            'manifestPlaceholders["appLabel"] = "Cuadrante DEV"' in gradle, 'DEV identity changed')
    code = re.search(r'versionCode\s*=\s*(\d+)', gradle)
    name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
    require(code and 1 <= int(code[1]) <= 2100000000 and name and re.fullmatch(r'\d+\.\d+\.\d+', name[1]), 'Invalid version')
    for key, value in [('compileSdk',36), ('minSdk',26), ('targetSdk',36)]:
        require(re.search(r'\b'+key+r'\s*=\s*'+str(value)+r'\b',gradle), 'SDK policy changed')
    data = root/'app/src/main/java/es/robertodorado/cuadrante/data/local'
    for file, constant, store, keys in [
        ('AppSettingsRepository.kt','DATA_STORE_NAME','app_settings',{'pattern_type','shift_sequence','reference_date'}),
        ('IncidentsRepository.kt','INCIDENTS_DATA_STORE_NAME','incidents',{'incidents_v1'})]:
        body = (data/file).read_text()
        require(f'const val {constant} = "{store}"' in body and
                re.search(r'preferencesDataStore\(\s*name\s*=\s*'+constant+r'\s*,?\s*\)', body), 'DataStore identity changed')
        require(keys <= set(re.findall(r'stringPreferencesKey\("([^"]+)"\)', body)), 'Persisted preference key changed')
        require(not re.search(r'\b(?:deleteFile|deleteDatabase)\s*\(|\.clear\s*\(',body), 'Destructive persistence operation')
    config = json.loads((root/'.github/android-distribution.json').read_text())
    require(config['repository']=='robdor80/Cuadrante_App' and config['packages']==dict(release=PACKAGE,dev=PACKAGE+'.debug') and
            config['minSdk']==26 and config['targetSdk']==36 and config['robstorePolicy'] is False, 'Distribution identity changed')
    require((root/config['iconSource']).read_bytes().startswith(b'\x89PNG\r\n\x1a\n'), 'Official icon missing')
    require(not re.search(r'storePassword|keyPassword|storeFile\s*=|signingConfigs\s*[.{]',gradle), 'Signing must stay outside Gradle')
    return int(code[1]),name[1]


def verify_apks(root=ROOT):
    code,name=source_identity(root)
    tools=Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0'
    for variant,package,label,version in [('release',PACKAGE,'Cuadrante App',name),('debug',PACKAGE+'.debug','Cuadrante DEV',name+'-DEV')]:
        apk=root/f'app/build/outputs/apk/{variant}/app-{variant}-unsigned.apk'
        def run(*args): return subprocess.check_output(list(map(str,args)),text=True)
        badging=run(tools/'aapt','dump','badging',apk)
        require(f"package: name='{package}' versionCode='{code}' versionName='{version}'" in badging,'Compiled APK identity/version differs')
        require(f"application-label:'{label}'" in badging and "sdkVersion:'26'" in badging and "targetSdkVersion:'36'" in badging and "compileSdkVersion='36'" in badging,'APK label/SDK differs')
        require(('application-debuggable' in badging)==(variant=='debug'),'Unexpected debuggable state')
        run(tools/'zipalign','-c','-p','4',apk)
        signature=subprocess.run([str(tools/'apksigner'),'verify',str(apk)],capture_output=True)
        with zipfile.ZipFile(apk) as archive:
            require(not any(re.match(r'META-INF/.*\.(RSA|DSA|EC|SF)$',entry,re.I) for entry in archive.namelist()),'Unexpected JAR signature')
        require(signature.returncode != 0 and b'DOES NOT VERIFY' in signature.stderr and b'APK Sig Block 42' not in apk.read_bytes(),'Normal CI APK must be unsigned')
        print(f'{variant}: package/version/label/SDK/alignment/unsigned PASS; SHA-256 {hashlib.sha256(apk.read_bytes()).hexdigest()}')


if __name__=='__main__':
    source_identity()
    if '--apks' in __import__('sys').argv: verify_apks()
    print('Historical package and DataStore identities: PASS')
