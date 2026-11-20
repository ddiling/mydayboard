"""Tests for .github/scripts/screenshot_comment.py (pure functions only, no network)."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import screenshot_comment as sc  # noqa: E402

BASE = "b" * 40
HEAD = "a" * 40
MERGE_BASE = "c" * 40
BOT = {"login": sc.BOT_LOGIN}
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
        comments = [{"id": 1, "body": "hello", "user": BOT}, {"id": 2, "body": f"{sc.MARKER}\nx", "user": BOT}]
        self.assertEqual(sc.find_comment_id(comments), 2)

    def test_find_comment_id_ignores_a_marker_comment_from_another_author(self):
        comments = [{"id": 1, "body": f"{sc.MARKER}\nx", "user": {"login": "someone"}}]
        self.assertIsNone(sc.find_comment_id(comments))

    def test_before_link_uses_the_sha_it_is_given_so_the_caller_can_pass_the_merge_base(self):
        body = sc.build_body([("M", shot("ListPreview"))], MERGE_BASE, HEAD)
        self.assertIn(f"/{MERGE_BASE}/{shot('ListPreview')}", body)

    def test_parse_keeps_deleted_screenshots(self):
        self.assertEqual(sc.parse_name_status(f"D\t{shot('X')}\n"), [("D", shot("X"))])

    def test_only_deleted_baselines_are_listed_as_removed_without_images(self):
        body = sc.build_body([("D", shot("OldPreview")), ("D", shot("GonePreview"))], BASE, HEAD)
        self.assertIn("Removed: 2 baselines", body)
        self.assertIn("OldPreview (Light)", body)
        self.assertNotIn("No screenshot changes", body)
        self.assertNotIn("![", body)

    def test_removed_line_is_added_below_the_table_when_images_also_changed(self):
        body = sc.build_body([("A", shot("NewPreview")), ("D", shot("OldPreview"))], BASE, HEAD)
        self.assertIn("![after]", body)
        self.assertIn("Removed: 1 baselines", body)

    def test_find_comment_id_is_none_without_a_marker(self):
        self.assertIsNone(sc.find_comment_id([{"id": 1, "body": "hello"}]))


if __name__ == "__main__":
    unittest.main()
