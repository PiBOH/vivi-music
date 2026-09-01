# VIVI Music DE — Error Codes

Every error that can appear in VIVI Music (desktop edition and the paired
mobile app) carries a code. If you hit an error, look its code up in the
table below to understand what happened and how to try to fix it.

- **Playback codes (1000–5999)** come from ExoPlayer / media3 and are shown
  on screen or in the playback logs (Settings → Content → Playback logs).
- **VIVI codes (E1000+)** are VIVI-specific errors (login, sync, backup…)
  and are shown with the `E` prefix in the error dialog.

---

## Playback error codes

| Code | Name | What it means | How to try to fix it |
| --- | --- | --- | --- |
| 1000 | UNSPECIFIED | An unknown playback error occurred. | Retry the track. If it keeps failing, check your connection and try a different audio quality. |
| 1001 | IO_NETWORK_CONNECTION_FAILED | The network connection failed while loading the stream. | Check your internet connection (and VPN/proxy settings). Retry the track. |
| 1002 | IO_NETWORK_CONNECTION_TIMEOUT | The connection timed out while loading the stream. | Your connection is slow or the server did not answer. Retry, or wait and try again. |
| 1003 | IO_INVALID_HTTP_CONTENT_TYPE | The server answered with an unexpected content type. | The stream URL is stale or the CDN rejected the request. Skip and come back to the track, or clear the stream cache. |
| 1004 | IO_BAD_HTTP_STATUS | The server answered with an HTTP error status (e.g. 403/404). | The stream URL is stale, restricted or the CDN blocked the request. Skip to the next track and back, or re-resolve the stream. |
| 1005 | IO_FILE_NOT_FOUND | The file or stream was not found. | The track may be unavailable. Try another track or refresh the library. |
| 1006 | IO_NO_PERMISSION | No permission to read the file/stream. | Check file permissions (local files) or your account access (YouTube Music). |
| 1007 | IO_CLEARTEXT_NOT_PERMITTED | Cleartext (HTTP) traffic is not allowed. | The URL is not HTTPS. Update VIVI or the track source. |
| 1008 | IO_READ_POSITION_OUT_OF_RANGE | Read position went out of range while loading. | Usually a temporary glitch — retry the track. |
| 1009 | IO_UNSPECIFIED | An unspecified I/O error — typically the YouTube Music CDN flagging the request (bot protection) or a stale stream URL. | Skip to the next track and back to re-resolve the URL, or disable the animated canvas, or try again later. A browser-like User-Agent is already sent; updating VIVI helps. |
| 2000 | IO_UNSPECIFIED (video) | Same as 1009, shown as "Video non disponibile" / "Source error" when the stream cannot be played. | Skip the track and come back (forces a fresh URL), or lower the audio quality. |
| 3000 | PARSING_CONTAINER_MALFORMED | The media container is malformed. | The stream data is corrupt — skip the track and retry. |
| 3001 | PARSING_MANIFEST_MALFORMED | The manifest (e.g. M3U8/DASH) is malformed. | The stream is broken — skip and retry, or update VIVI. |
| 3002 | PARSING_CONTAINER_UNSUPPORTED | The container format is not supported. | The format is unsupported by the player — try a different audio quality. |
| 3003 | PARSING_MANIFEST_UNSUPPORTED | The manifest format is not supported. | The stream uses an unsupported format — update VIVI. |
| 4000 | DECODER_INIT_FAILED | The audio/video decoder could not be initialized. | Update your audio drivers (Windows) or restart VIVI. |
| 4001 | DECODER_QUERY_FAILED | The decoder capabilities could not be queried. | Update your drivers/system and restart VIVI. |
| 4002 | DECODING_FAILED | Decoding the media failed. | The stream may be corrupt — skip and retry. |
| 4003 | DECODING_FORMAT_EXCEEDS_CAPABILITIES | The format exceeds what your device can decode. | Lower the audio quality (Settings → Player & audio → Audio quality). |
| 4004 | DECODING_FORMAT_UNSUPPORTED | The format is not supported by your device. | Lower the audio quality or update your system. |
| 5000 | AUDIO_TRACK_INIT_FAILED | The audio output track could not be initialized. | Check your audio output device and restart VIVI. |
| 5001 | AUDIO_TRACK_WRITE_FAILED | Writing audio to the output failed. | Check your audio device — another app may be holding it. Restart VIVI. |
| 6000 | DRM_UNSPECIFIED | An unknown DRM error occurred. | Retry the track. |
| 6001 | DRM_SCHEME_UNSUPPORTED | The DRM scheme is not supported. | The track is DRM-protected with an unsupported scheme — nothing to do. |
| 6002 | DRM_PROVISIONING_FAILED | DRM provisioning failed. | Check your connection and retry. |
| 6003 | DRM_CONTENT_ERROR | DRM content error. | Retry the track. |
| 6004 | DRM_LICENSE_ACQUISITION_FAILED | The DRM license could not be acquired. | Check your connection/account and retry. |
| 6005 | DRM_DISALLOWED_OPERATION | The DRM operation is not allowed. | The track is restricted — nothing to do. |
| 6006 | DRM_SYSTEM_ERROR | The DRM system reported an error. | Update your system and retry. |
| 6007 | DRM_DEVICE_REVOKED | The DRM device was revoked. | The device is blocked by the DRM provider — contact support. |
| 6008 | DRM_LICENSE_EXPIRED | The DRM license expired. | Re-acquire the license by replaying the track. |

## VIVI-specific error codes

| Code | Name | What it means | How to try to fix it |
| --- | --- | --- | --- |
| E1000 | LOGIN_VALIDATION_FAILED | The saved session could not be validated (missing/invalid cookies, e.g. expired `SAPISIDHASH`). | Sign in again: Settings → Account → "Sign in with Google" (or paste fresh cookies with the manual method). |
| E1001 | LOGIN_WEBVIEW_CLOSED | The sign-in window was closed before the login completed. | Try again, or use the manual cookie method. |
| E1002 | LOGIN_WEBVIEW_UNAVAILABLE | The embedded sign-in window is not available on this system. | Use the manual cookie method (open music.youtube.com in your browser). |
| E1003 | LOGIN_UNAUTHORIZED | YouTube Music answered 401 — the request is missing a valid credential. | Your cookies/session are stale. Sign in again. |
| E1004 | SYNC_PAIR_CODE_INVALID | The pairing code is invalid or expired. | Generate a new pairing code on the desktop and retry. |
| E1005 | SYNC_CONNECTION_FAILED | Could not connect to the sync relay/LAN server. | Check that both devices are on the same network (LAN) or that the relay is reachable; verify the server URL in Device sync. |
| E1006 | SYNC_PEER_DISCONNECTED | The paired device went offline. | Wake the device / check the connection; it will reconnect automatically. |
| E1007 | BACKUP_INTEGRITY_FAILED | The backup file failed the integrity check. | The file is corrupt or was modified — restore a different backup. |
| E1008 | BACKUP_RESTORE_FAILED | Restoring the backup failed. | The file may be from an incompatible version — check the date/version and try again. |
| E1009 | UPDATE_DOWNLOAD_FAILED | Downloading the update failed. | Check your connection and retry; the download resumes from where it stopped. |
| E1010 | UPDATE_INSTALL_FAILED | Installing the update failed. | Close VIVI and run the downloaded installer manually, or restart and retry. |
| E1011 | IMPORT_EQ_FAILED | The equalizer profile could not be imported. | The file is not a valid AutoEQ `ParametricEQ.txt` — check the file and retry. |
| E1012 | AI_TRANSLATION_FAILED | The AI lyrics translation request failed. | Check the provider URL, your API key and the network; see Settings → AI Lyrics Translation. |

---

If your error code is not listed here, report it (with the exact code and
message) in the [GitHub issues](https://github.com/PiBOH/vivi-music/issues)
so it can be added to this table.
