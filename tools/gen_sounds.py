"""
Synthesizes the mod's own sound effects from scratch (numpy + scipy), then encodes them to mono Ogg
Vorbis with ffmpeg. Nothing is sampled from the game or anywhere else.

  goal_complete  ship's bell plus a rising glockenspiel arpeggio
  bounty_up      a deep harbour bell toll with a coin tail
  rank_up        a bugle call (natural harmonics only, like a real bugle)
  boss_defeat    brass fanfare over a timpani roll and a cymbal swell
  boss_horn      a low two-tone war horn for summons
  pact_bind      an eerie detuned shimmer with breathy noise
  coins          a handful of coins clinking on a counter (3 variants)
  sea_ambient    a seamless 24 s loop of waves for the Sundered Sea
  sea_additions  gulls and hull creaks that play now and then at sea
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "piratecrew", "sounds")
rng = np.random.default_rng(1715)


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


def env(n, a=0.005, d=0.1, s=0.6, r=0.2, hold=None):
    """ADSR over n samples; hold = sustain length in seconds (defaults to whatever fits)."""
    a_n, d_n, r_n = int(a * SR), int(d * SR), int(r * SR)
    h_n = n - a_n - d_n - r_n if hold is None else int(hold * SR)
    h_n = max(0, h_n)
    e = np.concatenate([np.linspace(0, 1, a_n, endpoint=False), np.linspace(1, s, d_n, endpoint=False),
                        np.full(h_n, s), np.linspace(s, 0, r_n)])
    if len(e) < n:
        e = np.concatenate([e, np.zeros(n - len(e))])
    return e[:n]


def lp(x, f, order=2):
    return sosfilt(butter(order, f, "low", fs=SR, output="sos"), x)


def hp(x, f, order=2):
    return sosfilt(butter(order, f, "high", fs=SR, output="sos"), x)


def bp(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def place(buf, sig, at):
    i = int(at * SR)
    end = min(len(buf), i + len(sig))
    buf[i:end] += sig[:end - i]


def reverb(x, wet=0.25, room=0.82):
    """Small Schroeder reverb: four combs into two allpasses. Good enough for a ship's hold."""
    out = np.zeros(len(x) + SR)
    xp = np.concatenate([x, np.zeros(SR)])
    for dly in (1557, 1617, 1491, 1422):
        y = np.zeros_like(xp)
        for start in range(0, len(xp), dly):
            seg = xp[start:start + dly].copy()
            if start >= dly:
                seg += room * y[start - dly:start - dly + len(seg)]
            y[start:start + len(seg)] = seg
        out += y
    out /= 4
    for dly, g in ((225, 0.7), (556, 0.7)):
        y = np.zeros_like(out)
        buf = np.zeros(dly)
        for i in range(0, len(out), dly):
            seg = out[i:i + dly]
            prev = y[i - dly:i - dly + len(seg)] if i >= dly else np.zeros(len(seg))
            prev_in = out[i - dly:i - dly + len(seg)] if i >= dly else np.zeros(len(seg))
            y[i:i + len(seg)] = -g * seg + prev_in + g * prev
        out = y
    res = np.concatenate([x, np.zeros(SR)]) * (1 - wet) + out * wet
    # trim trailing silence
    nz = np.nonzero(np.abs(res) > 1e-4)[0]
    return res[:nz[-1] + 1] if len(nz) else res


def norm(x, peak=0.89):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x


def fade_out(x, sec=0.05):
    n = min(len(x), int(sec * SR))
    x[-n:] *= np.linspace(1, 0, n)
    return x


# ----------------------------------------------------------------- instruments

BELL_PARTIALS = [(0.5, 1.0, 2.2), (1.0, 0.9, 1.6), (1.19, 0.55, 1.2), (1.56, 0.4, 0.9), (2.0, 0.45, 0.8),
                 (2.51, 0.25, 0.5), (2.66, 0.22, 0.45), (3.01, 0.18, 0.4), (4.1, 0.12, 0.25), (5.4, 0.08, 0.15)]


def bell(f, dur=2.5, bright=1.0):
    t = t_axis(dur)
    s = np.zeros_like(t)
    for ratio, amp, decay in BELL_PARTIALS:
        detune = 1 + rng.uniform(-0.002, 0.002)
        s += amp * (bright if ratio > 2 else 1) * np.sin(2 * np.pi * f * ratio * detune * t) * np.exp(-t / decay)
    strike = rng.standard_normal(len(t)) * np.exp(-t / 0.004) * 0.3
    return (s + hp(strike, 2000)) * env(len(t), a=0.001, d=0.01, s=1, r=0.05)


def glock(f, dur=0.9):
    t = t_axis(dur)
    s = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.35) + 0.35 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t / 0.08) \
        + 0.15 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t / 0.03)
    return s * env(len(t), a=0.001, d=0.01, s=1, r=0.05)


def brass(f, dur, vib=5.5, bright=0.8, attack=0.04):
    """Additive brass: harmonics whose upper partials swell in with the attack (the 'blat')."""
    t = t_axis(dur)
    vibr = 1 + 0.004 * np.sin(2 * np.pi * vib * t) * np.clip(t / 0.25, 0, 1)
    phase = 2 * np.pi * f * np.cumsum(vibr) / SR
    s = np.zeros_like(t)
    swell = np.clip(t / (attack * 2.5), 0, 1)
    for h in range(1, 14):
        amp = (1 / h ** (1.6 - bright * 0.6)) * (1 if h < 3 else swell)
        s += amp * np.sin(h * phase + rng.uniform(0, 6.28))
    s += 0.02 * rng.standard_normal(len(t)) * np.exp(-t / 0.05)
    return lp(s, min(9000, f * 9)) * env(len(t), a=attack, d=0.08, s=0.8, r=min(0.15, dur * 0.3))


def timpani(f, dur=1.4, amt=1.0):
    t = t_axis(dur)
    pitch = f * (1 + 0.08 * np.exp(-t / 0.05))
    ph = 2 * np.pi * np.cumsum(pitch) / SR
    s = np.sin(ph) * np.exp(-t / 0.6) + 0.5 * np.sin(1.5 * ph) * np.exp(-t / 0.3) + 0.3 * np.sin(1.99 * ph) * np.exp(-t / 0.2)
    thump = lp(rng.standard_normal(len(t)), 600) * np.exp(-t / 0.02)
    return (s + thump * 0.8) * amt


def cymbal(dur=2.5, swell=False):
    t = t_axis(dur)
    n = hp(rng.standard_normal(len(t)), 4500)
    e = np.clip(t / dur, 0, 1) ** 2 if swell else np.exp(-t / 0.8)
    return n * e * 0.14


def clink(f):
    t = t_axis(0.35)
    s = np.zeros_like(t)
    for ratio, amp, dec in ((1, 1, 0.09), (2.32, 0.6, 0.05), (4.25, 0.4, 0.03), (6.8, 0.25, 0.02)):
        s += amp * np.sin(2 * np.pi * f * ratio * t) * np.exp(-t / dec)
    return s + hp(rng.standard_normal(len(t)), 5000) * np.exp(-t / 0.003) * 0.4


# -------------------------------------------------------------------- sounds

def goal_complete():
    buf = np.zeros(int(2.4 * SR))
    place(buf, bell(523.25 * 2, 2.0, 0.8) * 0.5, 0.0)
    for i, f in enumerate([783.99, 987.77, 1174.66, 1567.98]):
        place(buf, glock(f * 1.0) * (0.55 + i * 0.1), 0.06 + i * 0.09)
    place(buf, glock(2349.3) * 0.3, 0.5)
    return norm(fade_out(reverb(buf, 0.3)))


def bounty_up():
    buf = np.zeros(int(3.6 * SR))
    place(buf, bell(196.0, 3.4, 0.7), 0.0)
    place(buf, bell(196.0 * 1.003, 3.0, 0.5) * 0.25, 0.012)
    for i in range(7):
        place(buf, clink(rng.uniform(2600, 3800)) * rng.uniform(0.15, 0.3), 0.35 + i * rng.uniform(0.04, 0.09))
    return norm(fade_out(reverb(buf, 0.35)))


def rank_up():
    # Bugle call on the natural harmonics of a G bugle: G4 C5 E5 G5 ... (3rd,4th,5th,6th harmonic of C)
    c = 130.81
    notes = [(3, 0.12), (4, 0.12), (5, 0.12), (6, 0.36), (5, 0.12), (6, 0.6)]
    buf = np.zeros(int(2.2 * SR))
    at = 0.0
    for h, d in notes:
        place(buf, brass(c * h, d + 0.05, bright=0.9, attack=0.025) * 0.6, at)
        at += d
    return norm(fade_out(reverb(buf, 0.25)))


def boss_defeat():
    buf = np.zeros(int(5.0 * SR))
    # timpani roll crescendo
    for i in range(16):
        place(buf, timpani(87.31, 0.6, 0.15 + i * 0.04), i * 0.05)
    place(buf, timpani(87.31, 2.0, 1.2), 0.8)
    place(buf, cymbal(0.8, swell=True), 0.0)
    place(buf, cymbal(3.0), 0.8)
    # fanfare: F major, then Bb, then F with a high top
    chords = [(0.8, 0.5, [174.61, 220.0, 261.63, 349.23]),
              (1.3, 0.3, [233.08, 293.66, 349.23, 466.16]),
              (1.6, 0.25, [196.0, 261.63, 329.63, 392.0]),
              (1.85, 2.0, [174.61, 220.0, 261.63, 349.23, 440.0, 523.25])]
    for at, d, freqs in chords:
        for f in freqs:
            place(buf, brass(f, d, bright=0.85, attack=0.03) * 0.28, at + rng.uniform(0, 0.012))
    place(buf, timpani(87.31, 2.5, 1.0), 1.85)
    place(buf, cymbal(3.0), 1.85)
    return norm(fade_out(reverb(buf, 0.3)))


def boss_horn():
    t1 = brass(73.42, 1.4, vib=4, bright=1.0, attack=0.25)
    t2 = brass(65.41, 2.2, vib=4, bright=1.0, attack=0.15)
    growl = 1 + 0.25 * np.sin(2 * np.pi * 31 * t_axis(3.6))
    buf = np.zeros(int(3.6 * SR))
    place(buf, t1, 0.0)
    place(buf, brass(110.0, 1.4, vib=4, bright=0.6, attack=0.25) * 0.4, 0.0)
    place(buf, t2, 1.3)
    place(buf, brass(98.0, 2.2, vib=4, bright=0.6, attack=0.15) * 0.4, 1.3)
    buf *= growl
    return norm(fade_out(reverb(lp(buf, 2500), 0.4, 0.86)))


def pact_bind():
    dur = 2.8
    t = t_axis(dur)
    s = np.zeros_like(t)
    for f in (220, 277.18, 329.63, 440, 554.37):
        for det in (-0.6, 0.6):
            sweep = f * (1 + 0.08 * t / dur) + det
            s += np.sin(2 * np.pi * np.cumsum(sweep) / SR) * 0.2
    shimmer = sum(np.sin(2 * np.pi * f * t) * (0.5 + 0.5 * np.sin(2 * np.pi * r * t)) for f, r in ((1760, 6), (2217, 7.3), (2637, 5.1)))
    breath = bp(rng.standard_normal(len(t)), 600, 2400) * 0.1
    e = env(len(t), a=0.6, d=0.4, s=0.7, r=1.0)
    return norm(fade_out(reverb((s + 0.12 * shimmer + breath) * e, 0.45)))


def coins(seed):
    r = np.random.default_rng(seed)
    buf = np.zeros(int(0.9 * SR))
    at = 0.0
    for i in range(r.integers(4, 7)):
        place(buf, clink(r.uniform(2400, 4200)) * r.uniform(0.4, 1.0), at)
        at += r.uniform(0.03, 0.1)
    return norm(fade_out(reverb(buf, 0.15)), 0.7)


def sea_ambient():
    dur = 24.0
    n = int(dur * SR)
    t = np.arange(n) / SR
    # Brown-ish noise, low passed, with wave swells whose periods divide the loop length.
    white = rng.standard_normal(n + SR)
    brown = np.cumsum(white)
    brown = hp(brown, 30)[:n]
    brown /= np.max(np.abs(brown))
    surf = hp(rng.standard_normal(n), 900)
    swell = 0.5 + 0.5 * np.sin(2 * np.pi * t / 6.0) ** 3
    swell2 = 0.5 + 0.5 * np.sin(2 * np.pi * t / 8.0 + 1.3)
    wash = lp(brown, 500) * (0.4 + 0.6 * swell2) + lp(surf, 3000) * 0.12 * np.clip(swell, 0, 1) ** 2
    # make it loop seamlessly: crossfade the tail into the head
    x = np.concatenate([wash, wash[:SR]])
    head = x[:SR]
    tail = x[n:n + SR]
    k = np.linspace(0, 1, SR)
    x = x[:n].copy()
    x[:SR] = head * k + tail * (1 - k)
    return norm(x, 0.6)


def gull(seed):
    r = np.random.default_rng(seed)
    buf = np.zeros(int(1.6 * SR))
    at = 0.0
    for i in range(r.integers(2, 4)):
        d = r.uniform(0.25, 0.42)
        t = t_axis(d)
        f0 = r.uniform(1300, 1700)
        f = f0 * (1 + 0.35 * np.sin(np.pi * t / d)) - 300 * t / d
        ph = 2 * np.pi * np.cumsum(f) / SR
        s = sum(np.sin(h * ph) / h ** 1.2 for h in range(1, 6))
        s = bp(s + 0.2 * r.standard_normal(len(t)), 900, 6000) * env(len(t), a=0.02, d=0.05, s=0.8, r=0.12)
        place(buf, s * r.uniform(0.6, 1.0), at)
        at += d + r.uniform(0.05, 0.15)
    return norm(fade_out(reverb(buf, 0.2)), 0.5)


def creak(seed):
    r = np.random.default_rng(seed)
    d = r.uniform(0.9, 1.4)
    t = t_axis(d)
    f = r.uniform(70, 110) * (1 + 0.6 * t / d + 0.05 * np.sin(2 * np.pi * 9 * t))
    # stick-slip: a train of tiny clicks at the creak frequency
    ph = np.cumsum(f) / SR
    clicks = (np.diff(np.floor(ph), prepend=0) > 0).astype(float)
    s = bp(clicks + 0.02 * r.standard_normal(len(t)), 300, 2200) * env(len(t), a=0.15, d=0.1, s=0.9, r=0.3)
    return norm(fade_out(reverb(s, 0.25)), 0.45)


def write(name, x):
    os.makedirs(OUT, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        wavfile.write(f.name, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", f.name, "-ac", "1", "-c:a", "libvorbis", "-q:a", "4",
                        os.path.join(OUT, name + ".ogg")], check=True)
        os.unlink(f.name)
    print(f"{name}: {len(x) / SR:.1f}s")


def main():
    write("goal_complete", goal_complete())
    write("bounty_up", bounty_up())
    write("rank_up", rank_up())
    write("boss_defeat", boss_defeat())
    write("boss_horn", boss_horn())
    write("pact_bind", pact_bind())
    for i in range(3):
        write(f"coins{i + 1}", coins(40 + i))
    write("sea_ambient", sea_ambient())
    for i in range(3):
        write(f"gull{i + 1}", gull(70 + i))
    for i in range(2):
        write(f"creak{i + 1}", creak(90 + i))


if __name__ == "__main__":
    main()
