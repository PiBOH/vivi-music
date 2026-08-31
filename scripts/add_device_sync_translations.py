#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
One-shot: inject the mobile "Devices" (device sync) string translations into
every language resource file that is missing them.

The base English strings live in app/src/main/res/values/strings.xml. Most
languages translate VIVI-specific strings in vivi_strings.xml; a few smaller
locales (fi, ml, pa, bn-rIN, en-rCA) only ship strings.xml, so we target
whichever file exists.

Run from the repo root:  python3 scripts/add_device_sync_translations.py
"""

import os

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "app", "src", "main", "res")

# key -> { values-XX dir suffix -> translation }
T = {
    "devices": {
        "ar": "الأجهزة", "as": "ডিভাইচ", "az": "Cihazlar", "b+sr+Latn": "Uređaji",
        "be": "Прылады", "bg": "Устройства", "bn": "ডিভাইস", "bn-rIN": "ডিভাইস",
        "bs": "Uređaji", "ca": "Dispositius", "cs": "Zařízení", "de": "Geräte",
        "el": "Συσκευές", "en-rCA": "Devices", "es": "Dispositivos", "es-rUS": "Dispositivos",
        "et": "Seadmed", "eu": "Gailuak", "fa": "دستگاه‌ها", "fi": "Laitteet",
        "fil": "Mga device", "fr": "Appareils", "hi": "डिवाइस", "hr": "Uređaji",
        "hu": "Eszközök", "in": "Perangkat", "it": "Dispositivi", "iw": "מכשירים",
        "ja": "デバイス", "km": "ឧបករណ៍", "ko": "기기", "lt": "Įrenginiai",
        "mfe": "Bann aparey", "ml": "ഉപകരണങ്ങൾ", "ms": "Peranti", "nb-rNO": "Enheter",
        "nl": "Apparaten", "pa": "ਡਿਵਾਈਸਾਂ", "pl": "Urządzenia", "pt": "Dispositivos",
        "pt-rBR": "Dispositivos", "ro": "Dispozitive", "ru": "Устройства", "sk": "Zariadenia",
        "sl": "Naprave", "sv": "Enheter", "ta": "சாதனங்கள்", "te": "పరికరాలు",
        "th": "อุปกรณ์", "tr": "Cihazlar", "uk": "Пристрої", "vi": "Thiết bị",
        "zh-rCN": "设备", "zh-rTW": "裝置",
    },
    "device_sync": {
        "ar": "مزامنة الأجهزة", "as": "ডিভাইচ চিংক", "az": "Cihaz sinxronizasiyası",
        "b+sr+Latn": "Sinhronizacija uređaja", "be": "Сінхранізацыя прылад",
        "bg": "Синхронизиране на устройства", "bn": "ডিভাইস সিঙ্ক", "bn-rIN": "ডিভাইস সিঙ্ক",
        "bs": "Sinhronizacija uređaja", "ca": "Sincronització de dispositius",
        "cs": "Synchronizace zařízení", "de": "Geräte-Synchronisierung",
        "el": "Συγχρονισμός συσκευών", "en-rCA": "Device sync",
        "es": "Sincronización de dispositivos", "es-rUS": "Sincronización de dispositivos",
        "et": "Seadmete sünkroonimine", "eu": "Gailuen sinkronizazioa",
        "fa": "همگام‌سازی دستگاه‌ها", "fi": "Laitteiden synkronointi",
        "fil": "Pag-sync ng device", "fr": "Synchronisation des appareils",
        "hi": "डिवाइस सिंक", "hr": "Sinkronizacija uređaja", "hu": "Eszközszinkronizálás",
        "in": "Sinkronisasi perangkat", "it": "Sincronizzazione dispositivi",
        "iw": "סנכרון מכשירים", "ja": "デバイス同期", "km": "ធ្វើសមកាលកម្មឧបករណ៍",
        "ko": "기기 동기화", "lt": "Įrenginių sinchronizavimas",
        "mfe": "Sinkronizasion bann aparey", "ml": "ഉപകരണ സമന്വയം",
        "ms": "Penyegerakan peranti", "nb-rNO": "Enhetssynkronisering",
        "nl": "Apparaatsynchronisatie", "pa": "ਡਿਵਾਈਸ ਸਿੰਕ", "pl": "Synchronizacja urządzeń",
        "pt": "Sincronização de dispositivos", "pt-rBR": "Sincronização de dispositivos",
        "ro": "Sincronizarea dispozitivelor", "ru": "Синхронизация устройств",
        "sk": "Synchronizácia zariadení", "sl": "Sinhronizacija naprav",
        "sv": "Enhetssynkronisering", "ta": "சாதன ஒத்திசைவு", "te": "పరికర సమకాలీకరణ",
        "th": "ซิงค์อุปกรณ์", "tr": "Cihaz senkronizasyonu", "uk": "Синхронізація пристроїв",
        "vi": "Đồng bộ thiết bị", "zh-rCN": "设备同步", "zh-rTW": "裝置同步",
    },
    "device_sync_desc": {
        "ar": "اقرن هذا الهاتف مع VIVI Music DE على حاسوبك لمزامنة الإعدادات وقائمة الانتظار والمكتبة.",
        "as": "ছেটিংছ, শাৰী আৰু লাইব্ৰেৰী চিংক কৰিবলৈ এই ফোনটো আপোনাৰ কম্পিউটাৰৰ VIVI Music DE ৰ লগত যোৰা লগাওক।",
        "az": "Parametrləri, növbəni və kitabxananı sinxronlaşdırmaq üçün bu telefonu kompüterinizdəki VIVI Music DE ilə qoşalaşdırın.",
        "b+sr+Latn": "Uparite ovaj telefon sa VIVI Music DE na računaru da biste sinhronizovali podešavanja, red čekanja i biblioteku.",
        "be": "Спаруйце гэты тэлефон з VIVI Music DE на кампутары, каб сінхранізаваць налады, чаргу і бібліятэку.",
        "bg": "Сдвоете този телефон с VIVI Music DE на компютъра си, за да синхронизирате настройките, опашката и библиотеката.",
        "bn": "সেটিংস, কিউ এবং লাইব্রেরি সিঙ্ক করতে এই ফোনটি আপনার কম্পিউটারের VIVI Music DE-এর সাথে জোড়া দিন।",
        "bn-rIN": "সেটিংস, কিউ এবং লাইব্রেরি সিঙ্ক করতে এই ফোনটি আপনার কম্পিউটারের VIVI Music DE-এর সাথে জোড়া দিন।",
        "bs": "Uparite ovaj telefon sa VIVI Music DE na računaru da biste sinhronizovali postavke, red čekanja i biblioteku.",
        "ca": "Vincula aquest telèfon amb VIVI Music DE a l'ordinador per sincronitzar ajustos, cua i biblioteca.",
        "cs": "Spárujte tento telefon s VIVI Music DE v počítači a synchronizujte nastavení, frontu a knihovnu.",
        "de": "Kopple dieses Telefon mit VIVI Music DE auf deinem Computer, um Einstellungen, Warteschlange und Bibliothek zu synchronisieren.",
        "el": "Σύζευξη αυτού του τηλεφώνου με το VIVI Music DE στον υπολογιστή σας για συγχρονισμό ρυθμίσεων, ουράς και βιβλιοθήκης.",
        "en-rCA": "Pair this phone with VIVI Music DE on your computer to sync settings, queue and library.",
        "es": "Vincula este teléfono con VIVI Music DE en tu ordenador para sincronizar ajustes, cola y biblioteca.",
        "es-rUS": "Vincula este teléfono con VIVI Music DE en tu computadora para sincronizar ajustes, cola y biblioteca.",
        "et": "Sidu see telefon arvutis oleva VIVI Music DE-ga, et sünkroonida seaded, järjekord ja teek.",
        "eu": "Parekatu telefono hau zure ordenagailuko VIVI Music DE-rekin ezarpenak, ilara eta liburutegia sinkronizatzeko.",
        "fa": "برای همگام‌سازی تنظیمات، صف و کتابخانه، این گوشی را با VIVI Music DE روی رایانه خود جفت کنید.",
        "fi": "Yhdistä tämä puhelin tietokoneesi VIVI Music DE:hen synkronoidaksesi asetukset, jonon ja kirjaston.",
        "fil": "Ipares ang teleponong ito sa VIVI Music DE sa iyong computer para i-sync ang mga setting, queue at library.",
        "fr": "Associez ce téléphone à VIVI Music DE sur votre ordinateur pour synchroniser les réglages, la file et la bibliothèque.",
        "hi": "सेटिंग्स, क्यू और लाइब्रेरी सिंक करने के लिए इस फ़ोन को अपने कंप्यूटर के VIVI Music DE के साथ जोड़ें।",
        "hr": "Uparite ovaj telefon s VIVI Music DE na računalu kako biste sinkronizirali postavke, red čekanja i biblioteku.",
        "hu": "Párosítsd ezt a telefont a számítógépeden futó VIVI Music DE-vel a beállítások, a sor és a könyvtár szinkronizálásához.",
        "in": "Pasangkan ponsel ini dengan VIVI Music DE di komputer Anda untuk menyinkronkan pengaturan, antrean, dan pustaka.",
        "it": "Associa questo telefono a VIVI Music DE sul computer per sincronizzare impostazioni, coda e libreria.",
        "iw": "חבר טלפון זה ל-VIVI Music DE במחשב כדי לסנכרן הגדרות, תור וספרייה.",
        "ja": "設定・キュー・ライブラリを同期するには、このスマホをパソコンの VIVI Music DE とペアリングしてください。",
        "km": "ភ្ជាប់ទូរស័ព្ទនេះជាមួយ VIVI Music DE នៅលើកុំព្យូទ័ររបស់អ្នក ដើម្បីធ្វើសមកាលកម្មការកំណត់ បញ្ជីចាក់ និងបណ្ណាល័យ។",
        "ko": "설정, 대기열 및 라이브러리를 동기화하려면 이 휴대전화를 컴퓨터의 VIVI Music DE와 페어링하세요.",
        "lt": "Susiekite šį telefoną su VIVI Music DE kompiuteryje, kad sinchronizuotumėte nustatymus, eilę ir biblioteką.",
        "mfe": "Pare sa telefonn-la avek VIVI Music DE lor ou laptop pou sinkroniz bann reglaz, file ek biblitek.",
        "ml": "ക്രമീകരണങ്ങളും ക്യൂവും ലൈബ്രറിയും സമന്വയിപ്പിക്കാൻ ഈ ഫോൺ കമ്പ്യൂട്ടറിലെ VIVI Music DE-യുമായി ജോടിയാക്കുക.",
        "ms": "Pasangkan telefon ini dengan VIVI Music DE pada komputer anda untuk menyegerakkan tetapan, baris gilir dan pustaka.",
        "nb-rNO": "Koble denne telefonen til VIVI Music DE på datamaskinen for å synkronisere innstillinger, kø og bibliotek.",
        "nl": "Koppel deze telefoon aan VIVI Music DE op je computer om instellingen, wachtrij en bibliotheek te synchroniseren.",
        "pa": "ਸੈਟਿੰਗਾਂ, ਕਤਾਰ ਅਤੇ ਲਾਇਬ੍ਰੇਰੀ ਨੂੰ ਸਿੰਕ ਕਰਨ ਲਈ ਇਸ ਫ਼ੋਨ ਨੂੰ ਆਪਣੇ ਕੰਪਿਊਟਰ ਦੇ VIVI Music DE ਨਾਲ ਜੋੜੋ।",
        "pl": "Sparuj ten telefon z VIVI Music DE na komputerze, aby zsynchronizować ustawienia, kolejkę i bibliotekę.",
        "pt": "Emparelhe este telemóvel com o VIVI Music DE no seu computador para sincronizar definições, fila e biblioteca.",
        "pt-rBR": "Emparelhe este celular com o VIVI Music DE no seu computador para sincronizar configurações, fila e biblioteca.",
        "ro": "Asociați acest telefon cu VIVI Music DE de pe computer pentru a sincroniza setările, coada și biblioteca.",
        "ru": "Сопрягите этот телефон с VIVI Music DE на компьютере, чтобы синхронизировать настройки, очередь и библиотеку.",
        "sk": "Spárujte tento telefón s VIVI Music DE v počítači a synchronizujte nastavenia, front a knižnicu.",
        "sl": "Seznanite ta telefon z VIVI Music DE v računalniku za sinhronizacijo nastavitev, čakalne vrste in knjižnice.",
        "sv": "Para ihop den här telefonen med VIVI Music DE på datorn för att synkronisera inställningar, kö och bibliotek.",
        "ta": "அமைப்புகள், வரிசை மற்றும் நூலகத்தை ஒத்திசைக்க இந்த ஃபோனை உங்கள் கணினியில் உள்ள VIVI Music DE உடன் இணைக்கவும்.",
        "te": "సెట్టింగ్లు, క్యూ మరియు లైబ్రరీని సమకాలీకరించడానికి ఈ ఫోన్ను మీ కంప్యూటర్లోని VIVI Music DEతో జత చేయండి.",
        "th": "จับคู่โทรศัพท์นี้กับ VIVI Music DE บนคอมพิวเตอร์ของคุณเพื่อซิงค์การตั้งค่า คิว และคลัง",
        "tr": "Ayarları, kuyruğu ve kitaplığı senkronize etmek için bu telefonu bilgisayarınızdaki VIVI Music DE ile eşleştirin.",
        "uk": "Зіставте цей телефон із VIVI Music DE на комп'ютері, щоб синхронізувати налаштування, чергу та бібліотеку.",
        "vi": "Ghép đôi điện thoại này với VIVI Music DE trên máy tính để đồng bộ cài đặt, hàng đợi và thư viện.",
        "zh-rCN": "将此手机与电脑上的 VIVI Music DE 配对，以同步设置、队列和媒体库。",
        "zh-rTW": "將此手機與電腦上的 VIVI Music DE 配對，以同步設定、佇列和媒體庫。",
    },
    "device_sync_server": {
        "ar": "خادم الترحيل", "as": "ৰিলে ছাৰ্ভাৰ", "az": "Relay server",
        "b+sr+Latn": "Relejni server", "be": "Рэлейны сервер", "bg": "Релеен сървър",
        "bn": "রিলে সার্ভার", "bn-rIN": "রিলে সার্ভার", "bs": "Relejni server",
        "ca": "Servidor de retransmissió", "cs": "Přenosový server", "de": "Relay-Server",
        "el": "Διακομιστής αναμετάδοσης", "en-rCA": "Relay server",
        "es": "Servidor de retransmisión", "es-rUS": "Servidor de retransmisión",
        "et": "Vaheserver", "eu": "Errelebo-zerbitzaria", "fa": "سرور رله",
        "fi": "Välityspalvelin", "fil": "Relay server", "fr": "Serveur relais",
        "hi": "रिले सर्वर", "hr": "Relejni poslužitelj", "hu": "Relay szerver",
        "in": "Server relai", "it": "Server relay", "iw": "שרת ממסר",
        "ja": "リレーサーバー", "km": "ម៉ាស៊ីនមេបញ្ជូនបន្ត", "ko": "릴레이 서버",
        "lt": "Relės serveris", "mfe": "Servér relé", "ml": "റിലേ സെർവർ",
        "ms": "Pelayan geganti", "nb-rNO": "Reléserver", "nl": "Relayserver",
        "pa": "ਰੀਲੇ ਸਰਵਰ", "pl": "Serwer przekaźnikowy", "pt": "Servidor de retransmissão",
        "pt-rBR": "Servidor de retransmissão", "ro": "Server releu", "ru": "Ретрансляционный сервер",
        "sk": "Prenosový server", "sl": "Preusmerjevalni strežnik", "sv": "Reläserver",
        "ta": "ரிலே சேவையகம்", "te": "రిలే సర్వర్", "th": "เซิร์ฟเวอร์รีเลย์",
        "tr": "Aktarma sunucusu", "uk": "Ретрансляційний сервер", "vi": "Máy chủ chuyển tiếp",
        "zh-rCN": "中继服务器", "zh-rTW": "中繼伺服器",
    },
    "device_sync_discover": {
        "ar": "البحث عن سطح المكتب", "as": "ডেস্কটপ বিচাৰক", "az": "Masaüstünü tap",
        "b+sr+Latn": "Pronađi računar", "be": "Знайсці камп'ютар", "bg": "Откриване на компютъра",
        "bn": "ডেস্কটপ খুঁজুন", "bn-rIN": "ডেস্কটপ খুঁজুন", "bs": "Pronađi računar",
        "ca": "Cerca l'escriptori", "cs": "Najít počítač", "de": "Desktop finden",
        "el": "Εύρεση υπολογιστή", "en-rCA": "Find desktop", "es": "Buscar escritorio",
        "es-rUS": "Buscar escritorio", "et": "Otsi arvutit", "eu": "Aurkitu mahaigaina",
        "fa": "یافتن رایانه", "fi": "Etsi tietokone", "fil": "Hanapin ang desktop",
        "fr": "Trouver le bureau", "hi": "डेस्कटॉप खोजें", "hr": "Pronađi računalo",
        "hu": "Asztali gép keresése", "in": "Temukan desktop", "it": "Trova desktop",
        "iw": "מצא מחשב", "ja": "デスクトップを探す", "km": "ស្វែងរកកុំព្យូទ័រ",
        "ko": "데스크톱 찾기", "lt": "Rasti kompiuterį", "mfe": "Trouv desktop",
        "ml": "ഡെസ്ക്ടോപ്പ് കണ്ടെത്തുക", "ms": "Cari desktop", "nb-rNO": "Finn datamaskin",
        "nl": "Computer zoeken", "pa": "ਡੈਸਕਟਾਪ ਲੱਭੋ", "pl": "Znajdź komputer",
        "pt": "Encontrar computador", "pt-rBR": "Encontrar computador", "ro": "Găsește computerul",
        "ru": "Найти компьютер", "sk": "Nájsť počítač", "sl": "Poišči računalnik",
        "sv": "Hitta dator", "ta": "டெஸ்க்டாப்பைக் கண்டறி", "te": "డెస్క్టాప్ను కనుగొను",
        "th": "ค้นหาเดสก์ท็อป", "tr": "Masaüstünü bul", "uk": "Знайти комп'ютер",
        "vi": "Tìm máy tính", "zh-rCN": "查找桌面端", "zh-rTW": "尋找桌面端",
    },
    "device_sync_scan_qr": {
        "ar": "مسح رمز QR", "as": "QR ক'ড স্কেন কৰক", "az": "QR kodu skan et",
        "b+sr+Latn": "Skeniraj QR kod", "be": "Сканаваць QR-код", "bg": "Сканиране на QR код",
        "bn": "QR কোড স্ক্যান করুন", "bn-rIN": "QR কোড স্ক্যান করুন", "bs": "Skeniraj QR kod",
        "ca": "Escaneja el codi QR", "cs": "Naskenovat QR kód", "de": "QR-Code scannen",
        "el": "Σάρωση κωδικού QR", "en-rCA": "Scan QR code", "es": "Escanear código QR",
        "es-rUS": "Escanear código QR", "et": "Skanni QR-koodi", "eu": "Eskaneatu QR kodea",
        "fa": "اسکن کد QR", "fi": "Skannaa QR-koodi", "fil": "I-scan ang QR code",
        "fr": "Scanner le code QR", "hi": "QR कोड स्कैन करें", "hr": "Skeniraj QR kod",
        "hu": "QR-kód beolvasása", "in": "Pindai kode QR", "it": "Scansiona codice QR",
        "iw": "סרוק קוד QR", "ja": "QRコードをスキャン", "km": "ស្កេនកូដ QR",
        "ko": "QR 코드 스캔", "lt": "Nuskaityti QR kodą", "mfe": "Sken kod QR",
        "ml": "QR കോഡ് സ്കാൻ ചെയ്യുക", "ms": "Imbas kod QR", "nb-rNO": "Skann QR-kode",
        "nl": "QR-code scannen", "pa": "QR ਕੋਡ ਸਕੈਨ ਕਰੋ", "pl": "Zeskanuj kod QR",
        "pt": "Ler código QR", "pt-rBR": "Ler código QR", "ro": "Scanează codul QR",
        "ru": "Сканировать QR-код", "sk": "Naskenovať QR kód", "sl": "Skeniraj kodo QR",
        "sv": "Skanna QR-kod", "ta": "QR குறியீட்டை ஸ்கேன் செய்", "te": "QR కోడ్ను స్కాన్ చేయండి",
        "th": "สแกนคิวอาร์โค้ด", "tr": "QR kodunu tara", "uk": "Сканувати QR-код",
        "vi": "Quét mã QR", "zh-rCN": "扫描二维码", "zh-rTW": "掃描 QR 碼",
    },
    "device_sync_generate_code": {
        "ar": "إنشاء رمز", "as": "ক'ড সৃষ্টি কৰক", "az": "Kod yarat",
        "b+sr+Latn": "Generiši kod", "be": "Стварыць код", "bg": "Генериране на код",
        "bn": "কোড তৈরি করুন", "bn-rIN": "কোড তৈরি করুন", "bs": "Generiši kod",
        "ca": "Genera el codi", "cs": "Vygenerovat kód", "de": "Code generieren",
        "el": "Δημιουργία κωδικού", "en-rCA": "Generate code", "es": "Generar código",
        "es-rUS": "Generar código", "et": "Genereeri kood", "eu": "Sortu kodea",
        "fa": "ایجاد کد", "fi": "Luo koodi", "fil": "Bumuo ng code",
        "fr": "Générer le code", "hi": "कोड जनरेट करें", "hr": "Generiraj kod",
        "hu": "Kód generálása", "in": "Buat kode", "it": "Genera codice",
        "iw": "צור קוד", "ja": "コードを生成", "km": "បង្កើតកូដ",
        "ko": "코드 생성", "lt": "Generuoti kodą", "mfe": "Zener kod",
        "ml": "കോഡ് സൃഷ്ടിക്കുക", "ms": "Jana kod", "nb-rNO": "Generer kode",
        "nl": "Code genereren", "pa": "ਕੋਡ ਬਣਾਓ", "pl": "Wygeneruj kod",
        "pt": "Gerar código", "pt-rBR": "Gerar código", "ro": "Generează codul",
        "ru": "Создать код", "sk": "Vygenerovať kód", "sl": "Ustvari kodo",
        "sv": "Generera kod", "ta": "குறியீட்டை உருவாக்கு", "te": "కోడ్ను రూపొందించు",
        "th": "สร้างรหัส", "tr": "Kod oluştur", "uk": "Створити код",
        "vi": "Tạo mã", "zh-rCN": "生成代码", "zh-rTW": "產生代碼",
    },
    "device_sync_join_code": {
        "ar": "رمز من 6 أرقام من حاسوبك", "as": "আপোনাৰ কম্পিউটাৰৰ পৰা ৬ অংকৰ ক'ড",
        "az": "Kompüterinizdən 6 rəqəmli kod", "b+sr+Latn": "6-cifreni kod sa računara",
        "be": "6-значны код з камп'ютара", "bg": "6-цифрен код от компютъра ви",
        "bn": "আপনার কম্পিউটার থেকে ৬-সংখ্যার কোড", "bn-rIN": "আপনার কম্পিউটার থেকে ৬-সংখ্যার কোড",
        "bs": "6-cifreni kod sa računara", "ca": "Codi de 6 dígits del teu ordinador",
        "cs": "Šestimístný kód z počítače", "de": "6-stelliger Code von deinem Computer",
        "el": "6ψήφιος κωδικός από τον υπολογιστή σας", "en-rCA": "6-digit code from your computer",
        "es": "Código de 6 dígitos de tu ordenador", "es-rUS": "Código de 6 dígitos de tu computadora",
        "et": "6-kohaline kood arvutist", "eu": "Zure ordenagailuko 6 digituko kodea",
        "fa": "کد ۶ رقمی از رایانه شما", "fi": "6-numeroinen koodi tietokoneeltasi",
        "fil": "6-digit na code mula sa iyong computer", "fr": "Code à 6 chiffres de votre ordinateur",
        "hi": "आपके कंप्यूटर से 6 अंकों का कोड", "hr": "6-znamenkasti kod s računala",
        "hu": "6 számjegyű kód a számítógépedről", "in": "Kode 6 digit dari komputer Anda",
        "it": "Codice a 6 cifre dal computer", "iw": "קוד בן 6 ספרות מהמחשב שלך",
        "ja": "パソコンに表示される6桁のコード", "km": "កូដ 6 ខ្ទង់ពីកុំព្យូទ័ររបស់អ្នក",
        "ko": "컴퓨터의 6자리 코드", "lt": "6 skaitmenų kodas iš kompiuterio",
        "mfe": "Kod 6 chif depi ou laptop", "ml": "നിങ്ങളുടെ കമ്പ്യൂട്ടറിൽ നിന്നുള്ള 6 അക്ക കോഡ്",
        "ms": "Kod 6 digit daripada komputer anda", "nb-rNO": "6-sifret kode fra datamaskinen",
        "nl": "6-cijferige code van je computer", "pa": "ਆਪਣੇ ਕੰਪਿਊਟਰ ਤੋਂ 6-ਅੰਕਾਂ ਦਾ ਕੋਡ",
        "pl": "6-cyfrowy kod z komputera", "pt": "Código de 6 dígitos do seu computador",
        "pt-rBR": "Código de 6 dígitos do seu computador", "ro": "Cod de 6 cifre de pe computer",
        "ru": "6-значный код с компьютера", "sk": "6-miestny kód z počítača",
        "sl": "6-mestna koda iz računalnika", "sv": "6-siffrig kod från datorn",
        "ta": "உங்கள் கணினியிலிருந்து 6 இலக்கக் குறியீடு", "te": "మీ కంప్యూటర్ నుండి 6 అంకెల కోడ్",
        "th": "รหัส 6 หลักจากคอมพิวเตอร์ของคุณ", "tr": "Bilgisayarınızdan 6 haneli kod",
        "uk": "6-значний код із комп'ютера", "vi": "Mã 6 chữ số từ máy tính của bạn",
        "zh-rCN": "来自电脑的 6 位代码", "zh-rTW": "來自電腦的 6 位代碼",
    },
    "device_sync_pair": {
        "ar": "إقران", "as": "যোৰা লগাওক", "az": "Qoşalaşdır",
        "b+sr+Latn": "Upari", "be": "Спарыць", "bg": "Сдвояване", "bn": "জোড়া দিন",
        "bn-rIN": "জোড়া দিন", "bs": "Upari", "ca": "Vincula", "cs": "Spárovat",
        "de": "Koppeln", "el": "Σύζευξη", "en-rCA": "Pair", "es": "Vincular",
        "es-rUS": "Vincular", "et": "Sida", "eu": "Parekatu", "fa": "جفت شدن",
        "fi": "Yhdistä", "fil": "Ipares", "fr": "Associer", "hi": "जोड़ें",
        "hr": "Uparite", "hu": "Párosítás", "in": "Pasangkan", "it": "Associa",
        "iw": "חבר", "ja": "ペアリング", "km": "ភ្ជាប់", "ko": "페어링",
        "lt": "Susieti", "mfe": "Pare", "ml": "ജോടിയാക്കുക", "ms": "Pasangkan",
        "nb-rNO": "Koble sammen", "nl": "Koppelen", "pa": "ਜੋੜੋ", "pl": "Sparuj",
        "pt": "Emparelhar", "pt-rBR": "Emparelhar", "ro": "Asociază", "ru": "Сопрячь",
        "sk": "Spárovať", "sl": "Seznami", "sv": "Para ihop", "ta": "இணை",
        "te": "జత చేయి", "th": "จับคู่", "tr": "Eşleştir", "uk": "Зіставити",
        "vi": "Ghép đôi", "zh-rCN": "配对", "zh-rTW": "配對",
    },
    "device_sync_unpair": {
        "ar": "إلغاء الإقران", "as": "যোৰা ভাঙক", "az": "Qoşalaşdırmanı ləğv et",
        "b+sr+Latn": "Prekini uparivanje", "be": "Раз'яднаць", "bg": "Прекратяване на сдвояването",
        "bn": "জোড়া ভাঙুন", "bn-rIN": "জোড়া ভাঙুন", "bs": "Prekini uparivanje",
        "ca": "Desvincula", "cs": "Zrušit párování", "de": "Kopplung aufheben",
        "el": "Κατάργηση σύζευξης", "en-rCA": "Unpair", "es": "Desvincular",
        "es-rUS": "Desvincular", "et": "Lõpeta sidumine", "eu": "Desparekatu",
        "fa": "لغو جفت شدن", "fi": "Katkaise yhteys", "fil": "I-unpair",
        "fr": "Dissocier", "hi": "जोड़ी हटाएँ", "hr": "Prekini uparivanje",
        "hu": "Párosítás megszüntetése", "in": "Putuskan pasangan", "it": "Dissocia",
        "iw": "בטל חיבור", "ja": "ペアリング解除", "km": "ផ្តាច់ការភ្ជាប់",
        "ko": "페어링 해제", "lt": "Atsieti", "mfe": "Desparé", "ml": "ജോടി വേർപെടുത്തുക",
        "ms": "Nyahpasang", "nb-rNO": "Koble fra", "nl": "Ontkoppelen", "pa": "ਜੋੜ ਹਟਾਓ",
        "pl": "Rozparuj", "pt": "Desemparelhar", "pt-rBR": "Desemparelhar",
        "ro": "Anulează asocierea", "ru": "Разорвать пару", "sk": "Zrušiť párovanie",
        "sl": "Prekini seznanitev", "sv": "Koppla från", "ta": "இணைப்பை நீக்கு",
        "te": "జతను తొలగించు", "th": "เลิกจับคู่", "tr": "Eşleştirmeyi kaldır",
        "uk": "Розірвати пару", "vi": "Hủy ghép đôi", "zh-rCN": "取消配对", "zh-rTW": "取消配對",
    },
    "device_sync_paired": {
        "ar": "مقترن", "as": "যোৰা লগাইছে", "az": "Qoşalaşdırıldı",
        "b+sr+Latn": "Upareno", "be": "Спаравана", "bg": "Сдвоено", "bn": "জোড়া হয়েছে",
        "bn-rIN": "জোড়া হয়েছে", "bs": "Upareno", "ca": "Vinculat", "cs": "Spárováno",
        "de": "Gekoppelt", "el": "Σε σύζευξη", "en-rCA": "Paired", "es": "Vinculado",
        "es-rUS": "Vinculado", "et": "Seotud", "eu": "Parekatuta", "fa": "جفت شده",
        "fi": "Yhdistetty", "fil": "Nakapares", "fr": "Associé", "hi": "जोड़ा गया",
        "hr": "Upareno", "hu": "Párosítva", "in": "Terpasang", "it": "Associato",
        "iw": "מחובר", "ja": "ペアリング済み", "km": "បានភ្ជាប់", "ko": "페어링됨",
        "lt": "Susieta", "mfe": "Pared", "ml": "ജോടിയാക്കി", "ms": "Dipasangkan",
        "nb-rNO": "Koblet sammen", "nl": "Gekoppeld", "pa": "ਜੋੜਿਆ ਗਿਆ", "pl": "Sparowano",
        "pt": "Emparelhado", "pt-rBR": "Emparelhado", "ro": "Asociat", "ru": "Сопряжено",
        "sk": "Spárované", "sl": "Seznanjeno", "sv": "Ihopparad", "ta": "இணைக்கப்பட்டது",
        "te": "జత చేయబడింది", "th": "จับคู่แล้ว", "tr": "Eşleştirildi", "uk": "Зіставлено",
        "vi": "Đã ghép đôi", "zh-rCN": "已配对", "zh-rTW": "已配對",
    },
    "device_sync_not_paired": {
        "ar": "غير مقترن", "as": "যোৰা লগোৱা নাই", "az": "Qoşalaşdırılmayıb",
        "b+sr+Latn": "Nije upareno", "be": "Не спаравана", "bg": "Не е сдвоено",
        "bn": "জোড়া হয়নি", "bn-rIN": "জোড়া হয়নি", "bs": "Nije upareno",
        "ca": "No vinculat", "cs": "Nespárováno", "de": "Nicht gekoppelt",
        "el": "Χωρίς σύζευξη", "en-rCA": "Not paired", "es": "No vinculado",
        "es-rUS": "No vinculado", "et": "Pole seotud", "eu": "Ez parekatuta",
        "fa": "جفت نشده", "fi": "Ei yhdistetty", "fil": "Hindi nakapares",
        "fr": "Non associé", "hi": "जोड़ा नहीं गया", "hr": "Nije upareno",
        "hu": "Nincs párosítva", "in": "Tidak terpasang", "it": "Non associato",
        "iw": "לא מחובר", "ja": "未ペアリング", "km": "មិនបានភ្ជាប់", "ko": "페어링 안 됨",
        "lt": "Nesusieta", "mfe": "Pa ankor pared", "ml": "ജോടിയാക്കിയിട്ടില്ല",
        "ms": "Tidak dipasangkan", "nb-rNO": "Ikke koblet sammen", "nl": "Niet gekoppeld",
        "pa": "ਜੋੜਿਆ ਨਹੀਂ ਗਿਆ", "pl": "Nie sparowano", "pt": "Não emparelhado",
        "pt-rBR": "Não emparelhado", "ro": "Neasociat", "ru": "Не сопряжено",
        "sk": "Nespárované", "sl": "Ni seznanjeno", "sv": "Inte ihopparad",
        "ta": "இணைக்கப்படவில்லை", "te": "జత చేయబడలేదు", "th": "ยังไม่จับคู่",
        "tr": "Eşleştirilmedi", "uk": "Не зіставлено", "vi": "Chưa ghép đôi",
        "zh-rCN": "未配对", "zh-rTW": "未配對",
    },
    "device_sync_paired_device": {
        "ar": "الجهاز المقترن", "as": "যোৰা লগোৱা ডিভাইচ", "az": "Qoşalaşdırılmış cihaz",
        "b+sr+Latn": "Upareni uređaj", "be": "Спараваная прылада", "bg": "Сдвоено устройство",
        "bn": "জোড়া ডিভাইস", "bn-rIN": "জোড়া ডিভাইস", "bs": "Upareni uređaj",
        "ca": "Dispositiu vinculat", "cs": "Spárované zařízení", "de": "Gekoppeltes Gerät",
        "el": "Συσκευή σε σύζευξη", "en-rCA": "Paired device", "es": "Dispositivo vinculado",
        "es-rUS": "Dispositivo vinculado", "et": "Seotud seade", "eu": "Gailu parekatua",
        "fa": "دستگاه جفت شده", "fi": "Yhdistetty laite", "fil": "Nakapares na device",
        "fr": "Appareil associé", "hi": "जोड़ा गया डिवाइस", "hr": "Upareni uređaj",
        "hu": "Párosított eszköz", "in": "Perangkat terpasang", "it": "Dispositivo associato",
        "iw": "מכשיר מחובר", "ja": "ペアリング済みデバイス", "km": "ឧបករណ៍ដែលបានភ្ជាប់",
        "ko": "페어링된 기기", "lt": "Susietas įrenginys", "mfe": "Aparey pared",
        "ml": "ജോടിയാക്കിയ ഉപകരണം", "ms": "Peranti dipasangkan", "nb-rNO": "Sammenkoblet enhet",
        "nl": "Gekoppeld apparaat", "pa": "ਜੋੜਿਆ ਡਿਵਾਈਸ", "pl": "Sparowane urządzenie",
        "pt": "Dispositivo emparelhado", "pt-rBR": "Dispositivo emparelhado", "ro": "Dispozitiv asociat",
        "ru": "Сопряжённое устройство", "sk": "Spárované zariadenie", "sl": "Seznanjena naprava",
        "sv": "Ihopparad enhet", "ta": "இணைக்கப்பட்ட சாதனம்", "te": "జత చేసిన పరికరం",
        "th": "อุปกรณ์ที่จับคู่แล้ว", "tr": "Eşleştirilen cihaz", "uk": "Зіставлений пристрій",
        "vi": "Thiết bị đã ghép đôi", "zh-rCN": "已配对设备", "zh-rTW": "已配對裝置",
    },
    "sync_vivi_volume": {
        "ar": "مزامنة مستوى صوت VIVI", "as": "VIVI ভলিউম চিংক কৰক",
        "az": "VIVI səs səviyyəsini sinxronlaşdır", "b+sr+Latn": "Sinhronizuj VIVI jačinu zvuka",
        "be": "Сінхранізаваць гучнасць VIVI", "bg": "Синхронизиране на силата на звука на VIVI",
        "bn": "VIVI ভলিউম সিঙ্ক করুন", "bn-rIN": "VIVI ভলিউম সিঙ্ক করুন",
        "bs": "Sinhronizuj VIVI jačinu zvuka", "ca": "Sincronitza el volum de VIVI",
        "cs": "Synchronizovat hlasitost VIVI", "de": "VIVI-Lautstärke synchronisieren",
        "el": "Συγχρονισμός έντασης VIVI", "en-rCA": "Sync VIVI volume",
        "es": "Sincronizar volumen de VIVI", "es-rUS": "Sincronizar volumen de VIVI",
        "et": "VIVI helitugevuse sünkroonimine", "eu": "Sinkronizatu VIVI bolumena",
        "fa": "همگام‌سازی صدای VIVI", "fi": "Synkronoi VIVI-äänenvoimakkuus",
        "fil": "I-sync ang volume ng VIVI", "fr": "Synchroniser le volume de VIVI",
        "hi": "VIVI वॉल्यूम सिंक करें", "hr": "Sinkroniziraj VIVI glasnoću",
        "hu": "VIVI hangerő szinkronizálása", "in": "Sinkronkan volume VIVI",
        "it": "Sincronizza volume VIVI", "iw": "סנכרון עוצמת הקול של VIVI",
        "ja": "VIVI 音量を同期", "km": "ធ្វើសមកាលកម្មសំឡេង VIVI", "ko": "VIVI 볼륨 동기화",
        "lt": "Sinchronizuoti VIVI garsumą", "mfe": "Sinkroniz volim VIVI",
        "ml": "VIVI വോളിയം സമന്വയിപ്പിക്കുക", "ms": "Segerakkan kelantangan VIVI",
        "nb-rNO": "Synkroniser VIVI-volum", "nl": "VIVI-volume synchroniseren",
        "pa": "VIVI ਵਾਲੀਅਮ ਸਿੰਕ ਕਰੋ", "pl": "Synchronizuj głośność VIVI",
        "pt": "Sincronizar volume do VIVI", "pt-rBR": "Sincronizar volume do VIVI",
        "ro": "Sincronizează volumul VIVI", "ru": "Синхронизировать громкость VIVI",
        "sk": "Synchronizovať hlasitosť VIVI", "sl": "Sinhroniziraj glasnost VIVI",
        "sv": "Synkronisera VIVI-volym", "ta": "VIVI ஒலியளவை ஒத்திசை",
        "te": "VIVI వాల్యూమ్ను సమకాలీకరించు", "th": "ซิงค์ระดับเสียง VIVI",
        "tr": "VIVI ses düzeyini senkronize et", "uk": "Синхронізувати гучність VIVI",
        "vi": "Đồng bộ âm lượng VIVI", "zh-rCN": "同步 VIVI 音量", "zh-rTW": "同步 VIVI 音量",
    },
    "sync_vivi_volume_desc": {
        "ar": "عكس شريط مستوى الصوت داخل التطبيق بين هذا الهاتف وVIVI Music DE. عطّله لإبقاء مستوى صوت المشغل مستقلاً لكل جهاز.",
        "as": "এই ফোন আৰু VIVI Music DE ৰ মাজত এপৰ ভিতৰৰ ভলিউম স্লাইডাৰখন প্ৰতিফলিত কৰক। প্ৰতিটো ডিভাইচৰ প্লেয়াৰ ভলিউম স্বাধীন ৰাখিবলৈ ইয়াক নিষ্ক্ৰিয় কৰক।",
        "az": "Tətbiqdaxili səs sürgüsünü bu telefon və VIVI Music DE arasında əks etdir. Hər cihazın pleyer səsini müstəqil saxlamaq üçün onu söndür.",
        "b+sr+Latn": "Ogledalno sinhronizuj klizač jačine zvuka u aplikaciji između ovog telefona i VIVI Music DE. Isključi ga da jačina zvuka svakog uređaja ostane nezavisna.",
        "be": "Адлюстроўвайце паўзунок гучнасці ў дадатку паміж гэтым тэлефонам і VIVI Music DE. Адключыце, каб гучнасць плэера кожнай прылады заставалася незалежнай.",
        "bg": "Огледално синхронизирайте плъзгача за сила на звука в приложението между този телефон и VIVI Music DE. Изключете го, за да запазите независима силата на звука на всяко устройство.",
        "bn": "এই ফোন এবং VIVI Music DE-এর মধ্যে অ্যাপের ভলিউম স্লাইডারটি মিরর করুন। প্রতিটি ডিভাইসের প্লেয়ার ভলিউম স্বাধীন রাখতে এটি বন্ধ করুন।",
        "bn-rIN": "এই ফোন এবং VIVI Music DE-এর মধ্যে অ্যাপের ভলিউম স্লাইডারটি মিরর করুন। প্রতিটি ডিভাইসের প্লেয়ার ভলিউম স্বাধীন রাখতে এটি বন্ধ করুন।",
        "bs": "Ogledalno sinhronizuj klizač jačine zvuka u aplikaciji između ovog telefona i VIVI Music DE. Isključi ga da jačina zvuka svakog uređaja ostane nezavisna.",
        "ca": "Reflecteix el control lliscant de volum de l'app entre aquest telèfon i VIVI Music DE. Desactiva'l per mantenir el volum del reproductor independent a cada dispositiu.",
        "cs": "Zrcadlete posuvník hlasitosti v aplikaci mezi tímto telefonem a VIVI Music DE. Vypněte jej, aby hlasitost přehrávače každého zařízení zůstala nezávislá.",
        "de": "Spiegle den Lautstärkeregler der App zwischen diesem Telefon und VIVI Music DE. Deaktiviere ihn, damit die Player-Lautstärke jedes Geräts unabhängig bleibt.",
        "el": "Αντικατοπτρίστε το ρυθμιστικό έντασης της εφαρμογής μεταξύ αυτού του τηλεφώνου και του VIVI Music DE. Απενεργοποιήστε το για να διατηρήσετε ανεξάρτητη την ένταση κάθε συσκευής.",
        "en-rCA": "Mirror the in-app volume slider between this phone and VIVI Music DE. Disable it to keep each device's player volume independent.",
        "es": "Refleja el control deslizante de volumen de la app entre este teléfono y VIVI Music DE. Desactívalo para mantener el volumen del reproductor independiente en cada dispositivo.",
        "es-rUS": "Refleja el control deslizante de volumen de la app entre este teléfono y VIVI Music DE. Desactívalo para mantener el volumen del reproductor independiente en cada dispositivo.",
        "et": "Peegelda rakendusesisest helitugevuse liugurit selle telefoni ja VIVI Music DE vahel. Keela see, et iga seadme mängija helitugevus jääks sõltumatuks.",
        "eu": "Ispilatu aplikazioko bolumen-kontrola telefono honen eta VIVI Music DE-ren artean. Desgaitu gailu bakoitzaren erreproduzitzaile-bolumena independente mantentzeko.",
        "fa": "نوار لغزنده صدای داخل برنامه را بین این گوشی و VIVI Music DE منعکس کنید. برای مستقل نگه داشتن صدای پخش‌کننده هر دستگاه، آن را غیرفعال کنید.",
        "fi": "Peilaa sovelluksen äänenvoimakkuuden liukusäädin tämän puhelimen ja VIVI Music DE:n välillä. Poista se käytöstä pitääksesi kunkin laitteen soittimen äänenvoimakkuuden erillisenä.",
        "fil": "I-mirror ang volume slider ng app sa pagitan ng teleponong ito at VIVI Music DE. I-disable ito para manatiling hiwalay ang volume ng player ng bawat device.",
        "fr": "Reflétez le curseur de volume de l'app entre ce téléphone et VIVI Music DE. Désactivez-le pour garder le volume du lecteur indépendant sur chaque appareil.",
        "hi": "इस फ़ोन और VIVI Music DE के बीच ऐप के वॉल्यूम स्लाइडर को प्रतिबिंबित करें। प्रत्येक डिवाइस का प्लेयर वॉल्यूम स्वतंत्र रखने के लिए इसे बंद करें।",
        "hr": "Ogledalno sinkroniziraj klizač glasnoće u aplikaciji između ovog telefona i VIVI Music DE. Isključi ga da glasnoća svakog uređaja ostane neovisna.",
        "hu": "Tükrözd az alkalmazáson belüli hangerő-csúszkát e telefon és a VIVI Music DE között. Kapcsold ki, ha azt szeretnéd, hogy az egyes eszközök lejátszó-hangereje független maradjon.",
        "in": "Cerminkan penggeser volume dalam aplikasi antara ponsel ini dan VIVI Music DE. Nonaktifkan untuk menjaga volume pemutar setiap perangkat tetap independen.",
        "it": "Rispecchia il cursore del volume dell'app tra questo telefono e VIVI Music DE. Disattivalo per mantenere indipendente il volume del lettore di ogni dispositivo.",
        "iw": "שיקוף את מחוון עוצמת הקול של האפליקציה בין טלפון זה ל-VIVI Music DE. השבת אותו כדי לשמור על עוצמת הקול של הנגן בכל מכשיר בנפרד.",
        "ja": "このスマホと VIVI Music DE の間でアプリ内の音量スライダーをミラーリングします。各デバイスのプレイヤー音量を独立させるにはオフにしてください。",
        "km": "ធ្វើឲ្យរបារសំឡេងក្នុងកម្មវិធីដូចគ្នារវាងទូរស័ព្ទនេះ និង VIVI Music DE។ បិទវាដើម្បីរក្សាសំឡេងរបស់កម្មវិធីចាក់នៅលើឧបករណ៍នីមួយៗឱ្យឯករាជ្យ។",
        "ko": "이 휴대전화와 VIVI Music DE 간에 앱 내 볼륨 슬라이더를 미러링합니다. 각 기기의 플레이어 볼륨을 독립적으로 유지하려면 끄세요.",
        "lt": "Veidrodinkite programos garsumo slankiklį tarp šio telefono ir VIVI Music DE. Išjunkite, kad kiekvieno įrenginio grotuvo garsumas liktų nepriklausomas.",
        "mfe": "Reflet slider volim dan aplikasion ant sa telefonn ek VIVI Music DE. Deaktiv li pou gard volim player sak aparey indepandan.",
        "ml": "ഈ ഫോണും VIVI Music DE-യും തമ്മിൽ ആപ്പിനുള്ളിലെ വോളിയം സ്ലൈഡർ പ്രതിഫലിപ്പിക്കുക. ഓരോ ഉപകരണത്തിന്റെയും പ്ലെയർ വോളിയം സ്വതന്ത്രമായി നിലനിർത്താൻ ഇത് അപ്രാപ്തമാക്കുക.",
        "ms": "Cerminkan peluncur kelantangan dalam aplikasi antara telefon ini dan VIVI Music DE. Lumpuhkannya untuk memastikan kelantangan pemain setiap peranti kekal bebas.",
        "nb-rNO": "Speil volumglidebryteren i appen mellom denne telefonen og VIVI Music DE. Deaktiver den for å holde avspillingsvolumet på hver enhet uavhengig.",
        "nl": "Spiegel de volumeschuifregelaar in de app tussen deze telefoon en VIVI Music DE. Schakel het uit om het volume van elke speler onafhankelijk te houden.",
        "pa": "ਇਸ ਫ਼ੋਨ ਅਤੇ VIVI Music DE ਵਿਚਕਾਰ ਐਪ ਦੇ ਵਾਲੀਅਮ ਸਲਾਈਡਰ ਨੂੰ ਮਿਰਰ ਕਰੋ। ਹਰੇਕ ਡਿਵਾਈਸ ਦਾ ਪਲੇਅਰ ਵਾਲੀਅਮ ਸੁਤੰਤਰ ਰੱਖਣ ਲਈ ਇਸਨੂੰ ਬੰਦ ਕਰੋ।",
        "pl": "Odzwierciedlaj suwak głośności w aplikacji między tym telefonem a VIVI Music DE. Wyłącz go, aby głośność odtwarzacza każdego urządzenia pozostała niezależna.",
        "pt": "Espelhe o controlo deslizante de volume da aplicação entre este telemóvel e o VIVI Music DE. Desative-o para manter o volume do leitor independente em cada dispositivo.",
        "pt-rBR": "Espelhe o controle deslizante de volume do app entre este celular e o VIVI Music DE. Desative-o para manter o volume do player independente em cada dispositivo.",
        "ro": "Oglindiți glisorul de volum din aplicație între acest telefon și VIVI Music DE. Dezactivați-l pentru a menține volumul playerului independent pe fiecare dispozitiv.",
        "ru": "Отражайте ползунок громкости в приложении между этим телефоном и VIVI Music DE. Отключите его, чтобы громкость плеера каждого устройства оставалась независимой.",
        "sk": "Zrkadlite posuvník hlasitosti v aplikácii medzi týmto telefónom a VIVI Music DE. Vypnite ho, aby hlasitosť prehrávača každého zariadenia zostala nezávislá.",
        "sl": "Zrcali drsnik za glasnost v aplikaciji med tem telefonom in VIVI Music DE. Onemogoči ga, da glasnost predvajalnika vsake naprave ostane neodvisna.",
        "sv": "Spegla volymreglaget i appen mellan den här telefonen och VIVI Music DE. Inaktivera det för att hålla spelarens volym oberoende på varje enhet.",
        "ta": "இந்த ஃபோனுக்கும் VIVI Music DE-க்கும் இடையே ஆப்ஸின் ஒலி ஸ்லைடரைப் பிரதிபலிக்கவும். ஒவ்வொரு சாதனத்தின் பிளேயர் ஒலியும் தனித்தனியாக இருக்க இதை முடக்கவும்.",
        "te": "ఈ ఫోన్ మరియు VIVI Music DE మధ్య యాప్లోని వాల్యూమ్ స్లయిడర్ను ప్రతిబింబించండి. ప్రతి పరికరం యొక్క ప్లేయర్ వాల్యూమ్ స్వతంత్రంగా ఉంచడానికి దీన్ని నిలిపివేయండి.",
        "th": "สะท้อนแถบเลื่อนระดับเสียงในแอประหว่างโทรศัพท์นี้กับ VIVI Music DE ปิดใช้งานเพื่อให้ระดับเสียงของเครื่องเล่นแต่ละเครื่องเป็นอิสระต่อกัน",
        "tr": "Uygulama içi ses kaydırıcısını bu telefon ile VIVI Music DE arasında yansıt. Her cihazın oynatıcı sesini bağımsız tutmak için devre dışı bırak.",
        "uk": "Віддзеркалюйте повзунок гучності в застосунку між цим телефоном і VIVI Music DE. Вимкніть його, щоб гучність плеєра кожного пристрою залишалася незалежною.",
        "vi": "Phản chiếu thanh trượt âm lượng trong ứng dụng giữa điện thoại này và VIVI Music DE. Tắt nó để giữ âm lượng trình phát của từng thiết bị độc lập.",
        "zh-rCN": "在此手机与 VIVI Music DE 之间镜像应用内音量滑块。禁用它可让每台设备的播放器音量保持独立。",
        "zh-rTW": "在此手機與 VIVI Music DE 之間鏡像應用內音量滑桿。停用它可讓每台裝置的播放器音量保持獨立。",
    },
}


def esc(s):
    """Escape text for an Android <string> element body."""
    return (
        s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "\\'")
    )


def inject(path, key, value):
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    line = '    <string name="%s">%s</string>\n' % (key, esc(value))
    idx = text.rfind("</resources>")
    if idx == -1:
        raise RuntimeError("no </resources> in %s" % path)
    text = text[:idx] + line + text[idx:]
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def main():
    target_files = {}  # dir suffix -> path
    for entry in os.listdir(RES):
        if not entry.startswith("values") or entry == "values":
            continue
        d = os.path.join(RES, entry)
        if not os.path.isdir(d):
            continue
        suffix = entry[len("values"):].lstrip("-")
        if not suffix:
            continue
        for name in ("vivi_strings.xml", "strings.xml"):
            p = os.path.join(d, name)
            if os.path.exists(p):
                target_files[suffix] = p
                break

    injected = 0
    for key, langmap in T.items():
        for lang, value in langmap.items():
            path = target_files.get(lang)
            if path is None:
                print("SKIP (no file): %s" % lang)
                continue
            inject(path, key, value)
            injected += 1

    print("Injected %d translations across %d language files." % (injected, len(target_files)))


if __name__ == "__main__":
    main()
