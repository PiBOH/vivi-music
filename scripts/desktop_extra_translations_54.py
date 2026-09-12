# -*- coding: utf-8 -*-
"""Desktop-only translations for the EQ band range labels shown next to each
band's center frequency in the live equalizer editor (Sub-bass / Bass / Low
mid / Mid / High mid / Treble).

Merged into the desktop `Localization.kt` by
`scripts/generate_desktop_localization.py`.
"""

_LANGS = ["ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "de", "el", "es", "et", "eu", "fi", "fil", "fr",
          "hi", "hr", "hu", "id", "it", "ja", "km", "ko", "lt", "ml", "ms", "nb", "nl", "pa", "pl", "pt", "ro",
          "ru", "sk", "sl", "sr", "sv", "ta", "te", "th", "tr", "uk", "vi", "zh-rCN", "zh-rTW"]


def _all(value):
    return {k: value for k in _LANGS}


EXTRA_TRANSLATIONS = {
    "eq_range_sub_bass": {
        "ar": "تحت الجهير", "as": "চুবে-বাছ", "az": "Sub-bass", "be": "Суб-бас",
        "bg": "Суб-бас", "bn": "সাব-বাস", "bs": "Sub-bas", "ca": "Sub-greus",
        "cs": "Sub-bas", "de": "Subbass", "el": "Υπο-μπάσο", "es": "Subgraves",
        "et": "Subbass", "eu": "Azpi-baxu", "fi": "Subbasso", "fil": "Sub-bass",
        "fr": "Sub-basse", "hi": "सब-बास", "hr": "Sub-bas", "hu": "Szub-basszus",
        "id": "Sub-bass", "it": "Sub-bassi", "ja": "サブバス", "km": "ស្ងាត់ជ្រុល",
        "ko": "서브베이스", "lt": "Sub-bosas", "ml": "സബ്-ബാസ്", "ms": "Sub-bes",
        "nb": "Sub-bass", "nl": "Sub-bas", "pa": "ਸਬ-ਬਾਸ", "pl": "Sub-bas",
        "pt": "Sub-graves", "ro": "Sub-bas", "ru": "Суб-бас", "sk": "Sub-bas",
        "sl": "Sub-bas", "sr": "Суб-бас", "sv": "Subbas", "ta": "சப்-பாஸ்",
        "te": "సబ్-బాస్", "th": "ซับเบส", "tr": "Sub-bas", "uk": "Суб-бас",
        "vi": "Sub-bass", "zh-rCN": "超低音", "zh-rTW": "超低音",
    },
    "eq_range_bass": {
        "ar": "جهير", "as": "বাছ", "az": "Bas", "be": "Бас",
        "bg": "Бас", "bn": "বাস", "bs": "Bas", "ca": "Greus",
        "cs": "Basy", "de": "Bass", "el": "Μπάσα", "es": "Graves",
        "et": "Bass", "eu": "Baxu", "fi": "Basso", "fil": "Bass",
        "fr": "Basses", "hi": "बास", "hr": "Bas", "hu": "Basszus",
        "id": "Bass", "it": "Bassi", "ja": "低音", "km": "សំឡេងបាស",
        "ko": "베이스", "lt": "Bosai", "ml": "ബാസ്", "ms": "Bes",
        "nb": "Bass", "nl": "Bas", "pa": "ਬਾਸ", "pl": "Bas",
        "pt": "Graves", "ro": "Bas", "ru": "Бас", "sk": "Basy",
        "sl": "Bas", "sr": "Бас", "sv": "Bas", "ta": "பாஸ்",
        "te": "బాస్", "th": "เบส", "tr": "Bas", "uk": "Бас",
        "vi": "Trầm", "zh-rCN": "低频", "zh-rTW": "低音",
    },
    "eq_range_low_mid": {
        "ar": "وسط منخفض", "as": "নিম্ন মধ্য", "az": "Aşağı orta", "be": "Ніжняя сярэдзіна",
        "bg": "Нисък среден", "bn": "লো-মিড", "bs": "Niski srednji", "ca": "Mitjans baixos",
        "cs": "Nižší středy", "de": "Tiefe Mitten", "el": "Χαμηλά μέσα", "es": "Medios graves",
        "et": "Alumine kesk", "eu": "Ertain baxuak", "fi": "Alakäänet", "fil": "Low mid",
        "fr": "Bas médium", "hi": "लो-मिड", "hr": "Niske srednje", "hu": "Alsó közép",
        "id": "Mid rendah", "it": "Medi bassi", "ja": "低中音", "km": "កណ្តាលទាប",
        "ko": "저음 미드", "lt": "Žemieji viduriai", "ml": "ലോ-മിഡ്", "ms": "Mid rendah",
        "nb": "Lav-mellom", "nl": "Lage midden", "pa": "ਲੋ-ਮਿਡ", "pl": "Niższe średnie",
        "pt": "Médios graves", "ro": "Mid-jos", "ru": "Низкая середина", "sk": "Nižšie stredy",
        "sl": "Nizke sredine", "sr": "Ниске средње", "sv": "Låg-mellan", "ta": "லோ-மிட்",
        "te": "లో-మిడ్", "th": "กลางต่ำ", "tr": "Düşük orta", "uk": "Нижня середина",
        "vi": "Trung trầm", "zh-rCN": "中低频", "zh-rTW": "低中音",
    },
    "eq_range_mid": {
        "ar": "وسط", "as": "মধ্য", "az": "Orta", "be": "Сярэдзіна",
        "bg": "Среден", "bn": "মিড", "bs": "Srednji", "ca": "Mitjans",
        "cs": "Středy", "de": "Mitten", "el": "Μέσα", "es": "Medios",
        "et": "Kesksed", "eu": "Ertainak", "fi": "Keskialue", "fil": "Mid",
        "fr": "Médium", "hi": "मिड", "hr": "Srednje", "hu": "Közép",
        "id": "Mid", "it": "Medi", "ja": "中音", "km": "កណ្តាល",
        "ko": "미드", "lt": "Viduriai", "ml": "മിഡ്", "ms": "Mid",
        "nb": "Mellom", "nl": "Midden", "pa": "ਮਿਡ", "pl": "Średnie",
        "pt": "Médios", "ro": "Mid", "ru": "Середина", "sk": "Stredy",
        "sl": "Sredine", "sr": "Средње", "sv": "Mellan", "ta": "மிட்",
        "te": "మిడ్", "th": "กลาง", "tr": "Orta", "uk": "Середина",
        "vi": "Trung", "zh-rCN": "中频", "zh-rTW": "中音",
    },
    "eq_range_high_mid": {
        "ar": "وسط مرتفع", "as": "উচ্চ মধ্য", "az": "Yüksək orta", "be": "Верхняя сярэдзіна",
        "bg": "Висок среден", "bn": "হাই-মিড", "bs": "Visoki srednji", "ca": "Mitjans alts",
        "cs": "Vyšší středy", "de": "Höhere Mitten", "el": "Υψηλά μέσα", "es": "Medios agudos",
        "et": "Ülemine kesk", "eu": "Ertain altuak", "fi": "Yläkäänet", "fil": "High mid",
        "fr": "Haut médium", "hi": "हाई-मिड", "hr": "Visoke srednje", "hu": "Felső közép",
        "id": "Mid tinggi", "it": "Medi alti", "ja": "高中音", "km": "កណ្តាលខ្ពស់",
        "ko": "고음 미드", "lt": "Aukštieji viduriai", "ml": "ഹൈ-മിഡ്", "ms": "Mid tinggi",
        "nb": "Høy-mellom", "nl": "Hoge midden", "pa": "ਹਾਈ-ਮਿਡ", "pl": "Wyższe średnie",
        "pt": "Médios agudos", "ro": "Mid-înalt", "ru": "Высокая середина", "sk": "Vyššie stredy",
        "sl": "Visoke sredine", "sr": "Високе средње", "sv": "Hög-mellan", "ta": "ஹை-மிட்",
        "te": "హై-మిడ్", "th": "กลางสูง", "tr": "Yüksek orta", "uk": "Верхня середина",
        "vi": "Trung cao", "zh-rCN": "中高频", "zh-rTW": "高中音",
    },
    "eq_range_treble": {
        "ar": "حادة", "as": "উচ্চ-স্বৰ", "az": "Yüksək tezliklər", "be": "Высокія частоты",
        "bg": "Високи честоти", "bn": "ট্রেবল", "bs": "Visoki tonovi", "ca": "Aguts",
        "cs": "Výšky", "de": "Höhen", "el": "Πρίμα", "es": "Agudos",
        "et": "Kõrged", "eu": "Altuak", "fi": "Diskantti", "fil": "Treble",
        "fr": "Aigus", "hi": "ट्रेबल", "hr": "Visoki tonovi", "hu": "Magasak",
        "id": "Treble", "it": "Acuti", "ja": "高音", "km": "សំឡេងខ្ពស់",
        "ko": "고음", "lt": "Aukštieji", "ml": "ട്രെബിൾ", "ms": "Treble",
        "nb": "Diskant", "nl": "Hoge tonen", "pa": "ਟ੍ਰੇਬਲ", "pl": "Soprany",
        "pt": "Agudos", "ro": "Înalte", "ru": "Верхние частоты", "sk": "Výšky",
        "sl": "Visoki toni", "sr": "Високи тонови", "sv": "Diskant", "ta": "ட்ரெபிள்",
        "te": "ట్రెబుల్", "th": "แหลม", "tr": "Tizler", "uk": "Високі частоти",
        "vi": "Bổng", "zh-rCN": "高频", "zh-rTW": "高音",
    },
}