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
  window.VM = { $: $, $$: $$ };

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
    if (!("IntersectionObserver" in window)) {
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
  /* ---------- Order releases by publish date (chronological), not by the ----------
   * version numbers in the tag. GitHub returns releases newest-first by publish
   * date; combined tags (e.g. 6.4.45_DE-1.50.22 vs 6.0.6.1_DE-1.50.23) are NOT
   * string-comparable across versioning-scheme changes, so sorting by the tag
   * would pick an OLD release as "latest". The site must always resolve
   * "latest" as the most recently RELEASED one (chronology wins). */
  function byPublishedDesc(a, b) {
    var at = new Date(a.published_at || 0).getTime();
    var bt = new Date(b.published_at || 0).getTime();
    return bt - at;
  }

  window.vmReleases = function (n) {
    return window.vmGH.json("https://api.github.com/repos/" + REPO + "/releases?per_page=" + (n || 6))
      .then(function (list) { return (list || []).filter(Boolean).sort(byPublishedDesc); });
  };
  /* Fetch EVERY release, oldest included. The GitHub API caps per_page at
     100, so one call only ever returns the newest page; the changelog needs
     the full history ("first to last") to be searchable, so keep requesting
     the next page until a short one comes back, then sort by publish date. */
  window.vmAllReleases = function () {
    var PER = 100;
    var url = "https://api.github.com/repos/" + REPO + "/releases?per_page=" + PER + "&page=";
    var all = [];
    function next(page) {
      return window.vmGH.json(url + page).then(function (list) {
        var batch = (list || []).filter(Boolean);
        all = all.concat(batch);
        if (batch.length === PER) return next(page + 1);
        return all.sort(byPublishedDesc);
      });
    }
    return next(1);
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
    var html = "", inUl = false, inOl = false, inCode = false, codeBuf = null;
    var closeLists = function () {
      if (inUl) { html += "</ul>"; inUl = false; }
      if (inOl) { html += "</ol>"; inOl = false; }
    };
    src.split(/\r?\n/).forEach(function (raw) {
      var line = raw.replace(/\s+$/, "");
      var t = line.trim();
      if (/^```/.test(t)) {
        if (inCode) {
          /* closing fence: render the collected lines as a code block */
          html += "<pre><code>" + esc(codeBuf.join("\n")) + "</code></pre>";
          inCode = false;
          codeBuf = null;
        } else {
          inCode = true;
          codeBuf = [];
          closeLists();
        }
        return;
      }
      if (inCode) { codeBuf.push(line); return; }
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

  /* ---------- footer year ---------- */
  $$("[data-year]").forEach(function (el) { el.textContent = new Date().getFullYear(); });
  /* ---------- screenshot gallery helper ----------
     Lists .webp / .png / .jpg files in images/screenshots via the GitHub
     contents API and renders 16:9 cards into `container`. Freshly pushed
     images may not be on Pages yet, so every <img> falls back to
     raw.githubusercontent.com once. */
  function vmShotsLightbox(container) {
    var doc = container.ownerDocument;
    var lb = doc.createElement('div');
    lb.className = 'vm-lightbox';
    lb.setAttribute('role', 'dialog');
    lb.setAttribute('aria-label', 'Screenshot preview');
    lb.innerHTML =
      '<button class="vm-lb-nav vm-lb-prev" aria-label="Previous screenshot">' +
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>' +
      '</button>' +
      '<button class="vm-lb-nav vm-lb-next" aria-label="Next screenshot">' +
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"/></svg>' +
      '</button>' +
      '<span class="vm-lb-count"></span>' +
      '<img class="vm-lb-img" src="" alt="">';
    doc.body.appendChild(lb);

    function cards() {
      return Array.prototype.slice.call(container.querySelectorAll('.gshot'));
    }
    var idx = -1;
    function show(i) {
      var list = cards();
      if (!list.length) return;
      idx = (i + list.length) % list.length;
      var card = list[idx];
      var img = lb.querySelector('.vm-lb-img');
      img.src = card.getAttribute('data-src');
      var cap = card.querySelector('figcaption');
      img.alt = cap ? cap.textContent : '';
      lb.querySelector('.vm-lb-count').textContent = (idx + 1) + ' / ' + list.length;
      lb.classList.add('open');
    }
    function prev() { show(idx - 1); }
    function next() { show(idx + 1); }

    container.addEventListener('click', function (e) {
      var card = e.target.closest ? e.target.closest('.gshot') : null;
      if (!card) return;
      show(cards().indexOf(card));
    });
    lb.addEventListener('click', function (e) {
      var nav = e.target.closest ? e.target.closest('.vm-lb-nav') : null;
      if (nav) { nav.classList.contains('vm-lb-prev') ? prev() : next(); return; }
      lb.classList.remove('open');
    });
    doc.addEventListener('keydown', function (e) {
      if (!lb.classList.contains('open')) return;
      if (e.key === 'Escape') lb.classList.remove('open');
      else if (e.key === 'ArrowLeft') prev();
      else if (e.key === 'ArrowRight') next();
    });

    /* Touch swipe (phones/tablets/touch laptops): a horizontal drag switches
       to the next/previous screenshot, matching the arrow buttons and the
       Arrow keys. Vertical drags are ignored so the page can still scroll. */
    var touchX = null, touchY = null;
    lb.addEventListener('touchstart', function (e) {
      if (!lb.classList.contains('open') || e.touches.length !== 1) { touchX = touchY = null; return; }
      touchX = e.touches[0].clientX;
      touchY = e.touches[0].clientY;
    }, { passive: true });
    lb.addEventListener('touchend', function (e) {
      if (touchX === null || !lb.classList.contains('open')) { touchX = touchY = null; return; }
      var t = e.changedTouches[0];
      var dx = t.clientX - touchX;
      var dy = t.clientY - touchY;
      touchX = touchY = null;
      // 40 px threshold + mostly-horizontal so a scroll/zoom never navigates.
      if (Math.abs(dx) > 40 && Math.abs(dx) > Math.abs(dy)) {
        if (dx < 0) next();
        else prev();
      }
    }, { passive: true });
  }

  window.vmShots = function (container, opts) {
    opts = opts || {};
    if (!container) return;
    var API = 'https://api.github.com/repos/' + REPO + '/contents/.websitede/images/screenshots?ref=vivi-music-de';
    var PAGES = 'https://piboh.github.io/vivi-music/images/screenshots/';
    var RAW = 'https://raw.githubusercontent.com/' + REPO + '/vivi-music-de/.websitede/images/screenshots/';

    function pretty(name) {
            return name.replace(/\.(webp|png|jpe?g)$/i, '').replace(/[-_]+/g, ' ').replace(/\b\w/g, function (c) { return c.toUpperCase(); });
    }
    function empty() {
      container.innerHTML = '<p class="hnote" style="grid-column:1/-1">' + (opts.emptyText || 'No screenshots here yet.') + '</p>';
      if (opts.emptyHideSection) {
        var sec = container.closest('section');
        if (sec) sec.style.display = 'none';
      }
    }

    return window.vmGH.json(API)
      .then(function (files) {
                var shots = (files || []).filter(function (f) { return f.type === 'file' && /\.(webp|png|jpe?g)$/i.test(f.name); })
          .sort(function (a, b) { return a.name.localeCompare(b.name); });
        /* Same shot in two formats (e.g. shot.webp + shot.png) shows once,
           preferring .webp, then .png, then .jpg. */
        var best = {};
        shots.forEach(function (f) {
          var stem = f.name.replace(/\.(webp|png|jpe?g)$/i, '').toLowerCase();
          var rank = { webp: 0, png: 1, jpg: 2, jpeg: 2 }[f.name.split('.').pop().toLowerCase()];
          if (best[stem] === undefined || rank < best[stem]) best[stem] = rank;
        });
        shots = shots.filter(function (f) {
          var stem = f.name.replace(/\.(webp|png|jpe?g)$/i, '').toLowerCase();
          var rank = { webp: 0, png: 1, jpg: 2, jpeg: 2 }[f.name.split('.').pop().toLowerCase()];
          return rank === best[stem];
        });
        if (opts.max > 0) shots = shots.slice(0, opts.max);
        if (!shots.length) { empty(); return []; }
        var html = '';
        shots.forEach(function (f) {
          var enc = encodeURIComponent(f.name);
          var alt = pretty(f.name);
          html += '<figure class="gshot" data-src="' + PAGES + enc + '" data-fallback="' + RAW + enc + '">' +
            '<img src="' + PAGES + enc + '" alt="VIVI Music DE screenshot — ' + alt + '" loading="lazy" decoding="async">' +
            '<figcaption>' + alt + '</figcaption></figure>';
        });
        container.innerHTML = html;
        Array.prototype.forEach.call(container.querySelectorAll('img'), function (img) {
          img.addEventListener('error', function () {
            var fb = img.getAttribute('data-fallback');
            if (fb && img.src.indexOf(fb) === -1) img.src = fb;
          });
        });
        if (opts.lightbox !== false) vmShotsLightbox(container);
        return shots;
      })
      .catch(function () { empty(); return []; });
  };

})();
