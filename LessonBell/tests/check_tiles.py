from pathlib import Path
src = Path('LessonBell/app/src/main/java/com/lessonbell/watch/TileLayouts.java').read_text()
required = [
    'DimensionBuilders.expand()',
    'setBackground(fill(INK,120))',
    'NEXT LESSON',
    "TODAY'S LESSONS",
    'MINT_BG', 'SKIP_BG', 'SYNC_BG', 'OPEN_BG',
    'toggle-today', '"sync"', '"open"',
    'setFreshnessIntervalMillis(60_000L)',
]
for item in required:
    assert item in src, f'missing {item}'
assert 'versionCode 4' in Path('LessonBell/app/build.gradle').read_text()
print('PASS: expanded colorful Tiles, controls, refresh and version bump')
