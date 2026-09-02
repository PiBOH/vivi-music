/* main.js — shared bits for the VIVI Music DE site
   theme, nav, scroll reveals, copy buttons, os sniffing,
   github data + a small markdown renderer.
   vanilla js on purpose: this site has to stay pushable to
   github pages with zero build step. */

(function () {
  "use strict";

  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
  var root = document.documentElement;
  var RM = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  window.VM = { $: $, $$: $$, RM: RM };

  /* ---------- theme (toggle; the no-flash init lives in each page's <head>) ---------- */
  function setThemeIcon(t) {
    var b = $("[data-theme-toggle]");
    if (!b) return;
    b.textContent = t === "light" ? "☾" : "☀";
    b.setAttribute("aria-label", t === "light" ? "Switch to dark theme" : "Switch to light theme");
  }
  (function initTheme() {
    var current = root.getAttribute("data-theme") === "light" ? "light" : "dark";
    var b = $("[data-theme-toggle]");
    if (!b) return;
    setThemeIcon(current);
    b.addEventListener("click", function () {
      var next = root.getAttribute("data-theme") === "light" ? "dark" : "light";
      if (next === "light") root.setAttribute("data-theme", "light");
      else root.removeAttribute("data-theme");
      try { localStorage.setItem("vmde-theme", next); } catch (e) { /* private mode, whatever */ }
      setThemeIcon(next);
    });
  })();

  /* ---------- mobile nav ---------- */
  (function initNav() {
    var toggle = $("[data-nav-toggle]");
    var nav = $(".nav");
    if (!toggle || !nav) return;
    toggle.addEventListener("click", function () {
      var open = nav.classList.toggle("open");
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
      toggle.textContent = open ? "✕" : "☰";
    });
    $$(".nav-links a").forEach(function (a) {
      a.addEventListener("click", function () {
        nav.classList.remove("open");
        toggle.setAttribute("aria-expanded", "false");
        toggle.textContent = "☰";
      });
    });
  })();

  /* ---------- scroll reveals ---------- */
  (function initReveal() {
    var els = $$(".reveal");
    if (!els.length) return;
    if (RM || !("IntersectionObserver" in window)) {
      els.forEach(function (el) { el.classList.add("in"); });
      return;
    }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
      });
    }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" });
    els.forEach(function (el) {
      if (el.dataset.d) el.style.transitionDelay = el.dataset.d + "ms";
      io.observe(el);
    });
  })();

  /* ---------- copy buttons ---------- */
  function fallbackCopy(txt, done) {
    var ta = document.createElement("textarea");
    ta.value = txt;
    ta.style.position = "fixed";
    ta.style.opacity = "0";
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand("copy"); done(); } catch (e) { /* give up */ }
    document.body.removeChild(ta);
  }
  function copyText(txt, btn) {
    var done = function () {
      if (!btn) return;
      var old = btn.dataset.old || btn.textContent;
      btn.dataset.old = old;
      btn.textContent = "copied ✓";
      btn.classList.add("ok");
      setTimeout(function () { btn.textContent = old; btn.classList.remove("ok"); }, 1400);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(txt).then(done)["catch"](function () { fallbackCopy(txt, done); });
    } else {
      fallbackCopy(txt, done);
    }
  }
  window.vmCopy = copyText;
  (function initCopy() {
    $$("pre.code").forEach(function (pre) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "copy-btn";
      b.textContent = "copy";
      b.addEventListener("click", function () { copyText(pre.innerText.replace(/\ncopy\n?$/i, ""), b); });
      pre.appendChild(b);
    });
    document.addEventListener("click", function (e) {
      var t = e.target.closest ? e.target.closest("[data-copy]") : null;
      if (!t) return;
      e.preventDefault();
      var val = t.dataset.copy || "";
      copyText(val, t);
    });
  })();

  /* ---------- os sniffing (used by downloads + install guide) ---------- */
  /* device kind straight from the user agent: the layout follows the
     device (html.vm-phone / html.vm-tablet, set in each page head),
     not just the viewport width. */
  window.vmDeviceKind = function () {
    var ua = navigator.userAgent || "";
    var tablet = /iPad|Tablet|PlayBook|Kindle|Silk/i.test(ua) || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1);
    if (tablet) return "tablet";
    if (/Mobi|Android|iPhone|iPod|Windows Phone|IEMobile|Opera Mini|BlackBerry/i.test(ua)) return "phone";
    return "desktop";
  };

  window.vmDetectOS = function () {
    var ua = navigator.userAgent || "";
    if (/Windows/i.test(ua)) return { id: "windows", name: "Windows", icon: "🪟" };
    if (/iPhone|iPad|iPod/i.test(ua)) return { id: "ios", name: "iOS / iPadOS", icon: "🍏" };
    if (/Mac/i.test(ua)) return { id: "macos", name: "macOS", icon: "🍎" };
    if (/Android/i.test(ua)) return { id: "android", name: "Android", icon: "🤖" };
    if (/Linux/i.test(ua)) return { id: "linux", name: "Linux", icon: "🐧" };
    return { id: "unknown", name: "something unrecognizable", icon: "❔" };
  };

  /* ---------- github helpers ---------- */
  var REPO = "PiBOH/vivi-music";
  window.VM_REPO = REPO;
  window.vmGH = {
    REPO: REPO,
    json: function (url) {
      return fetch(url, { headers: { Accept: "application/vnd.github+json" } }).then(function (r) {
        if (!r.ok) throw new Error("HTTP " + r.status);
        return r.json();
      });
    },
    raw: function (url) {
      return fetch(url).then(function (r) {
        if (!r.ok) throw new Error("HTTP " + r.status);
        return r.text();
      });
    }
  };
  /* ---------- Order releases by the version in the tag, not by GitHub order ----------
   * GitHub returns releases newest-first by publish date, which can disagree with the
   * actual version numbers (backdated releases, re-published tags, force pushes).
   * The site must always resolve "latest" from the tag's version, so we sort
   * descending by its numeric parts (tag format like 6.4.41_DE-1.41.15-nightly). */
  function versionParts(tag) {
    var m = String(tag || "").match(/\d+(?:\.\d+)*/g);
    if (!m) return [];
    return m.join(".").split(".").map(function (n) { return parseInt(n, 10) || 0; });
  }
  function compareParts(a, b) {
    var len = Math.max(a.length, b.length);
    for (var i = 0; i < len; i++) {
      var av = a[i] || 0;
      var bv = b[i] || 0;
      if (av !== bv) return av - bv;
    }
    return 0;
  }
  function byVersionDesc(a, b) {
    return compareParts(versionParts(b.tag_name), versionParts(a.tag_name));
  }

  window.vmReleases = function (n) {
    return window.vmGH.json("https://api.github.com/repos/" + REPO + "/releases?per_page=" + (n || 6))
      .then(function (list) { return (list || []).filter(Boolean).sort(byVersionDesc); });
  };
  window.vmFindAsset = function (release, re) {
    var assets = (release && release.assets) || [];
    for (var i = 0; i < assets.length; i++) if (re.test(assets[i].name)) return assets[i];
    return null;
  };
  window.vmFmtDate = function (iso) {
    try {
      return new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });
    } catch (e) { return iso || ""; }
  };

  /* ---------- tiny markdown -> html (good enough for release notes) ---------- */
  window.vmEsc = function (s) {
    return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  };
  window.vmMdLite = function (md) {
    var src = String(md || "");
    var esc = window.vmEsc;
    var inline = function (s) {
      s = esc(s);
      s = s.replace(/`([^`]+)`/g, "<code>$1</code>");
      s = s.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
      s = s.replace(/\[([^\]]+)\]\((https?:[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
      s = s.replace(/(^|[^*\w])\*([^*\n]+)\*/g, "$1<em>$2</em>");
      return s;
    };
    var html = "", inUl = false, inOl = false, inCode = false;
    var closeLists = function () {
      if (inUl) { html += "</ul>"; inUl = false; }
      if (inOl) { html += "</ol>"; inOl = false; }
    };
    src.split(/\r?\n/).forEach(function (raw) {
      var line = raw.replace(/\s+$/, "");
      var t = line.trim();
      if (/^```/.test(t)) { inCode = !inCode; closeLists(); return; }
      if (inCode) { closeLists(); return; }
      if (!t) { closeLists(); return; }
      var h = t.match(/^(#{1,4})\s+(.*)/);
      if (h) { closeLists(); html += '<div class="mdh">' + inline(h[2]) + "</div>"; return; }
      var ul = t.match(/^[-*]\s+(.*)/);
      if (ul) { if (!inUl) { closeLists(); html += "<ul>"; inUl = true; } html += "<li>" + inline(ul[1]) + "</li>"; return; }
      var ol = t.match(/^\d+[.)]\s+(.*)/);
      if (ol) { if (!inOl) { closeLists(); html += "<ol>"; inOl = true; } html += "<li>" + inline(ol[1]) + "</li>"; return; }
      closeLists();
      html += "<p>" + inline(line) + "</p>";
    });
    closeLists();
    return html;
  };

  /* ---------- footer year + ticker loop ---------- */
  $$("[data-year]").forEach(function (el) { el.textContent = new Date().getFullYear(); });
  (function initTicker() {
    var tr = $(".ticker-track");
    if (tr) tr.innerHTML += tr.innerHTML; /* duplicate for a seamless -50% loop */
  })();
})();
