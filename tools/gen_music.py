"""
An original boss-battle loop for the mod, synthesized from scratch: D minor, 140 bpm, 16 bars.
Strings hammer an eighth-note ostinato over a i-VII-VI-V progression, war drums pound underneath,
and brass carries a melody that climbs an octave in the second half. Rendered three times over and
the middle pass is kept, so the reverb tails carry across the loop point and it repeats seamlessly.
"""
import numpy as np

import gen_sounds as S
from gen_sounds import SR, place, lp, hp, bp, env, brass, timpani, cymbal, reverb, norm, write

BPM = 140
BEAT = 60 / BPM
BAR = 4 * BEAT
BARS = 16
LOOP = BARS * BAR
rng = np.random.default_rng(140)


def hz(note):
    names = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "Ab": 8,
             "A": 9, "A#": 10, "Bb": 10, "B": 11}
    n, o = note[:-1], int(note[-1])
    return 440.0 * 2 ** ((names[n] + 12 * (o + 1) - 69) / 12)


def strings(f, dur, accent=1.0):
    """Short bowed note: three detuned saws, low passed, quick attack, clipped release."""
    t = np.arange(int(dur * SR)) / SR
    s = np.zeros_like(t)
    for det in (-0.12, 0.0, 0.13):
        ph = (f * (1 + det / 100) * t + rng.uniform()) % 1.0
        s += 2 * ph - 1
    s = lp(s / 3, 2200)
    return s * env(len(t), a=0.008, d=0.06, s=0.55, r=min(0.06, dur * 0.4)) * accent


def taiko(big=True):
    t = np.arange(int(0.9 * SR)) / SR
    f0 = 58 if big else 92
    ph = 2 * np.pi * np.cumsum(f0 * (1 + 0.6 * np.exp(-t / 0.03))) / SR
    body = np.sin(ph) * np.exp(-t / (0.35 if big else 0.18))
    skin = lp(rng.standard_normal(len(t)), 900) * np.exp(-t / 0.015)
    return (body + 0.6 * skin) * (1.0 if big else 0.55)


def snare():
    t = np.arange(int(0.3 * SR)) / SR
    noise = bp(rng.standard_normal(len(t)), 1500, 7000) * np.exp(-t / 0.07)
    tone = np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.04)
    return (noise * 0.7 + tone * 0.4) * 0.5


# i - VII - VI - V in D minor: root and the arpeggio the strings saw on
CHORDS = [("D3", "A2", "F3"), ("C3", "G2", "E3"), ("Bb2", "F2", "D3"), ("A2", "E2", "C#3")]
# melody: (bar, beat, note, beats)
MELODY_A = [(0, 0, "D4", 3), (0, 3, "F4", 1), (1, 0, "E4", 2), (1, 2, "C4", 2),
            (2, 0, "D4", 3), (2, 3, "Bb3", 1), (3, 0, "A3", 4),
            (4, 0, "A4", 2), (4, 2, "F4", 1), (4, 3, "G4", 1), (5, 0, "E4", 3), (5, 3, "C4", 1),
            (6, 0, "F4", 1), (6, 1, "E4", 1), (6, 2, "D4", 1), (6, 3, "Bb3", 1), (7, 0, "C#4", 4)]
MELODY_B = [(8 + b, beat, n[:-1] + str(int(n[-1]) + 1), d) for b, beat, n, d in MELODY_A[:7]] + \
           [(12, 0, "D5", 2), (12, 2, "A4", 2), (13, 0, "C5", 2), (13, 2, "G4", 2),
            (14, 0, "Bb4", 1), (14, 1, "A4", 1), (14, 2, "G4", 1), (14, 3, "F4", 1), (15, 0, "E4", 2), (15, 2, "C#5", 2)]


def one_pass():
    buf = np.zeros(int(LOOP * SR) + SR * 2)
    for bar in range(BARS):
        root, low, third = CHORDS[bar % 4]
        pattern = [root, root, low, root, third, root, low, root]
        t0 = bar * BAR
        for i, n in enumerate(pattern):
            acc = 1.0 if i % 2 == 0 else 0.7
            place(buf, strings(hz(n), BEAT / 2 * 0.9, acc) * 0.32, t0 + i * BEAT / 2)
            if bar >= 8:   # second half: violas an octave up join in
                place(buf, strings(hz(n) * 2, BEAT / 2 * 0.85, acc) * 0.16, t0 + i * BEAT / 2)
        # drums: big on 1 and 3, small on the and-of-2 and on 4-and
        place(buf, taiko(True) * 0.9, t0)
        place(buf, taiko(False) * 0.7, t0 + 1.5 * BEAT)
        place(buf, taiko(True) * 0.8, t0 + 2 * BEAT)
        place(buf, taiko(False) * 0.6, t0 + 3 * BEAT)
        place(buf, taiko(False) * 0.6, t0 + 3.5 * BEAT)
        if bar >= 4:
            place(buf, snare() * 0.6, t0 + BEAT)
            place(buf, snare() * 0.6, t0 + 3 * BEAT)
        if bar % 4 == 3:   # fill into the next phrase
            for k in range(4):
                place(buf, snare() * (0.3 + k * 0.1), t0 + 3 * BEAT + k * BEAT / 4)
        if bar % 8 == 0:
            place(buf, cymbal(2.2) * 1.4, t0)
        # brass chord pads in the second half
        if bar >= 8:
            for n in (root, third):
                place(buf, brass(hz(n) * 2, BAR * 0.95, bright=0.6, attack=0.08) * 0.07, t0)
    for bar, beat, n, beats in MELODY_A + MELODY_B:
        dur = beats * BEAT * 0.95
        place(buf, brass(hz(n), dur, bright=0.85, attack=0.03) * 0.3, bar * BAR + beat * BEAT)
        if bar >= 8:   # doubled a fifth below for weight
            place(buf, brass(hz(n) * 2 / 3, dur, bright=0.7, attack=0.04) * 0.14, bar * BAR + beat * BEAT)
    return buf


def main():
    p = one_pass()
    n = int(LOOP * SR)
    three = np.zeros(n * 3 + len(p))
    for k in range(3):
        three[k * n:k * n + len(p)] += p
    wet = reverb(three, 0.22, 0.8)
    loop = wet[n:2 * n]
    loop = hp(loop, 35)
    write("boss_theme", norm(loop, 0.8))


if __name__ == "__main__":
    main()
