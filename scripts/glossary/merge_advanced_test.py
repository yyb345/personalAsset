import json
import unittest
from merge_advanced import OUTPUT, SUPPLEMENT, merge_terms, normalize, validate


class AdvancedMergeTest(unittest.TestCase):
    def setUp(self):
        self.terms = json.loads(OUTPUT.read_text())
        self.specialists = json.loads(SUPPLEMENT.read_text())

    def test_repeated_merge_is_idempotent(self):
        once = merge_terms(self.terms)
        self.assertEqual(once, merge_terms(once))
        self.assertEqual(self.terms, once)

    def test_rebuild_from_original_concepts_preserves_all_specialists(self):
        # Simulate a fresh crawl of just the original source concepts.
        base = [t for t in self.terms if not t['id'].startswith('ai-advanced-')]
        rebuilt = merge_terms(base)
        self.assertEqual(self.terms, rebuilt)
        expected_new = sum(t['id'].startswith('ai-advanced-') for t in self.specialists)
        self.assertEqual(expected_new, len(rebuilt) - len(base))
        self.assertGreaterEqual(len(self.specialists), 102)

    def test_duplicate_alias_is_not_silently_assigned_to_a_different_concept(self):
        item = dict(self.specialists[0], id='another-concept')
        with self.assertRaisesRegex(ValueError, 'Alias collision'):
            merge_terms(self.terms, [item])

    def test_every_concept_has_bilingual_content_and_sources(self):
        validate(self.terms)
        self.assertEqual(795 + sum(t['id'].startswith('ai-advanced-') for t in self.specialists), len(self.terms))
        self.assertEqual(795, sum(not t['id'].startswith('ai-advanced-') for t in self.terms))

    def test_semantically_different_punctuation_does_not_merge_metrics(self):
        self.assertNotEqual(normalize('pass@k'), normalize('pass^k'))
        self.assertNotEqual(normalize('C++'), normalize('C'))
        self.assertEqual(normalize('decoder-only model'), normalize('decoder only model'))


if __name__ == '__main__':
    unittest.main()
