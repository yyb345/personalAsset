import importlib.util
from pathlib import Path
import re
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('analysis', Path(__file__).resolve().parents[1] / 'analyze-search-console.py')
analysis = importlib.util.module_from_spec(spec)
spec.loader.exec_module(analysis)


class SearchAnalysisTest(unittest.TestCase):
    def test_brand_expression_is_scoped_to_brand_variants(self):
        expression = re.compile(analysis.DEFAULT_BRAND, re.I)
        for query in ['xlearning', 'X Learning', 'x-learning app', 'www.xlearning.top']:
            self.assertIsNotNone(expression.search(query), query)
        for query in ['english learning', 'learning with youtube', 'nextlearning', 'xlearningish']:
            self.assertIsNone(expression.search(query), query)

    def test_bom_chinese_csv_and_weighted_metrics(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'Queries.csv'
            path.write_text('查询,点击次数,展示次数,平均排名\nxlearning,2,10,1\nenglish listening,1,90,11\n', encoding='utf-8-sig')
            rows = analysis.read_export(path, 'query')
            self.assertEqual(analysis.totals(rows), (3, 100, '3.00%', '10.00'))
            output = analysis.report(rows, [{'label': 'https://example.com/', 'clicks': 7, 'impressions': 150, 'position': 5}], re.compile(analysis.DEFAULT_BRAND, re.I), 'Test', 20)
            self.assertIn('| Nonbrand queries | 1 | 1 | 90 |', output)
            self.assertIn('minus visible query clicks: 4', output)
            self.assertIn('cannot be classified as brand/nonbrand', output)

    def test_missing_metrics_fail_clearly(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'Queries.csv'
            path.write_text('Top queries,Clicks\nhello,1\n', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'missing impressions column'):
                analysis.read_export(path, 'query')


if __name__ == '__main__':
    unittest.main()
