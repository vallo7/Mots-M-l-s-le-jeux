#!/usr/bin/env python3
"""Aide de développement : vérifie que les appels aux fonctions/constructeurs DU PROJET utilisent des paramètres nommés
qui existent et pas trop d'arguments positionnels. Complète check_kotlin_imports.py quand Gradle n'est pas disponible."""
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FILES = [p for d in ("app/src/main/java", "domain/src/main/kotlin") for p in (ROOT / d).rglob("*.kt")]


def clean(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"(?m)(?<![:\"])//.*$", "", text)
    text = re.sub(r'"(?:\\.|[^"\\])*"', '""', text)
    return text


def balanced(text, start):
    """text[start] == '(' -> renvoie (contenu, index après ')')."""
    depth = 0
    for i in range(start, len(text)):
        c = text[i]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
            if depth == 0:
                return text[start + 1:i], i + 1
    return None, len(text)


def split_top(args):
    parts, depth, cur = [], 0, ""
    for c in args:
        if c in "([{<" and not (c == "<" and False):
            depth += 1 if c != "<" else 0
        if c in ")]}":
            depth -= 1
        if c == "," and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += c
    if cur.strip():
        parts.append(cur)
    return [p.strip() for p in parts if p.strip()]


defs = defaultdict(list)  # nom -> liste de (fichier, [params], aVararg)
texts = {}
for path in FILES:
    text = clean(path.read_text(encoding="utf-8"))
    texts[path] = text
    for m in re.finditer(r"\bfun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?(\w+)\s*\(", text):
        body, _ = balanced(text, m.end() - 1)
        if body is None:
            continue
        params = [re.match(r"\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:vararg\s+|noinline\s+|crossinline\s+)?(?:private\s+|override\s+)?(?:val\s+|var\s+)?(\w+)\s*:", p).group(1)
                  for p in split_top(body) if re.match(r"\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:vararg\s+|noinline\s+|crossinline\s+)?(?:private\s+|override\s+)?(?:val\s+|var\s+)?(\w+)\s*:", p)]
        defs[m.group(1)].append((path, params, "vararg" in body))
    for m in re.finditer(r"\b(?:data\s+|sealed\s+|enum\s+)?class\s+(\w+)\s*(?:<[^>]*>)?\s*\(", text):
        body, _ = balanced(text, m.end() - 1)
        if body is None:
            continue
        params = [mm.group(1) for p in split_top(body) for mm in [re.match(r"\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:private\s+|override\s+)?(?:val\s+|var\s+)?(\w+)\s*:", p)] if mm]
        defs[m.group(1)].append((path, params, False))

problems = 0
for path, text in texts.items():
    for name, entries in defs.items():
        if len(entries) != 1:
            continue  # surcharges : on ne tranche pas
        _, params, vararg = entries[0]
        for m in re.finditer(rf"(?<![\w.]){re.escape(name)}\s*\(", text):
            before = text[max(0, m.start() - 12):m.start()]
            if re.search(r"\b(fun|class|object)\s+$", before):
                continue
            body, _ = balanced(text, m.end() - 1)
            if body is None:
                continue
            args = split_top(body)
            positional = 0
            for a in args:
                nm = re.match(r"(\w+)\s*=(?!=)", a)
                if nm:
                    if nm.group(1) not in params and not vararg:
                        line = text[: m.start()].count("\n") + 1
                        print(f"{path.relative_to(ROOT)}:{line} : {name}(…) argument nommé inconnu « {nm.group(1)} » (paramètres : {', '.join(params)})")
                        problems += 1
                else:
                    positional += 1
            if positional > len(params) and not vararg:
                line = text[: m.start()].count("\n") + 1
                print(f"{path.relative_to(ROOT)}:{line} : {name}(…) {positional} arguments positionnels pour {len(params)} paramètres")
                problems += 1
print("OK : appels cohérents avec les signatures du projet." if not problems else f"{problems} problème(s)")
sys.exit(1 if problems else 0)
