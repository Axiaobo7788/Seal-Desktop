import pathlib
import tempfile
import unittest
from unittest.mock import patch

from verify_packaged_tools import verify
from write_tool_provenance import sha256


class PackagedToolsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)

    def full(self, platform="test", include_unsigned=False):
        sections = [f"format_version=1\nbuild_commit=test-commit\nplatform={platform}\n"]
        for name in ("yt-dlp", "ffmpeg", "ffprobe"):
            tool = self.root / name
            tool.write_bytes(b"synthetic binary")
            unsigned = "\nunsigned_sha256=canonical-hash" if include_unsigned else ""
            sections.append(
                f"[{name}]\nversion=test-version\nsha256={sha256(tool)}\n"
                f"size_bytes={tool.stat().st_size}{unsigned}\nsource_type=test\nsource_url=https://example.com/tool\n"
            )
        (self.root / "THIRD_PARTY_VERSIONS.txt").write_text("\n".join(sections), encoding="utf-8")

    def test_empty_lite_passes(self):
        verify(self.root, "Lite", "test-commit")

    def test_lite_rejects_any_tool_payload(self):
        (self.root / "ffprobe.exe").write_bytes(b"tool")
        with self.assertRaisesRegex(ValueError, "unexpectedly"):
            verify(self.root, "Lite", "test-commit")

    def test_full_requires_manifest(self):
        with self.assertRaisesRegex(ValueError, "manifest"):
            verify(self.root, "Full", "test-commit")

    @patch("verify_packaged_tools.tool_version", return_value="test-version")
    def test_full_verifies_all_tools(self, version):
        self.full()
        verify(self.root, "Full", "test-commit")
        self.assertEqual(version.call_count, 3)

    def test_wrong_commit_is_rejected(self):
        self.full()
        with self.assertRaisesRegex(ValueError, "commit"):
            verify(self.root, "Full", "other-commit")

    def test_modified_tool_is_rejected(self):
        self.full()
        (self.root / "yt-dlp").write_bytes(b"modified binary!")
        with self.assertRaisesRegex(ValueError, "payload hash"):
            verify(self.root, "Full", "test-commit")

    @patch("verify_packaged_tools.macos_unsigned_sha256", return_value="canonical-hash")
    @patch("verify_packaged_tools.tool_version", return_value="test-version")
    def test_macos_accepts_recorded_unsigned_payload_after_signing(self, _version, unsigned_hash):
        self.full(platform="macos", include_unsigned=True)
        (self.root / "yt-dlp").write_bytes(b"signed synthetic binary")
        verify(self.root, "Full", "test-commit")
        self.assertGreaterEqual(unsigned_hash.call_count, 1)

    @patch("verify_packaged_tools.macos_unsigned_sha256", return_value="different")
    def test_macos_rejects_changed_payload_when_unsigned_hash_differs(self, _unsigned_hash):
        self.full(platform="macos", include_unsigned=True)
        (self.root / "yt-dlp").write_bytes(b"tampered")
        with self.assertRaisesRegex(ValueError, "payload hash"):
            verify(self.root, "Full", "test-commit")

    @patch("verify_packaged_tools.tool_version", side_effect=TimeoutError("tool timeout"))
    def test_broken_tool_cannot_pass(self, _version):
        self.full()
        with self.assertRaises(TimeoutError):
            verify(self.root, "Full", "test-commit")

    @patch("verify_packaged_tools.tool_version", return_value="different-version")
    def test_version_mismatch_is_rejected(self, _version):
        self.full()
        with self.assertRaisesRegex(ValueError, "version differs"):
            verify(self.root, "Full", "test-commit")


if __name__ == "__main__":
    unittest.main()
