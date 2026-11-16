from pathlib import Path
s=(Path(__file__).parents[1]/'app/src/main/java/com/lessonbell/watch/Lessons.java').read_text()
expected={'MONDAY':4,'TUESDAY':4,'WEDNESDAY':3,'THURSDAY':4}
for day,count in expected.items():
    assert s.count(f'p(DayOfWeek.{day},')==count, (day,count)
assert s.count('v.add(p(')==15
assert all(x in s for x in ('"08:00"','"15:25"','"P5S"'))
print('PASS: 15 teaching periods transcribed for Monday–Thursday')
