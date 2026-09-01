/* VIVI Music DE website — interactive error-code reference.
 * Loads ERRORS.md from the repository (branch vivi-music-de), parses its two
 * Markdown tables (Playback / VIVI-specific) and renders them as searchable,
 * filterable rows. Falls back to a link if the file can't be fetched. */
(function () {
  "use strict";

  var ERRORS_RAW =
    "https://raw.githubusercontent.com/PiBOH/vivi-music/vivi-music-de/ERRORS.md";
  var ERRORS_PAGE =
    "https://github.com/PiBOH/vivi-music/blob/vivi-music-de/ERRORS.md";

  var body = document.getElementById("errors-body");
  if (!body) return;

  var rows = [];        // parsed {code, name, meaning, fix, cat}
  var activeCat = "all";
  var query = "";

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
  }

  /* ---- Markdown table parser (the two tables in ERRORS.md) ---- */
  function parseErrors(md) {
    var lines = String(md || "").replace(/\r\n/g, "\n").split("\n");
    var out = [];
    var cat = "playback";
    var i;
    for (i = 0; i < lines.length; i++) {
      var t = lines[i].trim();
      if (/^##\s+/i.test(t)) {
        cat = /vivi/i.test(t) ? "vivi" : "playback";
        continue;
      }
      if (t.charAt(0) !== "|") continue;
      var cells = t.replace(/^\||\|$/g, "").split("|").map(function (c) {
        return c.trim();
      });
      // skip header row and the |---| separator
      if (cells.length < 4) continue;
      if (/^[-: ]+$/.test(cells[0])) continue;
      var code = cells[0];
      if (!/^\d+$/.test(code) && !/^E\d+$/.test(code)) continue;
      out.push({
        code: code,
        name: cells[1] || "",
        meaning: cells[2] || "",
        fix: cells[3] || "",
        cat: cat,
      });
    }
    return out;
  }

  /* ---- Rendering ---- */
  function rowHtml(r) {
    var cls = r.cat === "vivi" ? "code-vivi" : "code-playback";
    return (
      '<tr data-cat="' + r.cat + '">' +
      '<td><button type="button" class="err-code ' + cls + '" data-copy="' +
        escapeHtml(r.code) + '" title="Copy ' + escapeHtml(r.code) + '">' +
        escapeHtml(r.code) + "</button></td>" +
      "<td><strong>" + escapeHtml(r.name) + "</strong></td>" +
      "<td>" + escapeHtml(r.meaning) + "</td>" +
      "<td>" + escapeHtml(r.fix) + "</td>" +
      "</tr>"
    );
  }

  function visibleRows() {
    var q = query.toLowerCase();
    return rows.filter(function (r) {
      if (activeCat !== "all" && r.cat !== activeCat) return false;
      if (!q) return true;
      var hay = (r.code + " " + r.name + " " + r.meaning + " " + r.fix).toLowerCase();
      return hay.indexOf(q) >= 0;
    });
  }

  function updateCount(n, total) {
    var el = document.querySelector("[data-errors-count]");
    if (el) el.textContent = n + " / " + total;
    var tot = document.querySelector("[data-errors-total]");
    if (tot) tot.textContent = total + " error codes.";
  }

  function render() {
    var visible = visibleRows();
    if (!rows.length) return;
    if (!visible.length) {
      body.innerHTML =
        '<p class="errors-empty">No error codes match “' + escapeHtml(query) +
        '”. Try a code (e.g. <code>1009</code>) or a word like <code>login</code>.</p>';
      updateCount(0, rows.length);
      return;
    }
    body.innerHTML =
      '<div class="errors-table-wrap"><table class="errors-table">' +
      "<thead><tr>" +
      "<th>Code</th><th>Name</th><th>What it means</th><th>How to try to fix it</th>" +
      "</tr></thead><tbody>" + visible.map(rowHtml).join("") +
      "</tbody></table></div>";
    updateCount(visible.length, rows.length);
    bindCopy();
  }

  /* ---- Copy code to clipboard ---- */
  function bindCopy() {
    var btns = body.querySelectorAll("[data-copy]");
    Array.prototype.forEach.call(btns, function (btn) {
      btn.addEventListener("click", function () {
        var code = btn.getAttribute("data-copy");
        var done = function () {
          var old = btn.textContent;
          btn.textContent = "Copied!";
          setTimeout(function () { btn.textContent = old; }, 1200);
        };
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(code).then(done, done);
        } else {
          var ta = document.createElement("textarea");
          ta.value = code;
          document.body.appendChild(ta);
          ta.select();
          try { document.execCommand("copy"); } catch (e) { /* ignore */ }
          document.body.removeChild(ta);
          done();
        }
      });
    });
  }

  /* ---- Controls ---- */
  var search = document.getElementById("errors-search");
  if (search) {
    search.addEventListener("input", function () {
      query = search.value.trim();
      render();
    });
  }
  var tabs = document.querySelectorAll("[data-cat]");
  Array.prototype.forEach.call(tabs, function (tab) {
    tab.addEventListener("click", function () {
      activeCat = tab.getAttribute("data-cat");
      Array.prototype.forEach.call(tabs, function (t) {
        var on = t === tab;
        t.classList.toggle("active", on);
        t.setAttribute("aria-selected", on ? "true" : "false");
      });
      render();
    });
  });

  /* ---- Load ---- */
  fetch(ERRORS_RAW)
    .then(function (res) {
      if (!res.ok) throw new Error("fetch");
      return res.text();
    })
    .then(function (md) {
      rows = parseErrors(md);
      if (!rows.length) throw new Error("parse");
      render();
    })
    .catch(function () {
      body.innerHTML =
        '<p class="errors-empty">Could not load the error codes from GitHub. ' +
        'See the <a href="' + ERRORS_PAGE + '">ERRORS.md</a> file directly instead.</p>';
    });
})();
