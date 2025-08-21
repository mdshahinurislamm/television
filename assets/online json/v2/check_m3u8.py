import re
import json
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed

# Load your garbage text file
with open("tv.txt", "r", encoding="utf-8", errors="ignore") as f:
    content = f.read()

# Extract all links (http/https)
urls = re.findall(r'https?://[^\s"\'<>]+', content)

# Only keep .m3u8 and YouTube links
filtered = []
for u in urls:
    if u.endswith(".m3u8") or "youtube.com" in u or "youtu.be" in u:
        filtered.append(u)

print(f"Found {len(filtered)} candidate links")

def check_url(url):
    """Check if a URL is working"""
    try:
        r = requests.get(url, timeout=3, stream=True)
        if r.status_code == 200:
            # For m3u8 check if starts with #EXTM3U
            if url.endswith(".m3u8"):
                if r.text.strip().startswith("#EXTM3U"):
                    return (url, "stream", True)
                else:
                    return (url, "stream", False)
            # YouTube links (embed/web)
            elif "youtube.com" in url or "youtu.be" in url:
                return (url, "web", True)
    except:
        pass
    # Failed
    if url.endswith(".m3u8"):
        return (url, "stream", False)
    else:
        return (url, "web", False)

working_channels = []

# Multi-threaded check
with ThreadPoolExecutor(max_workers=20) as executor:
    futures = [executor.submit(check_url, u) for u in filtered]
    for i, future in enumerate(as_completed(futures), start=1):
        url, utype, ok = future.result()
        if ok:
            print(f"[{i}] WORKING: {url}")
            working_channels.append({
                "url": url,
                "type": utype
            })
        else:
            print(f"[{i}] BROKEN: {url}")

# Save in requested JSON format
with open("working_channels.json", "w", encoding="utf-8") as f:
    json.dump({"channels": working_channels}, f, indent=2)

print("\n Finished!")
print(f"Total working channels: {len(working_channels)}")
print("Saved to working_channels.json")
