"""Upload the release to an existing CurseForge project using an author token.

python tools/publish_curseforge.py --project-id 123456 --token-file /tmp/key_curse_upload
The Core API key used by launchers cannot upload author files.
"""
import argparse
import getpass
import json
from pathlib import Path
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
API = "https://minecraft.curseforge.com/api"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-id", type=int)
    parser.add_argument("--token-file", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if not args.check and args.project_id is None:
        parser.error("--project-id is required to upload a release")
    token = args.token_file.read_text().strip() if args.token_file else getpass.getpass("CurseForge author token (hidden): ").strip()

    def request(path, data=None, mime="application/json"):
        req = urllib.request.Request(API + path, data=data, headers={
            "X-Api-Token": token, "Content-Type": mime,
            "User-Agent": "Ender/AllTheSoupAsYouGo/1.1.0 (release tooling)"})
        try:
            with urllib.request.urlopen(req, timeout=60) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            message = error.read().decode(errors="replace").replace(token, "[REDACTED]")
            raise RuntimeError(f"CurseForge HTTP {error.code}: {message[:1200]}") from None

    versions = request("/game/versions")
    wanted = {"1.21.1", "NeoForge", "Java 21", "Client", "Server"}
    # 1.21.1 also exists under Bukkit and Bedrock addons. Select the Java Minecraft type.
    types = {"1.21.1": 77784, "NeoForge": 68441, "Java 21": 2, "Client": 75208, "Server": 75208}
    matched = [v for v in versions if v["name"] in wanted and v["gameVersionTypeID"] == types[v["name"]]]
    assert {"1.21.1", "NeoForge"} <= {v["name"] for v in matched}, "Missing Minecraft/NeoForge version IDs"
    if args.check:
        print("Author upload token validated. Target versions:", json.dumps(matched))
        return
    metadata = {
        "displayName": "Create: All the Soup as You Go 1.1.0 — NeoForge 1.21.1",
        "changelog": (ROOT / "publishing/changelog.md").read_text(), "changelogType": "markdown",
        "gameVersions": [v["id"] for v in matched], "releaseType": "release",
        "relations": {"projects": [
            {"slug": "create", "type": "requiredDependency"},
            {"slug": "farmers-delight", "type": "optionalDependency"},
        ]},
    }
    jar = ROOT / "build/libs/create-all-the-soup-as-you-go-1.1.0.jar"
    boundary = "SoupRelease" + uuid.uuid4().hex
    body = bytearray()
    for name, filename, mime, payload in [
        ("metadata", None, "application/json", json.dumps(metadata).encode()),
        ("file", jar.name, "application/java-archive", jar.read_bytes()),
    ]:
        body.extend(f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"".encode())
        if filename:
            body.extend(f"; filename=\"{filename}\"".encode())
        body.extend(f"\r\nContent-Type: {mime}\r\n\r\n".encode())
        body.extend(payload)
        body.extend(b"\r\n")
    body.extend(f"--{boundary}--\r\n".encode())
    result = request(f"/projects/{args.project_id}/upload-file", bytes(body), "multipart/form-data; boundary=" + boundary)
    state = {"project_id": args.project_id, "file_id": result["id"]}
    (ROOT / "publishing/curseforge-state.json").write_text(json.dumps(state, indent=2) + "\n")
    print(json.dumps(state))


if __name__ == "__main__":
    main()
