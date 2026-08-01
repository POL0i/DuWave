import xml.etree.ElementTree as ET
from xml.dom import minidom
import os

def dedup_xml(filepath):
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        seen = set()
        to_remove = []
        for child in root:
            if child.tag == 'string':
                name = child.get('name')
                if name in seen:
                    to_remove.append(child)
                else:
                    seen.add(name)
        for child in to_remove:
            root.remove(child)
        
        # Write back retaining formatting as much as possible, or just use minidom
        xmlstr = minidom.parseString(ET.tostring(root)).toprettyxml(indent="    ")
        # Remove empty lines introduced by minidom
        lines = [line for line in xmlstr.split('\n') if line.strip() != '']
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
    except Exception as e:
        print(f"Error on {filepath}: {e}")

dedup_xml('app/src/main/res/values/strings.xml')
dedup_xml('app/src/main/res/values-en/strings.xml')
dedup_xml('app/src/main/res/values-pt/strings.xml')
print("Deduplication complete.")
