"""Prints the client test screenshots into the job log as base64 JPEG (lines: SHOTJPG <name> <chunk>)."""
import base64
import io
import os
import sys

from PIL import Image

folder = sys.argv[1]
prefix = sys.argv[2] if len(sys.argv) > 2 else ''
if not os.path.isdir(folder):
    print('no screenshots folder', folder)
    sys.exit(0)
for name in sorted(os.listdir(folder)):
    if not name.endswith('.png'):
        continue
    img = Image.open(os.path.join(folder, name)).convert('RGB')
    buf = io.BytesIO()
    img.save(buf, 'JPEG', quality=84, optimize=True)
    data = base64.b64encode(buf.getvalue()).decode()
    stem = prefix + name[:-4]
    print(f'SHOTINFO {stem} {img.size[0]}x{img.size[1]} {len(buf.getvalue())} bytes')
    for i in range(0, len(data), 6000):
        print(f'SHOTJPG {stem} {data[i:i + 6000]}')
