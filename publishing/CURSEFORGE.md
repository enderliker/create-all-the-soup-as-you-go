# CurseForge publication

The Upload API needs an existing project and an **author upload token**. A Core
API key from console.curseforge.com can read the catalog but cannot upload files.

Author tokens: https://authors-old.curseforge.com/account/api-tokens

Project details:

- Name: **Create: All the Soup as You Go**
- Summary: Drain soups into Create tanks and serve them with a Spout. Includes distinct fluids and buckets, plus Farmer's Delight support.
- Game: Minecraft
- Categories: Food, Addons, Technology
- Logo: `curseforge-logo.png` (400 × 400)
- Description: `description-published.md` includes the uploaded screenshot URLs once Modrinth is ready.
- Gallery: the three original Minecraft screenshots in `gallery/`; captions are in `gallery.json`.
- License: All Rights Reserved
- Requirements: Minecraft 1.21.1, NeoForge 21.1.251, Create 6.0.10. Farmer's Delight is optional.

Once the project exists, upload the tested release:

```sh
python tools/publish_curseforge.py --project-id PROJECT_ID --token-file /tmp/key_curse_upload
```

The token is read locally and is never added to the upload or repository.

API documentation: https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api
