"""Tests for .github/scripts/screenshot_comment.py (pure functions only, no network)."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import screenshot_comment as sc  # noqa: E402

BASE = "b" * 40
HEAD = "a" * 40
PREFIX = "feature/trackers/src/test/screenshots/io.x.TrackerKt."


def shot(name: str, variant: str = "Light") -> str:
    return f"{PREFIX}{name}.{variant}.png"


class ScreenshotCommentTest(unittest.TestCase):
    def test_no_changes_says_so_and_keeps_the_marker(self):
        body = sc.build_body([], BASE, HEAD)
        self.assertIn(sc.MARKER, body)
        self.assertIn("No screenshot changes", body)

    def test_added_image_is_embedded_from_the_head_sha_without_a_before(self):
        body = sc.build_body([("A", shot("ListPreview"))], BASE, HEAD)
        self.assertIn(f"https://raw.githubusercontent.com/ddiling/mydayboard/{HEAD}/{shot('ListPreview')}", body)
        self.assertNotIn(BASE, body)

    def test_modified_image_also_links_the_base_version_as_before(self):
        body = sc.build_body([("M", shot("ListPreview"))], BASE, HEAD)
        self.assertIn(f"/{BASE}/{shot('ListPreview')}", body)

    def test_row_shows_module_and_preview_name_from_the_file_name(self):
        body = sc.build_body([("A", shot("ListPreview"))], BASE, HEAD)
        self.assertIn("| `feature:trackers` | ListPreview (Light) |", body)

    def test_light_variants_come_before_dark(self):
        body = sc.build_body([("A", shot("APreview", "Dark_NIGHT")), ("A", shot("ZPreview"))], BASE, HEAD)
        self.assertLess(body.index("ZPreview (Light)"), body.index("APreview (Dark_NIGHT)"))

    def test_more_than_the_limit_is_listed_as_and_n_more(self):
        changes = [("A", shot(f"P{i:02d}Preview")) for i in range(sc.MAX_IMAGES + 3)]
        body = sc.build_body(changes, BASE, HEAD)
        self.assertEqual(body.count("![after]"), sc.MAX_IMAGES)
        self.assertIn("and 3 more", body)

    def test_parse_keeps_only_screenshot_pngs(self):
        output = f"A\t{shot('X')}\nM\tfeature/trackers/src/main/Foo.kt\nM\tdocs/a.png\n"
        self.assertEqual(sc.parse_name_status(output), [("A", shot("X"))])

    def test_find_comment_id_matches_the_marker_only(self):
        comments = [{"id": 1, "body": "hello"}, {"id": 2, "body": f"{sc.MARKER}\nx"}]
        self.assertEqual(sc.find_comment_id(comments), 2)

    def test_find_comment_id_is_none_without_a_marker(self):
        self.assertIsNone(sc.find_comment_id([{"id": 1, "body": "hello"}]))


if __name__ == "__main__":
    unittest.main()
