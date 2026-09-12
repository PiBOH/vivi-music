# Security Policy

## Supported versions

Only the latest [VIVI Music DE release](https://github.com/PiBOH/vivi-music/releases)
receives security fixes. Please reproduce the problem there before reporting it.

## Reporting a vulnerability

Please **do not** open a public issue for a security problem — report it
privately instead:

- GitHub private reporting: [Report a vulnerability](https://github.com/PiBOH/vivi-music/security/advisories/new)
- Or email **piboh.github@gmail.com**

Include the version (About screen), your OS, and a short description of the
impact. You will get an answer as soon as possible.

## Scope

VIVI Music DE is a desktop client: it talks to YouTube Music on your behalf and
keeps everything locally under `~/.vivimusic/`. Reports about session/cookie
handling, the device-sync relay (`wss://vivimusic-device-sync.onrender.com`),
the login WebView or the installers are all in scope.
