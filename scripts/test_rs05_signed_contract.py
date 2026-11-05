"""Real Cuadrante APK -> signed inspection -> final catalog; temporary TEST ONLY key."""
import base64
import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import distribution as d
import mobile_distribution as m
from check_rs05_compatibility import ROOT


class SignedContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary=tempfile.TemporaryDirectory(prefix='cuadrante-ephemeral-test-only-')
        cls.addClassCleanup(cls.temporary.cleanup)
        cls.folder=Path(cls.temporary.name)
        cls.environment=dict(os.environ, CUADRANTE_FIXTURE_PASSWORD=secrets.token_urlsafe(32))
        def run(*args):
            result=subprocess.run(list(map(str,args)),env=cls.environment,capture_output=True)
            if cls.environment['CUADRANTE_FIXTURE_PASSWORD'].encode() in result.stdout+result.stderr:
                raise RuntimeError('Fixture password appeared in tool output')
            if result.returncode: raise RuntimeError('Ephemeral fixture tool failed; private output suppressed')
            return result.stdout
        store=cls.folder/'test-only.p12'
        run('keytool','-genkeypair','-storetype','PKCS12','-keystore',store,'-alias','fixture','-keyalg','RSA','-keysize','3072',
            '-validity','2','-dname','CN=TEST ONLY EPHEMERAL','-storepass:env','CUADRANTE_FIXTURE_PASSWORD')
        cls.pin=hashlib.sha256(run('keytool','-exportcert','-keystore',store,'-alias','fixture','-storepass:env','CUADRANTE_FIXTURE_PASSWORD')).hexdigest()
        cls.apk=cls.folder/'Android-release.apk'
        run(d.tools()/'apksigner','sign','--ks',store,'--ks-key-alias','fixture','--ks-pass','env:CUADRANTE_FIXTURE_PASSWORD',
            '--key-pass','env:CUADRANTE_FIXTURE_PASSWORD','--v1-signing-enabled','false','--v2-signing-enabled','true',
            '--out',cls.apk,ROOT/'app/build/outputs/apk/release/app-release-unsigned.apk')
        cls.settings=d.load(ROOT/'.github/android-distribution.json')

    def test_final_catalog_matches_real_signed_apk(self):
        actual=m.inspect(self.apk,'release',self.pin,self.settings)
        icon=(ROOT/self.settings['iconSource']).read_bytes()
        with patch.object(m,'api',return_value=dict(encoding='base64',size=len(icon),content=base64.b64encode(icon).decode())):
            files=m.publication_files(self.settings,'a'*40,actual,{})
        catalog=json.loads(files['robstore.json'])
        contract=json.loads(files[d.CONTRACT])
        self.assertEqual(contract,actual)
        for key in ('packageName','apkAssetName','versionCode','versionName'):
            self.assertEqual(catalog[key],actual[key])
        self.assertEqual(catalog['name'],self.settings['name'])
        self.assertEqual(catalog['description'],self.settings['description'])
        self.assertEqual(files[catalog['icon']],icon)
        self.assertEqual(actual['sha256'],d.digest(self.apk))

    def test_unsigned_release_rejected(self):
        with self.assertRaises(ValueError):
            m.inspect(ROOT/'app/build/outputs/apk/release/app-release-unsigned.apk','release',self.pin,self.settings)

    def test_tampered_signed_bytes_rejected(self):
        changed=self.folder/'tampered.apk'
        body=bytearray(self.apk.read_bytes()); body[100]^=1; changed.write_bytes(body)
        with self.assertRaises(ValueError): m.inspect(changed,'release',self.pin,self.settings)

    def test_false_metadata_and_certificate_rejected(self):
        actual=m.inspect(self.apk,'release',self.pin,self.settings)
        for key,value in [('packageName','es.other'),('versionCode',0),('versionName','invented'),('sha256','00'*32)]:
            with self.subTest(key=key),self.assertRaises(ValueError): d.validate_contract(dict(actual,**{key:value}),actual)
        with self.assertRaises(ValueError): m.inspect(self.apk,'release','00'*32,self.settings)


if __name__=='__main__': unittest.main()
