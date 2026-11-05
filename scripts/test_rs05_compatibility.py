import shutil
from pathlib import Path
import tempfile
import unittest
import check_rs05_compatibility as c


class CompatibilityTests(unittest.TestCase):
    def mutate(self,path,old,new,reject=True):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            for name in ('app/build.gradle.kts','.github/android-distribution.json',
                         'app/src/main/res/mipmap-xxxhdpi/ic_launcher.png',
                         'app/src/main/java/es/robertodorado/cuadrante/data/local/AppSettingsRepository.kt',
                         'app/src/main/java/es/robertodorado/cuadrante/data/local/IncidentsRepository.kt'):
                target=root/name; target.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(c.ROOT/name,target)
            target=root/path; body=target.read_text(); self.assertIn(old,body); target.write_text(body.replace(old,new))
            if reject:
                with self.assertRaises(ValueError): c.source_identity(root)
            else:
                self.assertEqual(c.source_identity(root),(2,'1.1.0'))

    def test_duplicate_release_package_assignment_rejected(self):
        self.mutate('app/build.gradle.kts','applicationId = "es.robertodorado.cuadrante"',
                    'applicationId = "es.robertodorado.cuadrante"\n        applicationId = "es.other"')
    def test_debug_package_change_rejected(self):
        self.mutate('app/build.gradle.kts','applicationIdSuffix = ".debug"','applicationIdSuffix = ".other"')
    def test_debug_name_change_rejected(self):
        self.mutate('app/build.gradle.kts','versionNameSuffix = "-DEV"','versionNameSuffix = "-OTHER"')
    def test_future_version_allowed(self):
        self.mutate('app/build.gradle.kts','versionCode = 1\n        versionName = "1.0.0"',
                    'versionCode = 2\n        versionName = "1.1.0"',reject=False)

    def test_current_identity(self): self.assertGreaterEqual(c.source_identity()[0],1)
    def test_package_change_rejected(self): self.mutate('app/build.gradle.kts','applicationId = "es.robertodorado.cuadrante"','applicationId = "es.other"')
    def test_settings_store_change_rejected(self): self.mutate('app/src/main/java/es/robertodorado/cuadrante/data/local/AppSettingsRepository.kt','"app_settings"','"other"')
    def test_incidents_store_change_rejected(self): self.mutate('app/src/main/java/es/robertodorado/cuadrante/data/local/IncidentsRepository.kt','"incidents"','"other"')
    def test_incidents_key_change_rejected(self): self.mutate('app/src/main/java/es/robertodorado/cuadrante/data/local/IncidentsRepository.kt','"incidents_v1"','"incidents_v2"')
    def test_settings_keys_change_rejected(self):
        for key in ('pattern_type','shift_sequence','reference_date'):
            self.mutate('app/src/main/java/es/robertodorado/cuadrante/data/local/AppSettingsRepository.kt',f'"{key}"','"other"')


if __name__=='__main__': unittest.main()
