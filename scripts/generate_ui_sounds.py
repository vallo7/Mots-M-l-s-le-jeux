#!/usr/bin/env python3
"""Synthétise les sons d'interface (WAV 16 bits mono 32 kHz) dans app/src/main/assets/audio/ui/.

Sons originaux générés par ce script (aucune ressource tierce). Attaque/fin adoucies : pas de clic ni de coupure brutale,
pic normalisé à -4 dBFS pour éviter toute saturation.
    python3 scripts/generate_ui_sounds.py
"""
import wave
from pathlib import Path

import numpy as np

SR = 32000
OUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/audio/ui"


def env(n, attack=0.004, release=0.02, decay=6.0):
    t = np.arange(n) / SR
    e = np.exp(-decay * t / (n / SR))
    a = np.minimum(1.0, t / max(attack, 1e-6))
    r = np.minimum(1.0, (n / SR - t) / max(release, 1e-6))
    return e * a * r


def bell(freq, dur, amp=1.0, decay=7.0, harmonics=((2, 0.35), (3, 0.12))):
    n = int(SR * dur)
    t = np.arange(n) / SR
    w = np.sin(2 * np.pi * freq * t)
    for mult, level in harmonics:
        w += level * np.sin(2 * np.pi * freq * mult * t)
    return amp * w * env(n, decay=decay)


def place(buf, sig, start):
    i = int(SR * start)
    end = min(len(buf), i + len(sig))
    buf[i:end] += sig[: end - i]


def mix(total, parts):
    buf = np.zeros(int(SR * total))
    for start, sig in parts:
        place(buf, sig, start)
    return buf


def glide(f0, f1, dur, amp=1.0, decay=3.0):
    n = int(SR * dur)
    freq = np.linspace(f0, f1, n)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    return amp * np.sin(phase) * env(n, decay=decay, release=0.03)


def swoosh(dur, f0, f1, amp=1.0, seed=1):
    """Souffle : bruit passe-bas dont la fréquence de coupure balaie de f0 à f1, enveloppe en cloche."""
    n = int(SR * dur)
    x = np.random.default_rng(seed).standard_normal(n)
    cutoff = np.linspace(f0, f1, n)
    alpha = 1.0 - np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros(n)
    acc = 0.0
    for i in range(n):
        acc += alpha[i] * (x[i] - acc)
        y[i] = acc
    t = np.linspace(0, 1, n)
    return amp * y * np.sin(np.pi * t) ** 2


C5, D5, E5, G5, C6 = 523.25, 587.33, 659.25, 783.99, 1046.5
SOUNDS = {
    "click": mix(0.09, [(0, bell(1250, 0.09, 1.0, decay=9, harmonics=((2, 0.25),)))]),
    "select_cell": mix(0.06, [(0, bell(740, 0.06, 1.0, decay=8, harmonics=((2, 0.2),)))]),
    "correct": mix(0.5, [(0.00, bell(C5, 0.3, 0.8)), (0.09, bell(E5, 0.3, 0.8)), (0.18, bell(G5, 0.32, 0.9))]),
    "wrong": mix(0.2, [(0, glide(196, 160, 0.18, 0.7, decay=4))]),
    "hint": mix(0.4, [(0.00, bell(1318.5, 0.2, 0.7, decay=9)), (0.07, bell(1568, 0.2, 0.7, decay=9)), (0.14, bell(1975.5, 0.25, 0.8, decay=9))]),
    "gift": mix(0.8, [(0.0, bell(C5, 0.4, 0.7)), (0.08, bell(D5, 0.4, 0.7)), (0.16, bell(E5, 0.4, 0.7)),
                      (0.24, bell(G5, 0.4, 0.8)), (0.34, bell(C6, 0.5, 0.9))]),
    "progress_up": mix(0.5, [(0, glide(420, 900, 0.32, 0.7, decay=2.5)), (0.3, bell(1175, 0.2, 0.7, decay=8))]),
    "victory": mix(1.3, [(0.00, bell(C5, 0.9, 0.7, decay=4)), (0.12, bell(E5, 0.9, 0.7, decay=4)), (0.24, bell(G5, 0.9, 0.75, decay=4)),
                         (0.36, bell(C6, 1.0, 0.85, decay=4)), (0.52, bell(1568, 0.7, 0.5, decay=6))]),
    "menu_change": mix(0.16, [(0, glide(480, 820, 0.14, 0.45, decay=4))]),
    # Mélange des lettres : trois souffles qui s'éclaircissent puis une petite cloche de « retombée ».
    "shuffle": mix(1.0, [(0.00, swoosh(0.30, 500, 2600, 1.0, seed=3)), (0.26, swoosh(0.30, 700, 3200, 0.9, seed=5)),
                         (0.52, swoosh(0.28, 900, 3800, 0.8, seed=8)), (0.70, bell(1318.5, 0.25, 0.45, decay=9)),
                         (0.78, bell(1760, 0.22, 0.4, decay=9))]),
    # Glissement de carrousel : deux petits tics montants, très doux.
    "swipe": mix(0.12, [(0.00, bell(880, 0.06, 0.8, decay=10, harmonics=((2, 0.15),))), (0.045, bell(1320, 0.07, 0.7, decay=10, harmonics=((2, 0.15),)))]),
    "success_generic": mix(0.6, [(0.0, bell(G5, 0.35, 0.8, decay=6)), (0.14, bell(C6, 0.45, 0.9, decay=6))]),
}


def write(name, signal):
    peak = float(np.max(np.abs(signal))) or 1.0
    signal = signal / peak * (10 ** (-4 / 20))
    fade = int(SR * 0.015)
    signal[-fade:] *= np.linspace(1, 0, fade)
    pcm = (signal * 32767).astype("<i2")
    with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    print(f"{name}.wav  {len(pcm) / SR:.2f}s")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for name, sig in SOUNDS.items():
        write(name, sig)
