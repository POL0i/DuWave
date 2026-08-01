with open("app/src/main/java/com/example/beatpulse/ui/screens/UnifiedLibraryScreen.kt", "r") as f:
    lines = f.readlines()

# Delete lines 299 to 400 (inclusive) -> indices 298 to 399
# And delete line 485 -> index 484, but since we delete 102 lines before it, index becomes 484 - 102 = 382

del lines[484]
del lines[298:400]

with open("app/src/main/java/com/example/beatpulse/ui/screens/UnifiedLibraryScreen.kt", "w") as f:
    f.writelines(lines)
