"""Synthesises the launcher's UI sounds into res/raw: short, soft and quiet.

Usage: python -I tools/generate_sounds.py res/raw
"""
import math
import struct
import sys
import wave
from pathlib import Path

SAMPLE_RATE = 44100
ATTACK_SECONDS = 0.006
LOW_PASS_HZ = 2400


def tone(frequency, seconds, decay_seconds, amplitude):
    """A sine with a soft attack and an exponential decay, like a felt mallet."""
    attack_samples = int(SAMPLE_RATE * ATTACK_SECONDS)
    samples = []
    for i in range(int(SAMPLE_RATE * seconds)):
        t = i / SAMPLE_RATE
        envelope = min(1.0, i / attack_samples) * math.exp(-t / decay_seconds)
        samples.append(amplitude * envelope * math.sin(2 * math.pi * frequency * t))
    return samples


def delayed(samples, seconds):
    return [0.0] * int(SAMPLE_RATE * seconds) + samples


def mix(*layers):
    length = max(len(layer) for layer in layers)
    return [sum(layer[i] for layer in layers if i < len(layer)) for i in range(length)]


def low_pass(samples, cutoff_hz=LOW_PASS_HZ):
    """One-pole low-pass: rounds off the glassy top end so nothing sounds sharp."""
    smoothing = 1 - math.exp(-2 * math.pi * cutoff_hz / SAMPLE_RATE)
    filtered, level = [], 0.0
    for sample in samples:
        level += smoothing * (sample - level)
        filtered.append(level)
    return filtered


def write_wav(path, samples):
    frames = b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples)
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(frames)


def focus_tick():
    """A soft, woody tap for moving between tiles: felt rather than heard."""
    return low_pass(mix(
        tone(880, 0.12, 0.022, 0.16),
        tone(1320, 0.08, 0.012, 0.04),
        tone(330, 0.10, 0.030, 0.08),
    ))


def select_chime():
    """A warm major-chord bloom for opening something, notes rolling in like a gentle strum."""
    return low_pass(mix(
        tone(523.25, 0.55, 0.16, 0.12),
        delayed(tone(659.25, 0.50, 0.15, 0.09), 0.035),
        delayed(tone(783.99, 0.45, 0.14, 0.08), 0.070),
        tone(261.63, 0.40, 0.12, 0.06),
    ))


def main(raw_dir):
    raw_dir.mkdir(parents=True, exist_ok=True)
    write_wav(raw_dir / "focus.wav", focus_tick())
    write_wav(raw_dir / "select.wav", select_chime())


if __name__ == "__main__":
    main(Path(sys.argv[1]))
