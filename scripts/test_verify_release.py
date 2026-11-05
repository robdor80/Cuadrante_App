"""Fail-closed regressions for the independent RS-05 candidate verifier."""
import hashlib
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import verify_release as v

class ReleaseVerifierTests(unittest.TestCase):
    def setUp(self):
        self.folder=tempfile.TemporaryDirectory(); self.addCleanup(self.folder.cleanup)
        self.apk=Path(self.folder.name)/'Android-release.apk'; self.apk.write_bytes(b'test APK bytes')
        self.pin='ab'*32
        self.contract=dict(schemaVersion=1,channel='release',packageName='es.robertodorado.cuadrante',versionCode=2,versionName='1.0.1',apkAssetName=self.apk.name,sha256=hashlib.sha256(self.apk.read_bytes()).hexdigest())
        self.badging="package: name='es.robertodorado.cuadrante' versionCode='2' versionName='1.0.1'\nsdkVersion:'26'\ntargetSdkVersion:'36'\n"
        self.cert=f'Signer #1 certificate SHA-256 digest: {self.pin}\nVerified using v2 scheme (APK Signature Scheme v2): true\n'
    def verify(self,contract=None,badging=None,cert=None):
        def tool(*args):
            if args[0]=='aapt': return self.badging if badging is None else badging
            if args[0]=='apksigner': return self.cert if cert is None else cert
            return ''
        with patch.object(v,'run',side_effect=tool): return v.verify(self.apk,self.contract if contract is None else contract,self.pin,2,'1.0.1')
    def test_valid_candidate(self): self.assertIn('1.0.1 (2)',self.verify())
    def test_false_contract_metadata_rejected(self):
        for key,value in [('schemaVersion',True),('channel','dev'),('packageName','other'),('versionCode',1),('versionCode',True),('versionName','1.0.0'),('apkAssetName','cuadrante-v1.0.0.apk'),('sha256','00'*32)]:
            with self.subTest(key=key,value=value),self.assertRaises(ValueError): self.verify(dict(self.contract,**{key:value}))
    def test_false_apk_identity_rejected(self):
        for old,new in [('es.robertodorado.cuadrante','es.other'),("versionCode='2'","versionCode='1'"),("versionName='1.0.1'","versionName='1.0.0'")]:
            with self.subTest(old=old),self.assertRaises(ValueError): self.verify(badging=self.badging.replace(old,new))
    def test_debuggable_rejected(self):
        with self.assertRaises(ValueError): self.verify(badging=self.badging+'application-debuggable\n')
    def test_sdk_rejected(self):
        for old in ("sdkVersion:'26'","targetSdkVersion:'36'"):
            with self.subTest(old=old),self.assertRaises(ValueError): self.verify(badging=self.badging.replace(old,''))
    def test_wrong_or_multiple_signers_rejected(self):
        for cert in ('',self.cert.replace('Scheme v2): true','Scheme v2): false'),self.cert.replace(self.pin,'00'*32),self.cert+self.cert.replace('#1','#2'),self.cert+'CN=Android Debug'):
            with self.subTest(cert=cert),self.assertRaises(ValueError): self.verify(cert=cert)
    def test_signature_and_alignment_tool_failure_propagates(self):
        for tool in ('zipalign','apksigner'):
            def run(*args):
                if args[0]==tool: raise subprocess.CalledProcessError(1,args)
                return self.badging
            with self.subTest(tool=tool),patch.object(v,'run',side_effect=run),self.assertRaises(subprocess.CalledProcessError): v.verify(self.apk,self.contract,self.pin,2,'1.0.1')
    def test_changed_apk_hash_rejected(self):
        self.apk.write_bytes(b'tampered')
        with self.assertRaises(ValueError): self.verify()

if __name__=='__main__': unittest.main()
