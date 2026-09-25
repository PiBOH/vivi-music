# -*- coding: utf-8 -*-
"""`artists_loading_hint`, for every one of the 52 languages.

The artists tab derives its list from the songs when the account does not return
one, which takes seconds rather than a moment — a bare spinner for that long
reads as a hang, so the tab now says how long it may take. The key is
desktop-only (no Android resource behind it), so its English lives in the
generator's inline map and every other language has to be written out here; a
language left out would print the English sentence, which is exactly the
complaint the translation audits exist for.

The sentence is short and its meaning is fixed ("this may take up to ten
seconds"), so each translation says that and nothing more.
"""

EXTRA_TRANSLATIONS = {
    "artists_loading_hint": {
        "ar": "قد يستغرق التحميل حتى 10 ثوانٍ",
        "as": "লোড হ’বলৈ 10 ছেকেণ্ডলৈকে লাগিব পাৰে",
        "az": "Yükləmə 10 saniyəyə qədər çəkə bilər",
        "be": "Загрузка можа заняць да 10 секунд",
        "bg": "Зареждането може да отнеме до 10 секунди",
        "bn": "লোড হতে 10 সেকেন্ড পর্যন্ত লাগতে পারে",
        "bs": "Učitavanje može potrajati do 10 sekundi",
        "ca": "La càrrega pot trigar fins a 10 segons",
        "cs": "Načítání může trvat až 10 sekund",
        "de": "Das Laden kann bis zu 10 Sekunden dauern",
        "el": "Η φόρτωση μπορεί να διαρκέσει έως 10 δευτερόλεπτα",
        "es": "La carga puede tardar hasta 10 segundos",
        "et": "Laadimine võib kesta kuni 10 sekundit",
        "eu": "Kargak 10 segundo arte iraun dezake",
        "fa": "بارگذاری ممکن است تا 10 ثانیه طول بکشد",
        "fi": "Lataus voi kestää jopa 10 sekuntia",
        "fil": "Maaaring tumagal ng hanggang 10 segundo ang pag-load",
        "fr": "Le chargement peut prendre jusqu'à 10 secondes",
        "hi": "लोड होने में 10 सेकंड तक लग सकते हैं",
        "hr": "Učitavanje može potrajati do 10 sekundi",
        "hu": "A betöltés akár 10 másodpercig is tarthat",
        "id": "Pemuatan bisa memakan waktu hingga 10 detik",
        "in": "Pemuatan bisa memakan waktu hingga 10 detik",
        "it": "Il caricamento può richiedere fino a 10 secondi",
        "iw": "הטעינה עשויה להימשך עד 10 שניות",
        "ja": "読み込みに最大10秒かかることがあります",
        "km": "ការផ្ទុកអាចចំណាយពេលរហូតដល់ 10 វិនាទី",
        "ko": "불러오는 데 최대 10초가 걸릴 수 있습니다",
        "lt": "Įkėlimas gali užtrukti iki 10 sekundžių",
        "ml": "ലോഡ് ചെയ്യാൻ 10 സെക്കൻഡ് വരെ എടുത്തേക്കാം",
        "ms": "Pemuatan boleh mengambil masa sehingga 10 saat",
        "nb": "Lastingen kan ta opptil 10 sekunder",
        "nb-rNO": "Lastingen kan ta opptil 10 sekunder",
        "nl": "Het laden kan tot 10 seconden duren",
        "pa": "ਲੋਡ ਹੋਣ ਵਿੱਚ 10 ਸਕਿੰਟ ਤੱਕ ਲੱਗ ਸਕਦੇ ਹਨ",
        "pl": "Ładowanie może potrwać do 10 sekund",
        "pt": "O carregamento pode demorar até 10 segundos",
        "pt-rBR": "O carregamento pode levar até 10 segundos",
        "ro": "Încărcarea poate dura până la 10 secunde",
        "ru": "Загрузка может занять до 10 секунд",
        "sk": "Načítanie môže trvať až 10 sekúnd",
        "sl": "Nalaganje lahko traja do 10 sekund",
        "sr": "Учитавање може потрајати до 10 секунди",
        "sv": "Laddningen kan ta upp till 10 sekunder",
        "ta": "ஏற்றுவதற்கு 10 வினாடிகள் வரை ஆகலாம்",
        "te": "లోడ్ అవ్వడానికి 10 సెకన్ల వరకు పట్టవచ్చు",
        "th": "การโหลดอาจใช้เวลาถึง 10 วินาที",
        "tr": "Yükleme 10 saniye kadar sürebilir",
        "uk": "Завантаження може тривати до 10 секунд",
        "vi": "Quá trình tải có thể mất tới 10 giây",
        "zh-rCN": "加载最多可能需要 10 秒",
        "zh-rTW": "載入最多可能需要 10 秒",
    },
}
