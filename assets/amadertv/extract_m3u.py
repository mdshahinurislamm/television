#!/usr/bin/env python3
import requests

url = "http://amadertv.top:8080/get.php?username=34634653&password=34634653&type=m3u_plus&output=m3u8"

r = requests.get(url, timeout=15)
r.raise_for_status()
text = r.text.splitlines()

pairs = []
current_meta = None

for line in text:
    line = line.strip()
    if not line:
        continue
    if line.startswith("#EXTINF"):
        # Attempt to extract the display name
        # Format example: #EXTINF:-1 tvg-id="..." tvg-name="NAME" tvg-logo="..." group-title="..." ,Display Name
        if ',' in line:
            current_meta = line.split(',', 1)[1].strip()
        else:
            current_meta = line
    elif line.startswith("#"):
        # other metadata - ignore
        continue
    else:
        # this is a URL/stream entry
        url_line = line
        name = current_meta or url_line
        pairs.append((name, url_line))
        current_meta = None

# Print results
for name, link in pairs:
    print(f"{name} — {link}")
