import xml.etree.ElementTree as ET
import os

def deduplicate_strings(filepath):
    tree = ET.parse(filepath)
    root = tree.getroot()
    
    seen_names = set()
    elements_to_remove = []
    
    # Iterate backwards so we keep the LAST definition (which should be our appended one if any conflicts exist)
    for elem in reversed(root):
        if elem.tag == 'string':
            name = elem.get('name')
            if name in seen_names:
                elements_to_remove.append(elem)
            else:
                seen_names.add(name)
                
    # Remove the duplicated elements
    for elem in elements_to_remove:
        root.remove(elem)
        
    tree.write(filepath, encoding='utf-8', xml_declaration=True)

deduplicate_strings('app/src/main/res/values/strings.xml')
deduplicate_strings('app/src/main/res/values-en/strings.xml')
deduplicate_strings('app/src/main/res/values-pt/strings.xml')

print("Deduplication complete.")
