import json
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed

# Load JSON file with links
with open("m3u8_links.json", "r", encoding="utf-8") as f:
    links = json.load(f)

working = []
broken = []

def check_url(url):
    try:
        r = requests.get(url, timeout=3, stream=True)
        if r.status_code == 200 and r.text.startswith("#EXTM3U"):
            return (url, True)
    except requests.exceptions.RequestException:
        pass
    return (url, False)

print(f"Checking {len(links)} links... please wait")

# Run with 20 parallel threads
with ThreadPoolExecutor(max_workers=20) as executor:
    future_to_url = {executor.submit(check_url, url): url for url in links}
    for i, future in enumerate(as_completed(future_to_url), start=1):
        url, ok = future.result()
        if ok:
            print(f"[{i}]  Working: {url}")
            working.append(url)
        else:
            print(f"[{i}]  Broken: {url}")
            broken.append(url)

# Save results
with open("working_links.json", "w", encoding="utf-8") as f:
    json.dump(working, f, indent=2)

with open("broken_links.json", "w", encoding="utf-8") as f:
    json.dump(broken, f, indent=2)

print("\nFinished!")
print(f" {len(working)} working links saved to working_links.json")
print(f" {len(broken)} broken links saved to broken_links.json")
