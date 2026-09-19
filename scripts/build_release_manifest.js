#!/usr/bin/env node
"use strict";
/*
 * Builds the website's static release data.
 *
 * The site used to call the public GitHub API from every page view, and that
 * API allows only 60 unauthenticated requests per hour per IP: once the quota is
 * spent (a normal browsing session is enough) the download dialog, the downloads
 * page and the download counter went empty for everybody behind that IP, and a
 * page refresh could not help for up to an hour. The site now reads these files,
 * which are generated here (with an authenticated token, so no quota is
 * involved) and served from the same origin.
 *
 *   releases.json  newest N releases WITHOUT the release notes (+ everything the
 *                  download UI needs) plus repo stats and the total download
 *                  count — small enough to load on the home page
 *   changelog.json every release WITH its notes, for the changelog page only
 *
 * Usage:
 *   node scripts/build_release_manifest.js <releases.json from the API> \
 *        <repo.json from the API> <out releases.json> <out changelog.json>
 *
 * The releases input may be a single array or an array of pages (--paginate).
 */
const fs = require("fs");
const path = require("path");

const RECENT_LIMIT = Number(process.env.MANIFEST_RECENT || 40);
const BODY_LIMIT = Number(process.env.MANIFEST_BODIES || 5);

const [, , releasesIn, repoIn, releasesOut, changelogOut] = process.argv;
if (!releasesIn || !releasesOut || !changelogOut) {
  console.error("usage: build_release_manifest.js <releases.json> [repo.json] <out.releases.json> <out.changelog.json>");
  process.exit(2);
}

function readJson(file) {
  if (!file || file === "-") return JSON.parse(fs.readFileSync(0, "utf8"));
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function flatten(raw) {
  if (!Array.isArray(raw)) return [];
  if (raw.every((x) => Array.isArray(x))) return raw.flat();
  return raw;
}

const byPublishedDesc = (a, b) =>
  new Date(b.published_at || 0).getTime() - new Date(a.published_at || 0).getTime();

const releases = flatten(readJson(releasesIn))
  .filter((r) => r && !r.draft)
  .sort(byPublishedDesc);

const repo = (() => {
  try { return readJson(repoIn); } catch (e) { return {}; }
})();

const totalDownloads = flatten(readJson(releasesIn)).reduce(
  (sum, r) => sum + (r.assets || []).reduce((s, a) => s + (a.download_count || 0), 0),
  0,
);

const assets = (r) =>
  (r.assets || []).map((a) => ({
    name: a.name,
    size: a.size,
    download_count: a.download_count,
    browser_download_url: a.browser_download_url,
  }));

const slim = (r, withBody) => {
  const out = {
    tag_name: r.tag_name,
    name: r.name,
    published_at: r.published_at,
    prerelease: !!r.prerelease,
    draft: !!r.draft,
    html_url: r.html_url,
    assets: assets(r),
  };
  if (withBody) out.body = r.body;
  return out;
};

const meta = {
  generatedAt: new Date().toISOString(),
  repo: "PiBOH/vivi-music",
  totalReleases: releases.length,
  totalDownloads,
  stars: repo.stargazers_count || 0,
  forks: repo.forks_count || 0,
};

const recent = {
  ...meta,
  list: releases.slice(0, RECENT_LIMIT).map((r, i) => slim(r, i < BODY_LIMIT)),
};
const changelog = {
  ...meta,
  list: releases.map((r) => slim(r, true)),
};

fs.writeFileSync(releasesOut, JSON.stringify(recent));
fs.writeFileSync(changelogOut, JSON.stringify(changelog));
console.log(
  `${path.basename(releasesOut)}: ${recent.list.length} releases, ${(JSON.stringify(recent).length / 1024).toFixed(0)} KiB` +
  ` | ${path.basename(changelogOut)}: ${changelog.list.length} releases, ${(JSON.stringify(changelog).length / 1024).toFixed(0)} KiB` +
  ` | total downloads ${totalDownloads}, stars ${meta.stars}`,
);
