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

    def full(self):
        sections = ["format_version=1\nbuild_commit=test-commit\n"]
        for name in ("yt-dlp", "ffmpeg", "ffprobe"):
            tool = self.root / name
            tool.write_bytes(b"synthetic binary")
            sections.append(
                f"[{name}]\nversion=test-version\nsha256={sha256(tool)}\n"
                f"size_bytes={tool.stat().st_size}\nsource_type=test\nsource_url=https://example.com/tool\n"
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
        with self.assertRaisesRegex(ValueError, "SHA256"):
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
