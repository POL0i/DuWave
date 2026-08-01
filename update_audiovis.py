import re

file_path = "/home/denis/DuWave/app/src/main/java/com/example/beatpulse/visualizer/AudioVisualizerManager.kt"
with open(file_path, "r", encoding='utf-8') as f:
    content = f.read()

old_advanced = "var isAdvancedMode = MutableStateFlow(prefs.isAdvancedMode)"
new_advanced = """var isAdvancedMode = MutableStateFlow(prefs.isAdvancedMode)
    var visualizerArchetype = MutableStateFlow(prefs.visualizerArchetype)"""

if "var visualizerArchetype =" not in content:
    content = content.replace(old_advanced, new_advanced)
    with open(file_path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Updated AudioVisualizerManager")
else:
    print("Already updated")
