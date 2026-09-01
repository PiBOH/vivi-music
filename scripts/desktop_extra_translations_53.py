# -*- coding: utf-8 -*-
"""Desktop-only translation for the "canvas" player-background option label.

Merged into the desktop `Localization.kt` by
`scripts/generate_desktop_localization.py`. Kept here because the Android
source has no string resource named `canvas`.
"""

_LANGS = ["ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "de", "el", "es", "et", "eu", "fi", "fil", "fr",
          "hi", "hr", "hu", "id", "it", "ja", "km", "ko", "lt", "ml", "ms", "nb", "nl", "pa", "pl", "pt", "ro",
          "ru", "sk", "sl", "sr", "sv", "ta", "te", "th", "tr", "uk", "vi", "zh-rCN", "zh-rTW"]


def _all(value):
    return {k: value for k in _LANGS}


EXTRA_TRANSLATIONS = {
    "canvas": {
        "ar": "لوحة الرسم", "as": "কেন্ভাচ", "az": "Kətan", "be": "Палатно",
        "bg": "Канвас", "bn": "ক্যানভাস", "bs": "Platno", "ca": "Llenç",
        "cs": "Plátno", "de": "Canvas", "el": "Καμβάς", "es": "Lienzo",
        "et": "Lõuend", "eu": "Oihala", "fi": "Kangas", "fil": "Canvas",
        "fr": "Toile", "hi": "कैनवास", "hr": "Platno", "hu": "Vászon",
        "id": "Kanvas", "it": "Canvas", "ja": "キャンバス", "km": "ផ្ទាំងក្រណាត់",
        "ko": "캔버스", "lt": "Drobė", "ml": "ക്യാൻവാസ്", "ms": "Kanvas",
        "nb": "Lerret", "nl": "Canvas", "pa": "ਕੈਨਵਸ", "pl": "Płótno",
        "pt": "Tela", "ro": "Pânză", "ru": "Канвас", "sk": "Plátno",
        "sl": "Platno", "sr": "Платно", "sv": "Canvas", "ta": "கேன்வாஸ்",
        "te": "కాన్వాస్", "th": "แคนวาส", "tr": "Tuval", "uk": "Полотно",
        "vi": "Tranh nền", "zh-rCN": "画布", "zh-rTW": "畫布",
    },
}