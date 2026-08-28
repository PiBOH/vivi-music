/* VIVI Music DE website — resolves releases from the GitHub API and fills the
 * download buttons / version badges / changelog. Two channels are offered,
 * like the original site: "Stable" and "Nightly builds". Static fallbacks keep
 * the page usable if the API is unreachable. */
(function () {
  "use strict";

  var REPO = "PiBOH/vivi-music";
  var API_LIST = "https://api.github.com/repos/" + REPO + "/releases?per_page=30";
  var RELEASES_PAGE = "https://github.com/" + REPO + "/releases";

  var CHANNEL_SUFFIX = /-(nightly|alpha|beta|rc|stable)$/i;
  var releases = [];
  var currentChannel = "stable";
  var selectedRel = null;

  /* -------- tiny Markdown renderer (self-contained, no external deps) -------- */
  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
  }

  function inlineMd(s) {
    s = escapeHtml(s);
    s = s.replace(/`([^`]+)`/g, "<code>$1</code>");
    s = s.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
    s = s.replace(/\*([^*]+)\*/g, "<em>$1</em>");
    s = s.replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g,
      '<a href="$2" rel="noopener" target="_blank">$1</a>');
    return s;
  }

  function mdToHtml(md) {
    var lines = String(md || "").replace(/\r\n/g, "\n").split("\n");
    var out = [];
    var inList = false;
    var i = 0;
    function closeList() {
      if (inList) { out.push("</ul>"); inList = false; }
    }
    while (i < lines.length) {
      var t = lines[i].trim();
      if (!t) { closeList(); i++; continue; }
      if (/^```/.test(t)) {
        closeList();
        var code = [];
        i++;
        while (i < lines.length && !/^```/.test(lines[i].trim())) { code.push(lines[i]); i++; }
        i++; // skip closing fence
        out.push("<pre><code>" + escapeHtml(code.join("\n")) + "</code></pre>");
        continue;
      }
      var h = t.match(/^(#{1,4})\s+(.*)$/);
      if (h) {
        closeList();
        var lvl = Math.min(h[1].length + 2, 4); // ## -> h3, ### -> h4
        out.push("<h" + lvl + ">" + inlineMd(h[2]) + "</h" + lvl + ">");
        i++;
        continue;
      }
      if (/^-{3,}$/.test(t)) { closeList(); out.push("<hr />"); i++; continue; }
      if (/^[-*]\s+/.test(t)) {
        if (!inList) { out.push("<ul>"); inList = true; }
        out.push("<li>" + inlineMd(t.replace(/^[-*]\s+/, "")) + "</li>");
        i++;
        continue;
      }
      closeList();
      out.push("<p>" + inlineMd(t) + "</p>");
      i++;
    }
    closeList();
    return out.join("\n");
  }

  function renderChangelog(rel) {
    var body = document.getElementById("changelog-body");
    if (!body) return;
    if (!rel || !rel.body) {
      body.innerHTML = "<p>No release notes available for this version.</p>";
    } else {
      body.innerHTML = mdToHtml(rel.body);
    }
    var tagEl = document.getElementById("changelog-version");
    if (tagEl) tagEl.textContent = rel ? (rel.tag_name || "") : "";
  }

  function fillVersionSelect() {
    var sel = document.getElementById("changelog-select");
    if (!sel) return;
    sel.innerHTML = "";
    releases.forEach(function (r) {
      var o = document.createElement("option");
      o.value = r.tag_name || "";
      o.textContent = r.tag_name || r.name || "Release";
      sel.appendChild(o);
    });
    if (selectedRel && selectedRel.tag_name) sel.value = selectedRel.tag_name;
    sel.addEventListener("change", function () {
      var rel = releases.find(function (r) { return r.tag_name === sel.value; });
      selectedRel = rel || null;
      renderChangelog(rel);
    });
  }

  function isStable(r) {
    return !r.prerelease && !CHANNEL_SUFFIX.test(r.tag_name || "");
  }
  function isPre(r) {
    return r.prerelease || /-(nightly|alpha|beta|rc)$/i.test(r.tag_name || "");
  }

  function firstByExt(assets, ext) {
    var hit = null;
    (assets || []).forEach(function (a) {
      if (hit) return;
      var name = (a.name || "").toLowerCase();
      if (name.split(".").pop() === ext) hit = a.browser_download_url;
    });
    return hit;
  }

  function applyChannel() {
    var rel = currentChannel === "nightly"
      ? (releases.find(isPre) || releases[0])
      : (releases.find(isStable) || releases[0]);

    var tag = rel ? (rel.tag_name || "") : "";
    document.querySelectorAll("[data-version]").forEach(function (el) {
      el.textContent = tag || "GitHub Releases";
    });

    var links = {
      winExe: firstByExt(rel && rel.assets, "exe"),
      winMsi: firstByExt(rel && rel.assets, "msi"),
      deb: firstByExt(rel && rel.assets, "deb"),
      appimage: firstByExt(rel && rel.assets, "appimage"),
      dmg: firstByExt(rel && rel.assets, "dmg"),
      pkg: firstByExt(rel && rel.assets, "pkg"),
    };

    document.querySelectorAll("[data-download]").forEach(function (a) {
      var url = links[a.getAttribute("data-download")];
      if (url) {
        a.href = url;
        a.classList.remove("disabled");
        a.removeAttribute("title");
      } else {
        a.classList.add("disabled");
        a.title = "Not in this release";
      }
    });

    selectedRel = rel || null;
    renderChangelog(rel);
    var sel = document.getElementById("changelog-select");
    if (sel && rel && rel.tag_name) sel.value = rel.tag_name;
  }

  function selectChannel(ch, button) {
    currentChannel = ch;
    document.querySelectorAll(".tab").forEach(function (t) {
      t.classList.toggle("active", t === button);
    });
    applyChannel();
  }

  function init() {
    var toggle = document.querySelector(".nav-toggle");
    var links = document.querySelector(".nav-links");
    if (toggle && links) {
      toggle.addEventListener("click", function () {
        var open = links.classList.toggle("open");
        toggle.setAttribute("aria-expanded", open ? "true" : "false");
        toggle.textContent = open ? "\u2715" : "\u2630";
      });
    }

    document.querySelectorAll(".tab").forEach(function (t) {
      t.addEventListener("click", function () {
        selectChannel(t.getAttribute("data-channel"), t);
      });
    });

    fetch(API_LIST, { headers: { Accept: "application/vnd.github+json" } })
      .then(function (res) {
        if (!res.ok) throw new Error("api");
        return res.json();
      })
      .then(function (list) {
        releases = Array.isArray(list) ? list : [];
        // If only pre-releases exist, default to Nightly so the buttons always work.
        if (!releases.find(isStable) && releases.find(isPre)) {
          currentChannel = "nightly";
          document.querySelectorAll(".tab").forEach(function (t) {
            t.classList.toggle("active", t.getAttribute("data-channel") === "nightly");
          });
        }
        applyChannel();
        fillVersionSelect();
      })
      .catch(function () {
        document.querySelectorAll("[data-download]").forEach(function (a) {
          a.href = RELEASES_PAGE;
          a.classList.remove("disabled");
        });
        document.querySelectorAll("[data-version]").forEach(function (el) {
          el.textContent = "GitHub Releases";
        });
        var body = document.getElementById("changelog-body");
        if (body) body.innerHTML = "<p>Unable to load release notes from GitHub. See the <a href=\"" + RELEASES_PAGE + "\">releases page</a> instead.</p>";
      });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();