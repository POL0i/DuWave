import os
import re

def process_file():
    path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt'
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Extract the block of code for the lyrics indicator
    start_str = '                if (lyrics.isEmpty() && autoAnalyzeLyrics) {'
    end_str = '                }\n            }\n        }\n\n        if (showLyricsMatches) {'
    
    # Find start and end indices
    start_idx = content.find(start_str)
    if start_idx == -1:
        print("Could not find start index")
        return
        
    end_idx = content.find(end_str)
    if end_idx == -1:
        print("Could not find end index")
        return
        
    # The block we want to extract ends right before the closing braces of the album art box
    # '                }\n' is the end of the if statement
    extracted_block_end_idx = content.find('                }\n', start_idx + len(start_str)) + 18
    
    extracted_block = content[start_idx:extracted_block_end_idx]
    
    # 2. Modify the alignment and padding in the extracted block
    modified_block = extracted_block.replace(
        '.align(Alignment.BottomCenter)\n                                .padding(bottom = 8.dp)',
        '.align(Alignment.Center)\n                                .offset(y = 110.dp)'
    )
    
    # 3. Remove the block from its current location
    content = content[:start_idx] + content[extracted_block_end_idx:]
    
    # 4. Insert the modified block AFTER the album art Box.
    # We find the place where the album art box closes:
    #             }
    #         }
    # 
    #         if (showLyricsMatches) {
    target_insert = '            }\n        }\n\n        if (showLyricsMatches) {'
    insert_idx = content.find(target_insert)
    if insert_idx == -1:
        print("Could not find target insertion point")
        return
        
    content = content[:insert_idx] + modified_block + '\n' + content[insert_idx:]
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
        
    print("Done editing PlayerScreen.kt")

process_file()
