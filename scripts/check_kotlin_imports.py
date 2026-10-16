#!/usr/bin/env python3
"""Aide de développement (non bloquante en CI) : signale les symboles Compose/AndroidX courants utilisés sans import,
et les accolades/parenthèses déséquilibrées. Utile quand Gradle n'est pas disponible. Heuristique : peut se tromper."""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SYMBOLS = {
    "androidx.compose.runtime": "Composable remember mutableStateOf getValue setValue LaunchedEffect DisposableEffect rememberCoroutineScope rememberUpdatedState CompositionLocalProvider compositionLocalOf staticCompositionLocalOf derivedStateOf State".split(),
    "androidx.compose.ui": "Modifier Alignment".split(),
    "androidx.compose.ui.draw": "clip".split(),
    "androidx.compose.ui.graphics": "Color graphicsLayer TransformOrigin StrokeCap".split(),
    "androidx.compose.ui.platform": "LocalContext LocalDensity LocalView LocalConfiguration testTag".split(),
    "androidx.compose.ui.res": "painterResource stringResource".split(),
    "androidx.compose.ui.semantics": "semantics contentDescription stateDescription heading Role clearAndSetSemantics liveRegion LiveRegionMode".split(),
    "androidx.compose.ui.text.style": "TextAlign TextOverflow TextDecoration".split(),
    "androidx.compose.ui.text.font": "FontWeight".split(),
    "androidx.compose.ui.unit": "dp sp Dp Density".split(),
    "androidx.compose.material3": "Text Icon MaterialTheme".split(),
    "androidx.compose.foundation": "background border clickable Canvas Image verticalScroll rememberScrollState".split(),
    "androidx.compose.foundation.layout": "Box Column Row Spacer fillMaxSize fillMaxWidth fillMaxHeight height width size padding offset weight defaultMinSize heightIn windowInsetsPadding WindowInsets safeDrawing requiredSize Arrangement PaddingValues BoxScope ColumnScope BoxWithConstraints".split(),
    "androidx.compose.foundation.shape": "RoundedCornerShape CircleShape".split(),
    "androidx.compose.animation.core": "tween spring snap keyframes Animatable animateFloatAsState animateDpAsState rememberInfiniteTransition infiniteRepeatable animateFloat Spring LinearEasing FastOutSlowInEasing RepeatMode".split(),
    "androidx.compose.animation": "AnimatedContent AnimatedVisibility slideInHorizontally slideOutHorizontally slideInVertically slideOutVertically togetherWith".split(),
    "androidx.compose.foundation.lazy": "LazyColumn items rememberLazyListState".split(),
    "androidx.compose.ui.input.pointer": "pointerInput".split(),
    "androidx.compose.foundation.gestures": "awaitEachGesture awaitFirstDown detectTapGestures".split(),
    "kotlinx.coroutines": "launch delay withContext Dispatchers CoroutineScope SupervisorJob Job isActive NonCancellable cancel".split(),
    "kotlinx.coroutines.flow": "MutableStateFlow StateFlow asStateFlow update".split(),
}
bad = False
for path in list((ROOT / "app/src/main/java").rglob("*.kt")) + list((ROOT / "app/src/test").rglob("*.kt")):
    text = path.read_text(encoding="utf-8")
    body = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    body = re.sub(r"(?m)^\s*//.*$|(?<=\s)//.*$", "", body)
    body = re.sub(r'"(?:\\.|[^"\\])*"', '""', body)
    body = re.sub(r"'(?:\\.|[^'\\])'", "''", body)
    # un symbole utilisé comme argument nommé (nom = valeur) n'est pas un appel de fonction
    body = re.sub(r"\b(width|background|size|height|padding|color)\s*=(?!=)", "NAMED=", body)
    for open_c, close_c in ("{}", "()", "[]"):
        if body.count(open_c) != body.count(close_c):
            print(f"{path.relative_to(ROOT)} : {open_c}{close_c} déséquilibré ({body.count(open_c)} vs {body.count(close_c)})")
            bad = True
    imports = set(re.findall(r"^import\s+([\w.*]+)", text, flags=re.M))
    declared = set(re.findall(r"\b(?:fun|class|object|val|var|interface)\s+(?:[\w<>.]+\.)?(\w+)", text))
    for pkg, names in SYMBOLS.items():
        for name in names:
            if name in declared:
                continue
            if re.search(rf"(?<![\w.]){re.escape(name)}(?![\w])", body) and f"{pkg}.{name}" not in imports and f"{pkg}.*" not in imports:
                # qualifié en ligne (pkg.Name) ?
                if re.search(rf"{re.escape(pkg)}\.{name}", body):
                    continue
                # membre d'un scope (ex: Modifier.weight dans RowScope) : on ne signale que les symboles de haut niveau fréquents
                print(f"{path.relative_to(ROOT)} : « {name} » utilisé sans import ({pkg})")
                bad = True
sys.exit(1 if bad else 0)
