#!/usr/bin/env python3
"""Vérifie la présence des fichiers attendus et l'absence de secrets / règles de motion violées (CI)."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

REQUIRED = [
    "README.md", "LICENSES.md", "PRIVACY.md", ".gitignore",
    "settings.gradle.kts", "build.gradle.kts", "gradle/libs.versions.toml",
    "gradlew", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
    "app/build.gradle.kts", "domain/build.gradle.kts", "app/src/main/AndroidManifest.xml",
    ".github/workflows/android.yml",
    "data/levels/index.json", "data/levels/chapter-01.json", "data/levels/chapter-02.json",
    "app/src/main/assets/audio/praise/praise_map.json",
    "docs/DECISIONS.md", "docs/CHECKLIST.md", "docs/ARCHITECTURE.md", "docs/LEVEL_AUTHORING.md",
    "docs/ADS.md", "docs/ASSETS.md", "docs/DESIGN_SYSTEM.md",
    "scripts/generate_levels.py", "scripts/validate_levels.py", "scripts/level_tools.py",
    "scripts/generate_ui_sounds.py", "scripts/generate_icons.py", "scripts/generate_vector_icons.py", "scripts/check_resources.py",
    "docs/QA.md", "docs/RAPPORT_FINAL.md", "docs/CHANGELOG.md", ".github/workflows/instrumented.yml",
    "assets/branding/app-logo.png", "assets/branding/play-store-icon-512.png",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml", "app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png",
    "app/src/main/res/values/ads_config.xml", "app/src/main/res/values-fr/strings.xml",
]
UI_SOUNDS = ["click", "select_cell", "correct", "wrong", "hint", "gift", "progress_up", "victory", "menu_change", "success_generic", "shuffle", "swipe"]
PRAISE_KEYS = ["nice", "great", "awesome", "amazing", "insane", "unreal", "divine", "legendary"]
GOOGLE_TEST_PUBLISHER = "ca-app-pub-3940256099942544"
PEM_MARKER = "-----" + "BEGIN"
CODE_DIRS = ["app/src", "domain/src"]
SKIP_DIRS = {".git", "build", ".gradle", ".idea"}
errors = []


def fail(msg):
    errors.append(msg)


def tracked_files():
    for path in ROOT.rglob("*"):
        if path.is_file() and not (set(path.relative_to(ROOT).parts) & SKIP_DIRS):
            yield path


for rel in REQUIRED:
    if not (ROOT / rel).is_file():
        fail(f"fichier attendu manquant : {rel}")

praise_dir = ROOT / "app/src/main/assets/audio/praise"
for key in PRAISE_KEYS:
    f = praise_dir / f"{key}.mp3"
    if not f.is_file() or f.stat().st_size == 0:
        fail(f"son de praise manquant ou vide : {f.relative_to(ROOT)}")
try:
    pmap = json.loads((praise_dir / "praise_map.json").read_text(encoding="utf-8"))
    if pmap["ladder"] != PRAISE_KEYS:
        fail("praise_map.json : ladder inattendu")
    for lang, block in pmap["voices"].items():
        for key, entry in block.items():
            if not (praise_dir / entry["file"]).is_file():
                fail(f"praise_map.json ({lang}/{key}) : fichier absent {entry['file']}")
except Exception as e:  # noqa: BLE001
    fail(f"praise_map.json illisible : {e}")

for name in UI_SOUNDS:
    f = ROOT / f"app/src/main/assets/audio/ui/{name}.wav"
    if not f.is_file() or f.stat().st_size < 1000:
        fail(f"son d'interface manquant : {f.relative_to(ROOT)}")

secret_files = [".jks", ".keystore", ".p12", ".pem", ".pfx"]
for path in tracked_files():
    rel = path.relative_to(ROOT).as_posix()
    if path.suffix.lower() in secret_files or path.name in ("local.properties", "keystore.properties"):
        fail(f"fichier sensible interdit dans le dépôt : {rel}")
    if path.suffix.lower() in (".kt", ".kts", ".xml", ".md", ".json", ".py", ".yml", ".yaml", ".properties", ".toml", ".pro"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        if path.name != "check_repo.py" and PEM_MARKER in text and "PRIVATE KEY" in text:
            fail(f"clé privée détectée : {rel}")
        for ad_id in re.findall(r"ca-app-pub-(\d{10,})", text):
            if f"ca-app-pub-{ad_id}" != GOOGLE_TEST_PUBLISHER:
                fail(f"identifiant publicitaire non-test : {rel}")

for code_dir in CODE_DIRS:
    for path in (ROOT / code_dir).rglob("*"):
        if not path.is_file() or path.suffix not in (".kt", ".kts", ".xml"):
            continue
        if set(path.relative_to(ROOT).parts) & SKIP_DIRS:
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        rel = path.relative_to(ROOT).as_posix()
        if re.search(r"\b(TODO|FIXME)\b", text):
            fail(f"marqueur TODO/FIXME interdit : {rel}")
        if re.search(r"fadeIn|fadeOut|Crossfade|confetti", text, re.IGNORECASE):
            fail(f"motion interdit (fondu/confettis) : {rel}")
        if path.suffix == ".kt" and re.search(r"\b(AlertDialog|ModalBottomSheet|DropdownMenu|Snackbar|SnackbarHost)\s*\(|androidx\.compose\.ui\.window\.Dialog|\bDialog\s*\(", text):
            fail(f"composant avec fondu intégré interdit (utiliser SheetOverlay/MessageHost) : {rel}")

if errors:
    print("\n".join(f"ERREUR : {e}" for e in errors))
    if any("audio" in e for e in errors):
        import os
        print(
            "\nAIDE : des fichiers sont absents du commit testé (" + os.environ.get("GITHUB_SHA", "local")[:7] + "). "
            "S'ils existent sur la branche, ce run date d'avant leur ajout : lancer un NOUVEAU run "
            "(Actions > Android CI > Run workflow, ou pousser un nouveau commit). « Re-run jobs » rejoue l'ANCIEN commit."
        )
    sys.exit(1)
print("OK : fichiers attendus présents, aucun secret, aucune règle de motion violée.")
