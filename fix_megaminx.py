import re

with open("app/src/main/java/com/example/beatpulse/ui/components/player/MegaminxSleepTimer.kt", "r") as f:
    content = f.read()

# Remove ',\n                        pathEffect = roundedEffect' from the drawPath calls
content = content.replace(",\n                        pathEffect = roundedEffect\n                    )", "\n                    )")

with open("app/src/main/java/com/example/beatpulse/ui/components/player/MegaminxSleepTimer.kt", "w") as f:
    f.write(content)
