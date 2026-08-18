# -*- coding: utf-8 -*-
"""Desktop-only translations: discovery screens, Listen Together, recognition.

These are the keys that don't have a matching Android string in every
language (or don't exist in Android at all). English is the fallback for
languages where a faithful translation isn't available yet.
"""

_LANGS = ['ar', 'as', 'az', 'be', 'bg', 'bn', 'bs', 'ca', 'cs', 'de', 'el', 'es', 'et', 'eu', 'fi', 'fil', 'fr', 'hi', 'hr', 'hu', 'id', 'it', 'ja', 'km', 'ko', 'lt', 'ml', 'ms', 'nb', 'nl', 'pa', 'pl', 'pt', 'ro', 'ru', 'sk', 'sl', 'sr', 'sv', 'ta', 'te', 'th', 'tr', 'uk', 'vi', 'zh-rCN', 'zh-rTW']


def _en(v):
    return {l: v for l in _LANGS}


EXTRA_TRANSLATIONS = {
    "items": {
        "ar": "العناصر", "as": "Items", "az": "Elementlər", "be": "Элементы",
        "bg": "Елементи", "bn": "আইটেম", "bs": "Stavke", "ca": "Elements",
        "cs": "Položky", "de": "Elemente", "el": "Στοιχεία", "es": "Elementos",
        "et": "Üksused", "eu": "Elementuak", "fi": "Kohteet", "fil": "Mga item",
        "fr": "Éléments", "hi": "आइटम", "hr": "Stavke", "hu": "Elemek",
        "id": "Item", "it": "Elementi", "ja": "項目", "km": "ធាតុ", "ko": "항목",
        "lt": "Elementai", "ml": "ഇനങ്ങൾ", "ms": "Item", "nb": "Elementer",
        "nl": "Items", "pa": "ਆਈਟਮਾਂ", "pl": "Elementy", "pt": "Itens",
        "ro": "Elemente", "ru": "Элементы", "sk": "Položky", "sl": "Elementi",
        "sr": "Ставке", "sv": "Objekt", "ta": "உருப்படிகள்", "te": "అంశాలు",
        "th": "รายการ", "tr": "Öğeler", "uk": "Елементи", "vi": "Mục",
        "zh-rCN": "项目", "zh-rTW": "項目",
    },
    "undo": {
        "ar": "تراجع", "as": "Undo", "az": "Geri al", "be": "Адмяніць",
        "bg": "Отмяна", "bn": "পূর্বাবস্থায়", "bs": "Poništi", "ca": "Desfés",
        "cs": "Zpět", "de": "Rückgängig", "el": "Αναίρεση", "es": "Deshacer",
        "et": "Võta tagasi", "eu": "Desegin", "fi": "Kumoa", "fil": "I-undo",
        "fr": "Annuler", "hi": "पूर्ववत करें", "hr": "Poništi", "hu": "Visszavonás",
        "id": "Urungkan", "it": "Annulla", "ja": "元に戻す", "km": "មិនធ្វើវិញ",
        "ko": "실행 취소", "lt": "Atšaukti", "ml": "പഴയപടിയാക്കുക", "ms": "Buat asal",
        "nb": "Angre", "nl": "Ongedaan maken", "pa": "ਵਾਪਸ", "pl": "Cofnij",
        "pt": "Desfazer", "ro": "Anulează", "ru": "Отменить", "sk": "Späť",
        "sl": "Razveljavi", "sr": "Опозови", "sv": "Ångra", "ta": "செயல்தவிர்",
        "te": "రద్దు చేయి", "th": "เลิกทำ", "tr": "Geri al", "uk": "Скасувати",
        "vi": "Hoàn tác", "zh-rCN": "撤销", "zh-rTW": "復原",
    },
    "redo": {
        "ar": "إعادة", "as": "Redo", "az": "Təkrarla", "be": "Паўтарыць",
        "bg": "Повтори", "bn": "পুনরায় করুন", "bs": "Ponovi", "ca": "Refés",
        "cs": "Znovu", "de": "Wiederholen", "el": "Επανάληψη", "es": "Rehacer",
        "et": "Tee uuesti", "eu": "Berregin", "fi": "Tee uudelleen", "fil": "I-redo",
        "fr": "Rétablir", "hi": "फिर से करें", "hr": "Ponovi", "hu": "Mégis",
        "id": "Ulangi", "it": "Ripeti", "ja": "やり直す", "km": "ធ្វើឡើងវិញ",
        "ko": "다시 실행", "lt": "Grąžinti", "ml": "വീണ്ടും ചെയ്യുക", "ms": "Buat semula",
        "nb": "Gjør om", "nl": "Opnieuw", "pa": "ਮੁੜ ਕਰੋ", "pl": "Ponów",
        "pt": "Refazer", "ro": "Refă", "ru": "Повторить", "sk": "Znova",
        "sl": "Uveljavi znova", "sr": "Понови", "sv": "Gör om", "ta": "மீண்டும் செய்",
        "te": "మళ్ళీ చేయి", "th": "ทำซ้ำ", "tr": "Yinele", "uk": "Повторити",
        "vi": "Làm lại", "zh-rCN": "重做", "zh-rTW": "重做",
    },
    "total_listening_time": {
        "ar": "إجمالي وقت الاستماع", "as": "Total Listening Time", "az": "Ümumi dinləmə vaxtı",
        "be": "Агульны час праслухоўвання", "bg": "Общо време на слушане", "bn": "মোট শ্রবণ সময়",
        "bs": "Ukupno vrijeme slušanja", "ca": "Temps total d'escolta", "cs": "Celkový čas poslechu",
        "de": "Gesamte Hörzeit", "el": "Συνολικός χρόνος ακρόασης", "es": "Tiempo total de escucha",
        "et": "Kuulamise koguaeg", "eu": "Entzuteko denbora osoa", "fi": "Kuunteluaika yhteensä",
        "fil": "Kabuuang oras ng pakikinig", "fr": "Temps d'écoute total", "hi": "कुल सुनने का समय",
        "hr": "Ukupno vrijeme slušanja", "hu": "Teljes hallgatási idő", "id": "Total waktu mendengarkan",
        "it": "Tempo di ascolto totale", "ja": "合計視聴時間", "km": "ពេលវេលាស្តាប់សរុប",
        "ko": "총 청취 시간", "lt": "Bendras klausymo laikas", "ml": "ആകെ കേൾക്കൽ സമയം",
        "ms": "Jumlah masa mendengar", "nb": "Total lyttetid", "nl": "Totale luistertijd",
        "pa": "ਕੁੱਲ ਸੁਣਨ ਦਾ ਸਮਾਂ", "pl": "Całkowity czas słuchania", "pt": "Tempo total de escuta",
        "ro": "Timp total de ascultare", "ru": "Общее время прослушивания", "sk": "Celkový čas počúvania",
        "sl": "Skupni čas poslušanja", "sr": "Укупно време слушања", "sv": "Total lyssningstid",
        "ta": "மொத்த கேட்கும் நேரம்", "te": "మొత్తం వినే సమయం", "th": "เวลาฟังทั้งหมด",
        "tr": "Toplam dinleme süresi", "uk": "Загальний час прослуховування", "vi": "Tổng thời gian nghe",
        "zh-rCN": "总收听时长", "zh-rTW": "總收聽時長",
    },
}
