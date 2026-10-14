"""Regenerates the APK padding asset.

The release APK is deliberately padded to 7 MB (see the README). The padding
lives in app/src/main/assets/release/placeholder.bin and holds random bytes,
which barely compress, so its size controls the final APK size.

PAD_TARGET is the raw byte count *before* base64 encoding; the asset is written
base64-encoded, so it ends up 4/3 of that. To nudge the APK toward 7,000,000
bytes, add the signed difference (APK size minus 7,000,000) to PAD_TARGET and
rebuild. The final byte oscillates and is not reachable exactly because DEFLATE
compression is not linear.
"""
import base64
import os
import random

TARGET = int(os.environ.get("PAD_TARGET", 3_078_267))
OUT = os.path.join(
    os.path.dirname(__file__),
    "..",
    "app",
    "src",
    "main",
    "assets",
    "release",
    "placeholder.bin",
)

rng = random.Random(20261014)
body = bytes(rng.randrange(256) for _ in range(TARGET))

with open(OUT, "wb") as f:
    f.write(base64.b64encode(body))
print("wrote", OUT, TARGET * 4 // 3, "bytes")
