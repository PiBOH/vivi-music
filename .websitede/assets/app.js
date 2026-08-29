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

  /* Expose the changelog machinery so the site-wide search (second IIFE)
     can reuse it: search filters the fetched releases and re-renders. */
  window.VMDE_CHANGELOG = {
    releases: releases,
    get currentChannel() { return currentChannel; },
    renderChangelog: renderChangelog,
    fillVersionSelect: fillVersionSelect,
    selectChannel: selectChannel,
    escapeHtml: escapeHtml
  };
})();
/* ============================================================
 * Site-wide interactivity (theme, OS detection, reveal-on-scroll,
 * back-to-top, active nav, changelog search, screenshot gallery).
 * ============================================================ */
(function () {
  "use strict";

  var THEME_KEY = "vmde-theme";

  /* ---------- Theme toggle ---------- */
  function applyTheme(theme, btn) {
    document.documentElement.setAttribute("data-theme", theme);
    try { localStorage.setItem(THEME_KEY, theme); } catch (e) { /* private mode */ }
    if (btn) btn.textContent = theme === "light" ? "☀️" : "🌙";
  }
  var stored = null;
  try { stored = localStorage.getItem(THEME_KEY); } catch (e) { /* ignore */ }
  var toggleBtn = document.querySelector("[data-theme-toggle]");
  if (toggleBtn) {
    applyTheme(stored || "dark", toggleBtn);
    toggleBtn.addEventListener("click", function () {
      var next = document.documentElement.getAttribute("data-theme") === "light" ? "dark" : "light";
      applyTheme(next, toggleBtn);
    });
  }

  /* ---------- OS detection ---------- */
  function detectOS() {
    var ua = navigator.userAgent || "";
    var platform = (navigator.userAgentData && navigator.userAgentData.platform) ||
      navigator.platform || "";
    var p = platform.toLowerCase();
    if (p.indexOf("win") >= 0) return "windows";
    if (p.indexOf("mac") >= 0 || p.indexOf("iphone") >= 0 || p.indexOf("ipad") >= 0) return "macos";
    if (p.indexOf("linux") >= 0) return /android/i.test(ua) ? "android" : "linux";
    if (/android/i.test(ua)) return "android";
    return null;
  }
  window.VMDE_OS = detectOS();

  /* Downloads page: banner that names the visitor's OS and jumps to it. */
  var osNote = document.querySelector("[data-os-note]");
  if (osNote) {
    var os = window.VMDE_OS;
    var osNames = { windows: "Windows", linux: "Linux", macos: "macOS" };
    if (os && osNames[os]) {
      var nameEl = osNote.querySelector("[data-os-name]");
      if (nameEl) nameEl.textContent = osNames[os];
      osNote.classList.add("show");
      osNote.addEventListener("click", function () {
        var target = document.querySelector("[data-os-section=\"" + os + "\"]");
        if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
      });
    }
  }

  /* ---------- Reveal on scroll ---------- */
  var revealEls = document.querySelectorAll(".reveal");
  if ("IntersectionObserver" in window && revealEls.length) {
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add("visible");
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12 });
    revealEls.forEach(function (el) { observer.observe(el); });
  } else {
    revealEls.forEach(function (el) { el.classList.add("visible"); });
  }

  /* ---------- Back to top ---------- */
  var toTop = document.createElement("button");
  toTop.type = "button";
  toTop.className = "to-top";
  toTop.setAttribute("aria-label", "Back to top");
  toTop.textContent = "↑";
  document.body.appendChild(toTop);
  function onScroll() {
    toTop.classList.toggle("show", (window.scrollY || document.documentElement.scrollTop) > 420);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
  toTop.addEventListener("click", function () {
    window.scrollTo({ top: 0, behavior: "smooth" });
  });

  /* ---------- Active nav link ---------- */
  var page = (location.pathname.split("/").pop() || "index.html");
  if (page === "") page = "index.html";
  document.querySelectorAll(".nav-links a").forEach(function (a) {
    var href = (a.getAttribute("href") || "").split("/").pop();
    if (href === page) a.classList.add("active");
  });

  /* ---------- Changelog search ---------- */
  var searchInput = document.getElementById("changelog-search");
  var changelog = window.VMDE_CHANGELOG;
  if (searchInput && changelog) {
    searchInput.addEventListener("input", function () {
      var q = searchInput.value.trim().toLowerCase();
      var sel = document.getElementById("changelog-select");
      var body = document.getElementById("changelog-body");
      if (!sel || !body) return;
      if (!q) {
        changelog.fillVersionSelect();
        changelog.selectChannel(changelog.currentChannel, null);
        return;
      }
      var matches = changelog.releases.filter(function (r) {
        var hay = ((r.tag_name || "") + " " + (r.name || "") + " " + (r.body || "")).toLowerCase();
        return hay.indexOf(q) >= 0;
      });
      sel.innerHTML = "";
      matches.forEach(function (r) {
        var o = document.createElement("option");
        o.value = r.tag_name || "";
        o.textContent = r.tag_name || r.name || "Release";
        sel.appendChild(o);
      });
      if (!matches.length) {
        body.innerHTML = "<p class=\"changelog-no-results\">No releases match \u201c" +
          changelog.escapeHtml(searchInput.value) + "\u201d.</p>";
        return;
      }
      changelog.renderChangelog(matches[0]);
      sel.value = matches[0].tag_name;
    });
  }

  /* ---------- Screenshot gallery ---------- */
  var gallery = document.querySelector("[data-gallery]");
  if (gallery) {
    var shots = (window.VMDE_SCREENSHOTS || []);
    var track = gallery.querySelector(".gallery-track");
    var dotsWrap = gallery.querySelector(".gallery-dots");
    var empty = gallery.querySelector(".gallery-empty");
    var usable = [];

    function build() {
      if (!usable.length) {
        if (track) track.style.display = "none";
        if (dotsWrap) dotsWrap.style.display = "none";
        if (empty) empty.style.display = "block";
        return;
      }
      if (empty) empty.style.display = "none";
      usable.forEach(function (src) {
        var slide = document.createElement("div");
        slide.className = "gallery-slide";
        var img = document.createElement("img");
        img.src = src;
        img.alt = "VIVI Music DE screenshot";
        img.loading = "lazy";
        img.addEventListener("error", function () { slide.remove(); pruneDots(); });
        slide.appendChild(img);
        track.appendChild(slide);
      });
      if (dotsWrap) {
        usable.forEach(function (_, i) {
          var dot = document.createElement("button");
          dot.type = "button";
          dot.setAttribute("aria-label", "Screenshot " + (i + 1));
          if (i === 0) dot.classList.add("active");
          dot.addEventListener("click", function () {
            var slide = track.querySelectorAll(".gallery-slide")[i];
            if (slide) slide.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "center" });
          });
          dotsWrap.appendChild(dot);
        });
      }
    }
    function pruneDots() {
      if (!dotsWrap) return;
      var slides = track.querySelectorAll(".gallery-slide").length;
      var dots = dotsWrap.querySelectorAll("button");
      for (var i = dots.length - 1; i >= slides; i--) dots[i].remove();
    }
    if (track) {
      track.addEventListener("scroll", function () {
        var slides = track.querySelectorAll(".gallery-slide");
        var dots = dotsWrap ? dotsWrap.querySelectorAll("button") : [];
        var center = track.scrollLeft + track.clientWidth / 2;
        slides.forEach(function (s, i) {
          var r = s.getBoundingClientRect();
          var tr = track.getBoundingClientRect();
          var isActive = r.left <= tr.left + tr.width / 2 && r.right >= tr.left + tr.width / 2;
          if (isActive && dots[i]) {
            dots.forEach(function (d) { d.classList.remove("active"); });
            dots[i].classList.add("active");
          }
        });
      }, { passive: true });
    }
    shots.forEach(function (src) {
      var probe = new Image();
      probe.onload = function () { usable.push(src); build(); };
      probe.onerror = function () { /* skip broken */ };
      probe.src = src;
    });
    if (!shots.length) build();
  }
})();
