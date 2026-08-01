import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/data/PreferencesManager.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

old_style = """    var visualizerStyle: String
        get() = prefs.getString("visualizerStyle", "BARS") ?: "BARS"
        set(value) = prefs.edit().putString("visualizerStyle", value).apply()"""

new_style = """    var visualizerStyle: String
        get() = prefs.getString("visualizerStyle", "BARS") ?: "BARS"
        set(value) = prefs.edit().putString("visualizerStyle", value).apply()

    var visualizerArchetype: Int
        get() = prefs.getInt("visualizerArchetype", 0) // 0 = Overlapped, 1 = Segmented
        set(value) = prefs.edit().putInt("visualizerArchetype", value).apply()"""

if "var visualizerArchetype: Int" not in content:
    content = content.replace(old_style, new_style)
    with open(file_path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Updated PreferencesManager")
else:
    print("Already updated")
