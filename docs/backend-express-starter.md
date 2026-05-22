# Backend restore (Express) for NewsHub Android

> **Update:** The official web repo uses **Spring Boot on Render for news only** and **Supabase for elections/votes**. Android now matches that split. See `docs/ARCHITECTURE_ALIGNMENT.md`. You do **not** need Express elections routes unless you intentionally move off Supabase.

Live probe of `https://newshub-4kk0.onrender.com` shows:

- `GET /api/news` → **200** (Spring-style JSON: `items`, `totalCount`, `hasMore`) but often **empty**
- `GET /api/elections` → **404** with Spring error body (`timestamp`, `status`, `error`, `path`)

The Android app expects **all** routes in `app/src/main/java/com/example/newshub/network/BackendApi.kt` under the same base URL.

## Render checklist

1. Confirm the Render service deploys the repo that should own **elections/candidates/votes**, not only a partial news service.
2. If the deployed app is **Spring Boot**, either add the missing controllers under `/api/elections`, `/api/candidates`, `/api/votes`, or redeploy the **Node/Express** backend below and point `BACKEND_BASE_URL` to it.
3. **Start command** (Node): `npm start` with `PORT` from Render.
4. **Root directory**: folder containing `package.json` (e.g. `server/` or repo root).
5. Seed news/elections data or connect upstream APIs so `/api/news` is not permanently empty.

## Minimal Express layout

```
backend/
  package.json
  src/
    index.js
    routes/
      news.js
      elections.js
      candidates.js
      votes.js
    data/
      elections.json   # seed data until DB wired
```

### `backend/package.json`

```json
{
  "name": "newshub-backend",
  "version": "1.0.0",
  "main": "src/index.js",
  "scripts": {
    "start": "node src/index.js"
  },
  "dependencies": {
    "cors": "^2.8.5",
    "express": "^4.21.2"
  }
}
```

### `backend/src/index.js`

```javascript
const express = require("express");
const cors = require("cors");
const newsRoutes = require("./routes/news");
const electionsRoutes = require("./routes/elections");
const candidatesRoutes = require("./routes/candidates");
const votesRoutes = require("./routes/votes");

const app = express();
app.use(cors());
app.use(express.json());

app.use("/api/news", newsRoutes);
app.use("/api/elections", electionsRoutes);
app.use("/api/candidates", candidatesRoutes);
app.use("/api/votes", votesRoutes);

app.get("/health", (_req, res) => res.json({ ok: true }));

const port = process.env.PORT || 8080;
app.listen(port, () => console.log(`NewsHub API on ${port}`));
```

### `backend/src/routes/elections.js` (matches Android parsers)

```javascript
const express = require("express");
const elections = require("../data/elections.json");

const router = express.Router();

router.get("/", (_req, res) => {
  res.json({ items: elections, data: elections });
});

router.get("/search", (req, res) => {
  const q = (req.query.q || "").toLowerCase();
  const limit = Number(req.query.limit || 5);
  const items = elections.filter((e) => e.name.toLowerCase().includes(q)).slice(0, limit);
  res.json({ items });
});

router.get("/:electionId", (req, res) => {
  const item = elections.find((e) => e.id === req.params.electionId);
  if (!item) return res.status(404).json({ message: "Election not found" });
  res.json(item);
});

router.get("/:electionId/candidates", (req, res) => {
  const election = elections.find((e) => e.id === req.params.electionId);
  res.json({ items: election?.candidates || [] });
});

module.exports = router;
```

Implement `news.js`, `candidates.js`, and `votes.js` with the same path prefixes as `BackendApi.kt`. Vote responses should include `receiptId`, `status`, and `message` keys.

## Android config

No change required if Render serves the full API at:

`BACKEND_BASE_URL=https://newshub-4kk0.onrender.com`

(trailing slash is trimmed in `BackendService`.)
