import os
import re
import yaml

src_dir = r"c:\Users\Toyger\OneDrive\Projects51c\DeathSwap\src\main\java"
res_dir = r"c:\Users\Toyger\OneDrive\Projects51c\DeathSwap\src\main\resources"
fr_yaml = os.path.join(res_dir, "messages_fr.yml")
en_yaml = os.path.join(res_dir, "messages_en.yml")

def extract_keys_from_code():
    keys = set()
    pattern = re.compile(r'Lang\.get(?:Component)?\(\s*"([^"]+)"\s*(?:,|0)')
    # also support variables like `Lang.get("death-cause-" + cause.name() + "-name")`
    # actually, variable-based keys are harder to analyze statically. Let's just find hardcoded ones first.
    
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith(".java"):
                with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                    content = f.read()
                    matches = pattern.findall(content)
                    keys.update(matches)
    return keys

def load_yaml_keys(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        data = yaml.safe_load(f)
        if hasattr(data, "keys"):
            return set(data.keys())
        return set()

code_keys = extract_keys_from_code()
print(f"Hardcoded keys found in Java code: {len(code_keys)}")

fr_keys = load_yaml_keys(fr_yaml)
en_keys = load_yaml_keys(en_yaml)

print("\n--- Keys in code but NOT in messages_fr.yml ---")
for k in sorted(code_keys - fr_keys):
    print(k)

print("\n--- Keys in code but NOT in messages_en.yml ---")
for k in sorted(code_keys - en_keys):
    print(k)

print("\n--- Keys in messages_fr.yml but NOT in messages_en.yml ---")
for k in sorted(fr_keys - en_keys):
    print(k)

print("\n--- Keys in messages_en.yml but NOT in messages_fr.yml ---")
for k in sorted(en_keys - fr_keys):
    print(k)
