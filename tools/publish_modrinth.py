"""Publish the verified release to Modrinth. The token stays in process memory.

Usage: python tools/publish_modrinth.py --check
       python tools/publish_modrinth.py --publish
       python tools/publish_modrinth.py --update-links
"""
from __future__ import annotations

import argparse
import getpass
import hashlib
import json
from pathlib import Path
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PUB = ROOT / "publishing"
SLUG = "create-all-the-soup-as-you-go"
VERSION = "1.1.0"
TITLE = "Create: All the Soup as You Go"
API = "https://api.modrinth.com/v2"
SOURCE_URL = "https://github.com/enderliker/create-all-the-soup-as-you-go"


class Client:
    def __init__(self, token):
        self.token = token

    def request(self, method, path, data=None, content_type="application/json"):
        if isinstance(data, dict):
            data = json.dumps(data).encode()
        request = urllib.request.Request(API + path, data=data, method=method, headers={
            "Authorization": self.token,
            "User-Agent": "Ender/AllTheSoupAsYouGo/1.1.0 (release tooling)",
            "Content-Type": content_type,
        })
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as error:
            message = error.read().decode(errors="replace").replace(self.token, "[REDACTED]")
            raise RuntimeError(f"Modrinth {method} {path}: HTTP {error.code}: {message[:1500]}") from None

    def multipart(self, path, metadata, files):
        boundary = "SoupRelease" + uuid.uuid4().hex
        body = bytearray()
        for name, filename, mime, payload in [("data", None, "application/json", json.dumps(metadata).encode()), *files]:
            body.extend(f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"".encode())
            if filename:
                body.extend(f"; filename=\"{filename}\"".encode())
            body.extend(f"\r\nContent-Type: {mime}\r\n\r\n".encode())
            body.extend(payload)
            body.extend(b"\r\n")
        body.extend(f"--{boundary}--\r\n".encode())
        return self.request("POST", path, bytes(body), "multipart/form-data; boundary=" + boundary)


def publish(client, user, project_only=False):
    jar = ROOT / f"build/libs/{SLUG}-{VERSION}.jar"
    with zipfile.ZipFile(jar) as archive:
        assert not any("/dev/" in name for name in archive.namelist()), "Development automation in release jar"
        assert "allthesoupasyougo.mixins.json" in archive.namelist()
        assert len([n for n in archive.namelist() if n.startswith("data/allthesoupasyougo/recipe/") and n.endswith(".json")]) == 26
    for report in ("full-integration.txt", "vanilla-integration.txt"):
        text = (PUB / "verification" / report).read_text()
        assert "FAIL" not in text and "PASS" in text, "Missing passing integration report"
    gallery = json.loads((PUB / "gallery.json").read_text())
    for entry in gallery:
        assert (PUB / "gallery" / entry["file"]).is_file()
    body_template = (PUB / "description.md").read_text()
    body = body_template
    for entry in gallery:
        body = body.replace("{{" + entry["key"] + "}}", "")
    projects = client.request("GET", f"/user/{user['id']}/projects")
    project = next((p for p in projects if p["slug"] == SLUG), None)
    if project is None:
        metadata = {
            "slug": SLUG, "title": TITLE,
            "description": "Drain soups into Create tanks and serve them with a Spout. Includes distinct fluids and buckets, plus Farmer's Delight support.",
            "body": body, "categories": ["food", "technology"], "additional_categories": ["utility"],
            "client_side": "required", "server_side": "required", "license_id": "LicenseRef-All-Rights-Reserved",
            "project_type": "mod", "is_draft": True, "status": "draft",
            "source_url": SOURCE_URL, "issues_url": SOURCE_URL + "/issues",
            "initial_versions": [], "gallery_items": [],
        }
        project = client.multipart("/project", metadata, [
            ("icon", "logo.png", "image/png", (ROOT / "branding/logo.png").read_bytes())])
    project_id = project["id"]
    (PUB / "modrinth-state.json").write_text(json.dumps({"project_id": project_id, "slug": SLUG}, indent=2) + "\n")
    print(f"Project: https://modrinth.com/mod/{SLUG}", flush=True)
    version = None
    digest = hashlib.sha512(jar.read_bytes()).hexdigest()
    if not project_only:
        # A new project has no loader yet; v2's version-list route returns 404 until its first upload.
        versions = client.request("GET", f"/project/{project_id}/version") if project.get("versions") else []
        digest = hashlib.sha512(jar.read_bytes()).hexdigest()
        version = next((v for v in versions if any(f["hashes"].get("sha512") == digest for f in v["files"])), None)
        if version is None:
            assert not any(v["version_number"] == VERSION for v in versions), "Version already exists with a different jar"
            create_versions = client.request("GET", "/project/create/version?" + urllib.parse.urlencode({
                "loaders": '["neoforge"]', "game_versions": '["1.21.1"]'}))
            create = next(v for v in create_versions if v["version_number"] == "6.0.10+mc1.21.1")
            fd = client.request("GET", "/project/farmers-delight")
            metadata = {
                "project_id": project_id, "name": f"{TITLE} {VERSION}", "version_number": VERSION,
                "changelog": (PUB / "changelog.md").read_text(),
                "dependencies": [
                    {"project_id": create["project_id"], "version_id": create["id"], "dependency_type": "required"},
                    {"project_id": fd["id"], "dependency_type": "optional"},
                ],
                "game_versions": ["1.21.1"], "loaders": ["neoforge"], "environment": "client_and_server",
                "version_type": "release", "featured": True, "status": "listed",
                "file_parts": ["mod"], "primary_file": "mod",
            }
            version = client.multipart("/version", metadata, [("mod", jar.name, "application/java-archive", jar.read_bytes())])
    project = client.request("GET", f"/project/{project_id}")
    for index, entry in enumerate(gallery):
        if any(g.get("title") == entry["title"] for g in project["gallery"]):
            continue
        query = urllib.parse.urlencode({"ext": "png", "featured": str(entry["featured"]).lower(),
            "title": entry["title"], "description": entry["description"], "ordering": index})
        client.request("POST", f"/project/{project_id}/gallery?{query}",
            (PUB / "gallery" / entry["file"]).read_bytes(), "image/png")
    project = client.request("GET", f"/project/{project_id}")
    body = body_template
    for entry in gallery:
        uploaded = next(g for g in project["gallery"] if g.get("title") == entry["title"])
        body = body.replace("{{" + entry["key"] + "}}", f"![{entry['title']}]({uploaded.get('raw_url') or uploaded['url']})")
    client.request("PATCH", f"/project/{project_id}", {
        "body": body, "source_url": SOURCE_URL, "issues_url": SOURCE_URL + "/issues"})
    (PUB / "description-published.md").write_text(body)
    if version is not None and project["status"] == "draft":
        client.request("PATCH", f"/project/{project_id}", {"status": "processing", "requested_status": "approved"})
    project = client.request("GET", f"/project/{project_id}")
    state = {"project_id": project_id, "slug": SLUG, "status": project["status"], "version_id": version["id"] if version else None,
             "sha512": digest, "url": f"https://modrinth.com/mod/{SLUG}",
             "gallery": [{"title": g["title"], "url": g["url"]} for g in project["gallery"]]}
    (PUB / "modrinth-state.json").write_text(json.dumps(state, indent=2) + "\n")
    print(json.dumps(state, indent=2))


def update_links(client):
    client.request("PATCH", f"/project/{SLUG}", {
        "source_url": SOURCE_URL, "issues_url": SOURCE_URL + "/issues"})
    project = client.request("GET", f"/project/{SLUG}")
    if project.get("source_url") != SOURCE_URL:
        raise RuntimeError("Modrinth did not return the updated source URL")
    print(json.dumps({key: project[key] for key in ("id", "status", "source_url", "issues_url")}, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--check", action="store_true")
    group.add_argument("--publish", action="store_true")
    group.add_argument("--project-only", action="store_true")
    group.add_argument("--update-links", action="store_true")
    parser.add_argument("--token-file", type=Path)
    args = parser.parse_args()
    token = args.token_file.read_text().strip() if args.token_file else getpass.getpass("Modrinth token (hidden): ").strip()
    client = Client(token)
    user = client.request("GET", "/user")
    print("Authenticated Modrinth account:", user["username"], flush=True)
    if args.publish or args.project_only:
        publish(client, user, args.project_only)
    elif args.update_links:
        update_links(client)


if __name__ == "__main__":
    main()
