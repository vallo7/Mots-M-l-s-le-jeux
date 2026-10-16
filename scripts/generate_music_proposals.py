#!/usr/bin/env python3
"""Synthétise trois propositions de musique d'ambiance en boucle (originales, aucune ressource tierce).

    A  « Sunny Marimba »   — 100 BPM, do majeur, marimba en arpèges + pad chaud + shaker : joyeux, énergique doux.
    B  « Lo-fi Puzzle »    —  78 BPM, ii-V-I-vi jazzy, piano électrique + basse + batterie feutrée : concentré, cosy.
    C  « Music Box Dream » —  84 BPM, fa majeur, boîte à musique + cordes planantes : calme, onirique.

Chaque boucle dure 16 mesures et se raccorde sans coupure : la réverbération est calculée de façon circulaire
(sa queue retombe sur le début), et la durée est un nombre entier de mesures.

Sorties : assets/audio/music_proposals/*.ogg  (hors APK) ; le morceau livré par défaut est copié vers
          app/src/main/assets/audio/music/bgm_main.ogg (option --install A|B|C).

    pip install numpy   &&   ffmpeg requis (libvorbis)
    python3 scripts/generate_music_proposals.py            # génère les 3 propositions
    python3 scripts/generate_music_proposals.py --install A
"""
import shutil
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

SR = 44100
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "assets/audio/music_proposals"
INSTALL = ROOT / "app/src/main/assets/audio/music/bgm_main.ogg"
rng = np.random.default_rng(2026)


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


class Track:
    """Piste stéréo mixée à une position en secondes (les notes qui dépassent la fin reviennent au début : boucle)."""

    def __init__(self, seconds):
        self.n = int(round(seconds * SR))
        self.buf = np.zeros((self.n, 2))

    def add(self, sig, start, pan=0.0, gain=1.0):
        i = int(round(start * SR)) % self.n
        l = np.cos((pan + 1) * np.pi / 4) * gain
        r = np.sin((pan + 1) * np.pi / 4) * gain
        end = i + len(sig)
        if end <= self.n:
            self.buf[i:end, 0] += sig * l
            self.buf[i:end, 1] += sig * r
        else:
            k = self.n - i
            self.buf[i:, 0] += sig[:k] * l
            self.buf[i:, 1] += sig[:k] * r
            rest = sig[k:]
            m = min(len(rest), self.n)
            self.buf[:m, 0] += rest[:m] * l
            self.buf[:m, 1] += rest[:m] * r


def env_ad(n, attack=0.003, decay=6.0):
    t = np.arange(n) / SR
    return np.minimum(1.0, t / attack) * np.exp(-decay * t)


def marimba(freq, dur, vel=1.0):
    n = int(SR * dur)
    t = np.arange(n) / SR
    w = np.sin(2 * np.pi * freq * t) + 0.75 * np.sin(2 * np.pi * freq * 4.0 * t) * np.exp(-22 * t) + 0.30 * np.sin(2 * np.pi * freq * 9.2 * t) * np.exp(-45 * t)
    return vel * w * env_ad(n, 0.002, 7.0)


def music_box(freq, dur, vel=1.0):
    n = int(SR * dur)
    t = np.arange(n) / SR
    w = np.sin(2 * np.pi * freq * t) + 0.5 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-9 * t) + 0.25 * np.sin(2 * np.pi * freq * 5.4 * t) * np.exp(-18 * t)
    return vel * w * env_ad(n, 0.001, 3.6)


def epiano(freq, dur, vel=1.0):
    n = int(SR * dur)
    t = np.arange(n) / SR
    index = 1.6 * np.exp(-4.5 * t)
    w = np.sin(2 * np.pi * freq * t + index * np.sin(2 * np.pi * freq * t))
    w += 0.35 * np.sin(2 * np.pi * freq * 2 * t + 0.6 * index * np.sin(2 * np.pi * freq * 14 * t)) * np.exp(-7 * t)
    return vel * w * env_ad(n, 0.004, 2.8)


def pad(freqs, dur, vel=1.0, attack=0.6, release=0.8):
    n = int(SR * dur)
    t = np.arange(n) / SR
    w = np.zeros(n)
    for f in freqs:
        for detune in (-0.0035, 0.0, 0.0035):
            w += np.sin(2 * np.pi * f * (1 + detune) * t + rng.uniform(0, 6.28))
            w += 0.25 * np.sin(2 * np.pi * 2 * f * (1 + detune) * t + rng.uniform(0, 6.28))
    a = np.minimum(1.0, t / attack)
    r = np.minimum(1.0, (dur - t) / release)
    return vel * w / (len(freqs) * 3) * a * r


def bass(freq, dur, vel=1.0):
    n = int(SR * dur)
    t = np.arange(n) / SR
    w = np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(2 * np.pi * freq * 2 * t)
    return vel * w * env_ad(n, 0.006, 3.2)


def kick(vel=1.0):
    n = int(SR * 0.22)
    t = np.arange(n) / SR
    f = 46 + 90 * np.exp(-28 * t)
    return vel * np.sin(2 * np.pi * np.cumsum(f) / SR) * env_ad(n, 0.001, 14)


def noise_hit(dur, hp=0.9, vel=1.0, decay=40):
    n = int(SR * dur)
    x = rng.standard_normal(n)
    x = x - hp * np.concatenate(([0], x[:-1]))          # passe-haut grossier
    return vel * x * env_ad(n, 0.001, decay)


def reverb(buf, seconds=1.4, wet=0.28):
    """Réverbération circulaire (FFT) : la queue se raccorde au début, la boucle reste continue."""
    n = buf.shape[0]
    out = np.zeros_like(buf)
    for ch in range(2):
        ir_n = int(SR * seconds)
        t = np.arange(ir_n) / SR
        ir = rng.standard_normal(ir_n) * np.exp(-3.4 * t / seconds * 2)
        ir[: int(0.012 * SR)] *= np.linspace(0, 1, int(0.012 * SR))
        ir /= np.sqrt(np.sum(ir ** 2))
        irp = np.zeros(n)
        irp[: len(ir)] = ir
        out[:, ch] = np.fft.irfft(np.fft.rfft(buf[:, ch]) * np.fft.rfft(irp), n)
    return buf * (1 - wet * 0.5) + out * wet


def finalize(buf, target_db=-3.0):
    # Petit filtre passe-haut (retire le continu), normalisation crête, limitation douce.
    buf = buf - buf.mean(axis=0)
    peak = np.max(np.abs(buf)) or 1.0
    buf = buf / peak * (10 ** (target_db / 20))
    return np.tanh(buf * 1.1) / np.tanh(1.1)


def write_ogg(name, buf):
    OUT.mkdir(parents=True, exist_ok=True)
    pcm = (np.clip(buf, -1, 1) * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "t.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        target = OUT / name
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "3", str(target)], check=True)
    print(f"{name}  {buf.shape[0] / SR:.1f}s  {target.stat().st_size / 1024:.0f} Ko")
    return target


# ------------------------------------------------------------------------------------------------------ A : Sunny Marimba
def proposal_a():
    bpm, bars = 100, 16
    beat = 60 / bpm
    bar = 4 * beat
    tr = Track(bars * bar)
    chords = [([60, 64, 67, 72], 36), ([57, 60, 64, 69], 33), ([53, 57, 60, 65], 41), ([55, 59, 62, 67], 43)]   # C Am F G
    arp = [0, 1, 2, 3, 2, 1, 2, 1]
    motif = {  # mélodie pentatonique (mesures 5-8 et 13-16)
        0: [76, None, 79, None, 81, 79, None, 76],
        1: [None, 79, 76, None, 74, None, 72, None],
        2: [77, None, 81, None, 84, 81, None, 77],
        3: [79, 77, None, 76, None, 74, None, None],
    }
    for b in range(bars):
        notes, root = chords[b % 4]
        t0 = b * bar
        tr.add(pad([midi(n) for n in notes[:3]], bar + 0.4, 0.5), t0, 0.0, 0.45)
        tr.add(bass(midi(root + 12), beat * 1.8, 0.9), t0, 0.0, 0.42)
        tr.add(bass(midi(root + (7 if b % 2 else 0) + 12), beat * 1.8, 0.7), t0 + 2 * beat, 0.0, 0.34)
        for k, idx in enumerate(arp):
            tr.add(marimba(midi(notes[idx % 4]), 0.6, 0.55), t0 + k * beat / 2, pan=-0.35 + 0.1 * (k % 4), gain=0.8)
        if (b // 4) % 2 == 1:
            for k, m in enumerate(motif[b % 4]):
                if m is not None:
                    tr.add(marimba(midi(m), 0.8, 0.9), t0 + k * beat / 2, pan=0.3, gain=0.7)
        for k in range(8):                                    # shaker sur les contretemps
            if k % 2 == 1:
                tr.add(noise_hit(0.05, 0.9, 0.35, 70), t0 + k * beat / 2, pan=0.4, gain=0.35)
        if b % 2 == 0:                                         # petit tom doux sur le premier temps
            tr.add(kick(0.55), t0, 0.0, 0.25)
    return finalize(reverb(tr.buf, 1.3, 0.26))


# ------------------------------------------------------------------------------------------------------ B : Lo-fi Puzzle
def proposal_b():
    bpm, bars = 78, 16
    beat = 60 / bpm
    bar = 4 * beat
    tr = Track(bars * bar)
    chords = [([50, 57, 60, 64, 65], 38), ([43, 53, 59, 64, 69], 31), ([48, 55, 59, 62, 64], 36), ([45, 55, 60, 64, 67], 33)]   # Dm9 G13 Cmaj9 Am7
    swing = 0.14 * beat
    for b in range(bars):
        notes, root = chords[(b // 2) % 4]
        t0 = b * bar
        for hit in (0.0, 1.5 * beat + swing, 2.5 * beat + swing):                       # comping piano électrique
            tr.add(sum(epiano(midi(n + 12), 1.6, 0.5) for n in notes), t0 + hit, pan=-0.2, gain=0.5)
        tr.add(bass(midi(root + 12), beat * 1.6, 0.95), t0, 0.0, 0.36)
        tr.add(bass(midi(root + 19), beat * 1.2, 0.8), t0 + 2 * beat, 0.0, 0.28)
        tr.add(bass(midi(root + 24), beat * 0.9, 0.7), t0 + 3 * beat + swing, 0.0, 0.2)
        tr.add(kick(0.8), t0, 0.0, 0.3)
        tr.add(kick(0.6), t0 + 2.5 * beat + swing, 0.0, 0.22)
        tr.add(noise_hit(0.16, 0.7, 0.8, 22), t0 + beat, 0.1, 0.34)                      # caisse claire feutrée
        tr.add(noise_hit(0.16, 0.7, 0.8, 22), t0 + 3 * beat, 0.1, 0.34)
        for k in range(8):
            off = swing if k % 2 else 0.0
            tr.add(noise_hit(0.04, 0.97, 0.5, 90), t0 + k * beat / 2 + off, 0.35, 0.22 if k % 2 else 0.3)
        if b % 4 == 3:                                                                   # petite phrase de fin de cycle
            for k, m in enumerate([72, 71, 67, 69]):
                tr.add(epiano(midi(m), 1.0, 0.7), t0 + k * beat + swing * (k % 2), pan=0.3, gain=0.45)
    return finalize(reverb(tr.buf, 1.1, 0.2))


# ------------------------------------------------------------------------------------------------------ C : Music Box Dream
def proposal_c():
    bpm, bars = 84, 16
    beat = 60 / bpm
    bar = 4 * beat
    tr = Track(bars * bar)
    chords = [[53, 57, 60, 64], [50, 53, 57, 60], [58, 62, 65, 69], [48, 55, 60, 64]]   # Fmaj7 Dm7 Bbmaj7 C
    melody = [
        [81, None, 84, None, 81, None, 79, None],
        [77, None, 81, None, 79, None, 77, None],
        [86, None, 84, None, 81, None, 84, None],
        [79, None, 76, None, 77, None, None, None],
    ]
    for b in range(bars):
        notes = chords[b % 4]
        t0 = b * bar
        tr.add(pad([midi(n - 12) for n in notes], bar + 0.8, 0.8, attack=1.0, release=1.2), t0, 0.0, 0.55)
        tr.add(pad([midi(n) for n in notes[1:]], bar + 0.8, 0.5, attack=1.2, release=1.2), t0, 0.2, 0.3)
        for k in range(8):                                                                # arpège de boîte à musique
            tr.add(music_box(midi(notes[(k * 2) % 4] + 12), 1.4, 0.55), t0 + k * beat / 2, pan=-0.3 + 0.08 * k, gain=0.5)
        if (b // 4) % 2 == 1:
            for k, m in enumerate(melody[b % 4]):
                if m is not None:
                    tr.add(music_box(midi(m), 1.8, 0.9), t0 + k * beat / 2, pan=0.35, gain=0.55)
        tr.add(bass(midi(notes[0] - 12), beat * 3.5, 0.8), t0, 0.0, 0.32)
    return finalize(reverb(tr.buf, 1.8, 0.34))


PROPOSALS = {"A": ("music_a_sunny_marimba.ogg", proposal_a), "B": ("music_b_lofi_puzzle.ogg", proposal_b), "C": ("music_c_music_box_dream.ogg", proposal_c)}

if __name__ == "__main__":
    wanted = sys.argv[sys.argv.index("--install") + 1].upper() if "--install" in sys.argv else None
    built = {}
    for key, (filename, fn) in PROPOSALS.items():
        built[key] = write_ogg(filename, fn())
    if wanted:
        INSTALL.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(built[wanted], INSTALL)
        print(f"Installé : {INSTALL.relative_to(ROOT)} <- {built[wanted].name}")
