"""
Three music discs, synthesized from scratch (no samples):

  disc_sailor   "Drunken Sailor" (traditional sea shanty, public domain): concertina, fiddle, plucked
                bass, foot stomps and claps, three times round with the band building up.
  disc_jig      "The Ruby Jig" (original): a 6/8 jig on fiddle and concertina over bodhran.
  disc_kraken   "The Kraken's Lullaby" (original): a slow 3/4 waltz on harp and low strings with the
                sea breathing underneath.
"""
import numpy as np

from gen_sounds import SR, place, lp, hp, bp, env, reverb, norm, write, fade_out

rng = np.random.default_rng(1720)
NAMES = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "Ab": 8,
         "A": 9, "A#": 10, "Bb": 10, "B": 11}


def hz(note):
    n, o = note[:-1], int(note[-1])
    return 440.0 * 2 ** ((NAMES[n] + 12 * (o + 1) - 69) / 12)


def seq(text):
    """'A4:1 A4:.5 r:1 ...' -> [(note or None, beats)]."""
    out = []
    for tok in text.split():
        n, b = tok.split(":")
        out.append((None if n == "r" else n, float(b)))
    return out


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


# --------------------------------------------------------------- instruments

def concertina(f, dur, amp=1.0):
    t = t_axis(dur + 0.06)
    trem = 1 + 0.06 * np.sin(2 * np.pi * 5.2 * t)
    s = np.zeros_like(t)
    for det in (-0.25, 0.25):
        ph = 2 * np.pi * f * (1 + det / 100) * t
        for h in range(1, 12):
            s += (np.sin(h * np.pi * 0.32) / h) * np.sin(h * ph)  # pulse wave, 32% duty
    s = lp(s * trem, 3800) * 0.25
    return s * env(len(t), a=0.025, d=0.05, s=0.85, r=0.06) * amp


def fiddle(f, dur, amp=1.0):
    t = t_axis(dur + 0.08)
    vib = 1 + 0.005 * np.sin(2 * np.pi * 5.8 * t) * np.clip((t - 0.12) / 0.2, 0, 1)
    ph = 2 * np.pi * np.cumsum(f * vib) / SR
    s = sum(np.sin(h * ph) / h ** 1.1 for h in range(1, 16))
    bow = bp(rng.standard_normal(len(t)), 2000, 7000) * 0.04
    s = lp(s + bow, 5000) * 0.35
    s = bp(s, 250, 6000) * 1.2   # a little body resonance
    return s * env(len(t), a=0.04, d=0.08, s=0.8, r=0.08) * amp


def pluck(f, dur, amp=1.0, bright=0.15):
    """Karplus-Strong string, computed one period at a time."""
    n = int((dur + 0.6) * SR)
    period = max(2, int(round(SR / f)))
    burst = rng.uniform(-1, 1, period)
    burst = lp(burst, 1500 + bright * 6000) if period > 12 else burst
    y = np.zeros(n + period + 1)
    y[:period] = burst
    decay = 0.996
    i = period
    while i < n:
        j = min(n, i + period)
        y[i:j] = decay * 0.5 * (y[i - period:j - period] + y[i - period - 1:j - period - 1] if i > period else
                                y[i - period:j - period] + np.concatenate([[0], y[i - period:j - period - 1]]))
        i = j
    y = y[:n]
    y = lp(y, min(5000, f * 10))
    return y * env(n, a=0.002, d=0.01, s=1.0, r=0.25) * amp * 1.1


def harp(f, dur, amp=1.0):
    t = t_axis(dur + 1.5)
    s = np.sin(2 * np.pi * f * t) * np.exp(-t / 1.4) + 0.4 * np.sin(4 * np.pi * f * t) * np.exp(-t / 0.6) \
        + 0.15 * np.sin(6 * np.pi * f * t) * np.exp(-t / 0.3)
    return s * env(len(t), a=0.003, d=0.01, s=1, r=0.3) * amp * 0.4


def cello(f, dur, amp=1.0):
    t = t_axis(dur + 0.3)
    vib = 1 + 0.004 * np.sin(2 * np.pi * 5 * t)
    ph = 2 * np.pi * np.cumsum(f * vib) / SR
    s = sum(np.sin(h * ph) / h ** 1.4 for h in range(1, 10))
    return lp(s, 1500) * env(len(t), a=0.25, d=0.1, s=0.8, r=0.3) * amp * 0.3


def stomp():
    t = t_axis(0.4)
    ph = 2 * np.pi * np.cumsum(70 * (1 + 1.5 * np.exp(-t / 0.01))) / SR
    wood = bp(rng.standard_normal(len(t)), 150, 900) * np.exp(-t / 0.03)
    return (np.sin(ph) * np.exp(-t / 0.09) + wood * 0.8) * 0.8


def clap():
    t = t_axis(0.25)
    s = np.zeros_like(t)
    for k in range(3):
        o = int(k * 0.008 * SR)
        s[o:] += bp(rng.standard_normal(len(t) - o), 900, 4000) * np.exp(-t[:len(t) - o] / (0.01 if k < 2 else 0.05))
    return s * 0.25


def bodhran(low=True):
    t = t_axis(0.5)
    f0 = 85 if low else 140
    ph = 2 * np.pi * np.cumsum(f0 * (1 + 0.5 * np.exp(-t / 0.02))) / SR
    skin = bp(rng.standard_normal(len(t)), 200, 2500) * np.exp(-t / 0.02)
    return (np.sin(ph) * np.exp(-t / (0.2 if low else 0.1)) + 0.5 * skin) * (0.8 if low else 0.5)


def jingle():
    t = t_axis(0.2)
    return hp(rng.standard_normal(len(t)), 6000) * np.exp(-t / 0.05) * 0.18


def play(buf, notes, start, beat, inst, amp=1.0, transpose=1.0, gap=0.92):
    at = start
    for n, b in notes:
        if n is not None:
            place(buf, inst(hz(n) * transpose, b * beat * gap, amp), at)
        at += b * beat
    return at


# ------------------------------------------------------------- Drunken Sailor

SAILOR_VERSE = seq("""
A4:1 A4:.5 A4:.5 A4:1 A4:.5 A4:.5 A4:1 D4:1 F4:1 A4:1
G4:1 G4:.5 G4:.5 G4:1 G4:.5 G4:.5 G4:1 C4:1 E4:1 G4:1
A4:1 A4:.5 A4:.5 A4:1 A4:.5 A4:.5 A4:1 B4:1 C5:1 D5:1
C5:1 A4:1 G4:1 E4:1 D4:2 D4:2
""")
SAILOR_CHORUS = seq("""
A4:2 A4:1 A4:1 A4:1 D4:1 F4:1 A4:1
G4:2 G4:1 G4:1 G4:1 C4:1 E4:1 G4:1
A4:2 A4:1 A4:1 A4:1 B4:1 C5:1 D5:1
C5:1 A4:1 G4:1 E4:1 D4:2 D4:2
""")
SAILOR_CHORDS = ["D", "D", "C", "C", "D", "D", "C", "D"]
ROOT = {"D": ("D2", "A2"), "C": ("C2", "G2"), "A": ("A1", "E2"), "G": ("G1", "D2"), "F": ("F1", "C2"),
        "E": ("E2", "B2"), "Bb": ("Bb1", "F2")}


def disc_sailor():
    beat = 60 / 132
    bar = 4 * beat
    rounds = 3
    length = 4 * bar + rounds * 16 * bar + 4 * bar
    buf = np.zeros(int((length + 3) * SR))
    # intro: stomps and claps
    t = 0.0
    for b in range(4):
        place(buf, stomp(), t + b * bar)
        place(buf, stomp(), t + b * bar + 2 * beat)
        if b >= 2:
            place(buf, clap(), t + b * bar + beat)
            place(buf, clap(), t + b * bar + 3 * beat)
    t = 4 * bar
    for r in range(rounds):
        start = t
        for half, melody in enumerate((SAILOR_VERSE, SAILOR_CHORUS)):
            s0 = start + half * 8 * bar
            play(buf, melody, s0, beat, concertina, 0.9)
            if r >= 1:
                play(buf, melody, s0, beat, fiddle, 0.55, transpose=2.0 if r == 2 else 1.0)
            if r == 2:   # a third below on concertina for harmony
                play(buf, melody, s0, beat, concertina, 0.35, transpose=2 ** (-4 / 12))
            for b, ch in enumerate(SAILOR_CHORDS):
                bt = s0 + b * bar
                lo, fifth = ROOT[ch]
                for k, n in enumerate((lo, fifth, lo, fifth)):
                    place(buf, pluck(hz(n), beat * 0.9, 0.9), bt + k * beat)
                place(buf, stomp(), bt)
                place(buf, stomp(), bt + 2 * beat)
                if r >= 1 or half == 1:
                    place(buf, clap(), bt + beat)
                    place(buf, clap(), bt + 3 * beat)
                if r == 2:
                    for k in range(8):
                        place(buf, jingle() * (1.0 if k % 2 else 0.6), bt + k * beat / 2)
        t += 16 * bar
    # outro: the last line again, slowing, and a final chord
    tail = seq("C5:1 A4:1 G4:1 E4:1 D4:4")
    play(buf, tail, t, beat * 1.25, concertina, 0.9)
    play(buf, tail, t, beat * 1.25, fiddle, 0.5)
    place(buf, pluck(hz("D2"), 3.0, 1.0), t + 4 * beat * 1.25)
    place(buf, stomp(), t + 4 * beat * 1.25)
    return norm(fade_out(reverb(buf, 0.2), 0.5), 0.85)


# ---------------------------------------------------------------- Ruby Jig

# 6/8: each bar = 6 eighths; beat unit below = one eighth
JIG_A = seq("""
G4:1 B4:1 D5:1 G5:2 D5:1  E5:1 C5:1 A4:1 B4:1 G4:1 E4:1
D4:1 G4:1 B4:1 D5:2 B4:1  C5:1 A4:1 F#4:1 A4:3
G4:1 B4:1 D5:1 G5:2 A5:1  B5:1 G5:1 E5:1 D5:1 B4:1 G4:1
A4:1 C5:1 E5:1 D5:1 B4:1 F#4:1  G4:3 G4:3
""")
JIG_B = seq("""
B5:2 A5:1 G5:2 E5:1  D5:1 E5:1 G5:1 A5:3
B5:2 A5:1 G5:1 A5:1 B5:1  A5:1 F#5:1 D5:1 E5:1 F#5:1 A5:1
B5:2 A5:1 G5:2 E5:1  D5:1 B4:1 G4:1 B4:1 D5:1 G5:1
E5:1 C5:1 A4:1 D5:1 F#4:1 A4:1  G4:3 G4:3
""")
JIG_CHORDS = ["G", "C", "G", "D", "G", "Em", "Am", "G"]
JIG_ROOT = {"G": "G2", "C": "C3", "D": "D3", "Em": "E2", "Am": "A2"}


def disc_jig():
    e = 60 / 116 / 2   # eighth at 116 dotted-quarter... brisk
    bar = 6 * e
    tunes = [JIG_A, JIG_A, JIG_B, JIG_B] * 2
    length = 2 * bar + len(tunes) * 8 * bar + 2 * bar
    buf = np.zeros(int((length + 3) * SR))
    for b in range(2):
        place(buf, bodhran(True), b * bar)
        place(buf, bodhran(False), b * bar + 3 * e)
        place(buf, bodhran(False), b * bar + 5 * e)
    t = 2 * bar
    for i, tune in enumerate(tunes):
        lead = fiddle if i % 2 == 0 else concertina
        play(buf, tune, t, e, lead, 0.75, gap=0.9)
        if i >= 4:
            play(buf, tune, t, e, concertina if lead is fiddle else fiddle, 0.4, transpose=0.5, gap=0.9)
        for b, ch in enumerate(JIG_CHORDS):
            bt = t + b * bar
            root = hz(JIG_ROOT[ch])
            place(buf, pluck(root, 2 * e, 0.8), bt)
            place(buf, pluck(root * 1.5, e, 0.5), bt + 2 * e)
            place(buf, pluck(root, 2 * e, 0.7), bt + 3 * e)
            place(buf, pluck(root * 2, e, 0.5), bt + 5 * e)
            place(buf, bodhran(True), bt)
            place(buf, bodhran(False), bt + 2 * e)
            place(buf, bodhran(True), bt + 3 * e)
            place(buf, bodhran(False), bt + 5 * e)
        t += 8 * bar
    place(buf, pluck(hz("G2"), 2.5, 1.0), t)
    place(buf, fiddle(hz("G4"), 1.5, 0.7), t)
    place(buf, bodhran(True), t)
    return norm(fade_out(reverb(buf, 0.18), 0.4), 0.85)


# ------------------------------------------------------------ Kraken's Lullaby

LULL_A = seq("""
E5:2 D5:1  C5:2 B4:1  A4:2 B4:1  C5:3
D5:2 C5:1  B4:2 G4:1  A4:3  E4:3
E5:2 D5:1  C5:2 B4:1  A4:2 C5:1  E5:3
F5:2 E5:1  D5:1 C5:1 B4:1  A4:3  A4:3
""")
LULL_B = seq("""
C5:2 D5:1  E5:2 G5:1  F5:2 E5:1  D5:3
B4:2 C5:1  D5:2 F5:1  E5:3  E5:3
A5:2 G5:1  F5:2 E5:1  D5:2 C5:1  B4:3
C5:1 B4:1 A4:1  G#4:2 B4:1  A4:3  A4:3
""")
LULL_CHORDS = ["Am", "Am", "F", "Am", "Dm", "G", "Am", "E",
               "Am", "Am", "F", "Am", "Dm", "E", "Am", "Am"]
LULL_NOTES = {"Am": ("A2", "E3", "A3", "C4"), "F": ("F2", "C3", "F3", "A3"), "Dm": ("D2", "A2", "D3", "F3"),
              "G": ("G2", "D3", "G3", "B3"), "E": ("E2", "B2", "E3", "G#3"), "C": ("C2", "G2", "C3", "E3")}
LULL_CHORDS_B = ["F", "C", "Dm", "G", "G", "C", "E", "E", "F", "C", "Dm", "Am", "Dm", "E", "Am", "Am"]


def sea_bed(sec):
    n = int(sec * SR)
    t = np.arange(n) / SR
    brown = np.cumsum(rng.standard_normal(n))
    brown = hp(brown, 25)
    brown /= np.max(np.abs(brown))
    return lp(brown, 400) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 7.0)) * 0.12


def disc_kraken():
    beat = 60 / 84
    bar = 3 * beat
    form = [(LULL_A, LULL_CHORDS), (LULL_B, LULL_CHORDS_B), (LULL_A, LULL_CHORDS)]
    length = 2 * bar + len(form) * 16 * bar + 3 * bar
    buf = np.zeros(int((length + 4) * SR))
    t = 0.0
    for b in range(2):   # harp intro arpeggio
        for k, n in enumerate(LULL_NOTES["Am"][1:]):
            place(buf, harp(hz(n), beat), t + b * bar + k * beat)
    t = 2 * bar
    for i, (melody, chords) in enumerate(form):
        play(buf, melody, t, beat, harp if i != 1 else fiddle, 0.9 if i != 1 else 0.5, gap=1.0 if i != 1 else 0.95)
        if i == 2:
            play(buf, melody, t, beat, harp, 0.4, transpose=0.5)
        for b, ch in enumerate(chords):
            notes = LULL_NOTES[ch]
            bt = t + b * bar
            place(buf, cello(hz(notes[0]), bar * 0.98, 0.9), bt)
            place(buf, harp(hz(notes[1]), beat, 0.45), bt + beat)
            place(buf, harp(hz(notes[2]), beat, 0.4), bt + beat)
            place(buf, harp(hz(notes[1]), beat, 0.4), bt + 2 * beat)
            place(buf, harp(hz(notes[3]), beat, 0.35), bt + 2 * beat)
        t += 16 * bar
    place(buf, cello(hz("A2"), 3 * bar, 0.9), t)
    for k, n in enumerate(("A3", "C4", "E4", "A4")):
        place(buf, harp(hz(n), 2.0, 0.6), t + k * beat * 0.5)
    out = reverb(buf, 0.35, 0.86)
    out[:len(out)] += sea_bed(len(out) / SR)[:len(out)]
    return norm(fade_out(out, 2.0), 0.8)


def main():
    write("disc_sailor", disc_sailor())
    write("disc_jig", disc_jig())
    write("disc_kraken", disc_kraken())


if __name__ == "__main__":
    main()
