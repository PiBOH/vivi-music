# -*- coding: utf-8 -*-
"""Desktop translations for the keys the 1.53.21 audit still found in English.

Two families of bug, both found by reading the *generated* tables rather than
the Android resources:

1. Keys whose value was still the English wording (or an English wording with
   slightly different punctuation, e.g. "Stream cache minutes" against
   "Stream cache (minutes)" — which is why an equality check alone missed it).
   `mini_player_standard`, `randomize` and `wrapped_title` were English in all
   50 languages; `wrapped_show_on_home_desc` was a whole English sentence in
   36; `wrapped_listening_time` and the player-background options (Glow,
   Gradient, Blur, Off, Pure black) in 9-33 each.

2. Strings where another language was mixed into the translation, the exact
   complaint: "كلمات أغنية Romanize Kyrgyz" (Arabic + English), "Romanize Kyrgyz
   mahnı sözləri" (Azerbaijani + English), "APIキーは、 deepl.com/pro-api for free
   and paid keys で取得できます" (Japanese + an English clause), "Pro-grade
   acoustic tuning at mga epekto" (Tagalog + English), the Azerbaijani tray
   description and the Tagalog one.

`in`, `nb-rNO` and `pt-rBR` are copied from `id`, `nb` and `pt` by the
generator, so they are not listed here.
"""

EXTRA_TRANSLATIONS = {
    # --- English in all 50 languages -------------------------------------
    "mini_player_standard": {
        "ar": "قياسي", "as": "প্ৰামাণিক", "az": "Standart", "be": "Стандартны",
        "bg": "Стандартен", "bn": "স্ট্যান্ডার্ড", "bs": "Standardni",
        "ca": "Estàndard", "cs": "Standardní", "de": "Standard",
        "el": "Τυπικό", "es": "Estándar", "et": "Tavaline", "eu": "Estandarra",
        "fa": "استاندارد", "fi": "Vakio", "fil": "Pamantayan", "fr": "Standard",
        "hi": "मानक", "hr": "Standardni", "hu": "Normál", "id": "Standar",
        "it": "Standard", "iw": "רגיל", "ja": "標準", "km": "ស្តង់ដារ",
        "ko": "표준", "lt": "Standartinis", "ml": "സ്റ്റാൻഡേർഡ്",
        "ms": "Standard", "nb": "Standard", "nl": "Standaard", "pa": "ਮਿਆਰੀ",
        "pl": "Standardowy", "pt": "Padrão", "ro": "Standard",
        "ru": "Стандартный", "sk": "Štandardný", "sl": "Standardno",
        "sr": "Стандардни", "sv": "Standard", "ta": "நிலையான",
        "te": "ప్రామాణిక", "th": "มาตรฐาน", "tr": "Standart",
        "uk": "Стандартний", "vi": "Tiêu chuẩn", "zh-rCN": "标准",
        "zh-rTW": "標準",
    },
    "randomize": {
        "ar": "ترتيب عشوائي", "as": "এলোমেলো কৰক", "az": "Təsadüfi",
        "be": "Перамяшаць", "bg": "Разбъркване", "bn": "এলোমেলো করুন",
        "bs": "Nasumično", "ca": "Aleatori", "cs": "Náhodně", "de": "Zufällig",
        "el": "Τυχαία σειρά", "es": "Aleatorio", "et": "Juhuslik",
        "eu": "Ausazkoa", "fa": "تصادفی‌سازی", "fi": "Satunnaista",
        "fil": "I-random", "fr": "Aléatoire", "hi": "यादृच्छिक",
        "hr": "Nasumično", "hu": "Véletlen", "id": "Acak", "it": "Casuale",
        "iw": "ערבב", "ja": "ランダム", "km": "ចៃដន្យ", "ko": "무작위",
        "lt": "Atsitiktinė", "ml": "ക്രമരഹിതം", "ms": "Rawak",
        "nb": "Tilfeldig", "nl": "Willekeurig", "pa": "ਬੇਤਰਤੀਬ", "pl": "Losowo",
        "pt": "Aleatório", "ro": "Aleatoriu", "ru": "Случайно",
        "sk": "Náhodne", "sl": "Naključno", "sr": "Насумично", "sv": "Slumpa",
        "ta": "கலக்கு", "te": "షఫుల్", "th": "สุ่ม", "tr": "Rastgele",
        "uk": "Випадково", "vi": "Ngẫu nhiên", "zh-rCN": "随机",
        "zh-rTW": "隨機",
    },
    "wrapped_title": {
        "ar": "VIVI Wrapped · هذه الجلسة", "as": "VIVI Wrapped · এই অধিৱেশন",
        "az": "VIVI Wrapped · Bu sessiya", "be": "VIVI Wrapped · Гэтая сесія",
        "bg": "VIVI Wrapped · Тази сесия", "bn": "VIVI Wrapped · এই সেশন",
        "bs": "VIVI Wrapped · Ova sesija", "ca": "VIVI Wrapped · Aquesta sessió",
        "cs": "VIVI Wrapped · Tato relace", "de": "VIVI Wrapped · Diese Sitzung",
        "el": "VIVI Wrapped · Αυτή η συνεδρία", "es": "VIVI Wrapped · Esta sesión",
        "et": "VIVI Wrapped · See seanss", "eu": "VIVI Wrapped · Saio hau",
        "fa": "VIVI Wrapped · این نشست", "fi": "VIVI Wrapped · Tämä istunto",
        "fil": "VIVI Wrapped · Ang sesyong ito",
        "fr": "VIVI Wrapped · Cette session", "hi": "VIVI Wrapped · यह सत्र",
        "hr": "VIVI Wrapped · Ova sesija", "hu": "VIVI Wrapped · Ez a munkamenet",
        "id": "VIVI Wrapped · Sesi ini", "it": "VIVI Wrapped · Questa sessione",
        "iw": "VIVI Wrapped · ההפעלה הזו", "ja": "VIVI Wrapped · このセッション",
        "km": "VIVI Wrapped · សេសសិននេះ", "ko": "VIVI Wrapped · 이 세션",
        "lt": "VIVI Wrapped · Šis seansas", "ml": "VIVI Wrapped · ഈ സെഷൻ",
        "ms": "VIVI Wrapped · Sesi ini", "nb": "VIVI Wrapped · Denne økten",
        "nl": "VIVI Wrapped · Deze sessie", "pa": "VIVI Wrapped · ਇਹ ਸੈਸ਼ਨ",
        "pl": "VIVI Wrapped · Ta sesja", "pt": "VIVI Wrapped · Esta sessão",
        "ro": "VIVI Wrapped · Această sesiune", "ru": "VIVI Wrapped · Этот сеанс",
        "sk": "VIVI Wrapped · Táto relácia", "sl": "VIVI Wrapped · Ta seja",
        "sr": "VIVI Wrapped · Ова сесија",
        "sv": "VIVI Wrapped · Den här sessionen",
        "ta": "VIVI Wrapped · இந்த அமர்வு", "te": "VIVI Wrapped · ఈ సెషన్",
        "th": "VIVI Wrapped · เซสชันนี้", "tr": "VIVI Wrapped · Bu oturum",
        "uk": "VIVI Wrapped · Цей сеанс", "vi": "VIVI Wrapped · Phiên này",
        "zh-rCN": "VIVI Wrapped · 本次会话", "zh-rTW": "VIVI Wrapped · 本次工作階段",
    },
    # --- English in 38 languages (and the wording differed by a bracket, so
    #     an equality test against the English value missed it) -------------
    "stream_cache_minutes": {
        "ar": "ذاكرة البث المؤقتة (دقائق)", "as": "ষ্ট্ৰীম কেশ্ব (মিনিট)",
        "az": "Yayım keşi (dəqiqə)", "be": "Кэш патоку (хвіліны)",
        "bg": "Кеш на потока (минути)", "bn": "স্ট্রিম ক্যাশে (মিনিট)",
        "bs": "Keš toka (minute)", "ca": "Memòria cau del flux (minuts)",
        "cs": "Mezipaměť streamu (minuty)", "de": "Stream-Cache (Minuten)",
        "el": "Προσωρινή μνήμη ροής (λεπτά)", "es": "Caché del stream (minutos)",
        "et": "Voogedastuse vahemälu (minutid)", "eu": "Fluxuaren cachea (minutuak)",
        "fa": "کش استریم (دقیقه)", "fi": "Suoratoistovälimuisti (minuutit)",
        "fil": "Cache ng stream (minuto)", "fr": "Cache du stream (minutes)",
        "hi": "स्ट्रीम कैश (मिनट)", "hr": "Predmemorija toka (minute)",
        "hu": "Stream-gyorsítótár (perc)", "id": "Cache stream (menit)",
        "it": "Cache dello stream (minuti)", "iw": "מטמון סטרימינג (דקות)",
        "ja": "ストリームキャッシュ（分）", "km": "ឃ្លាំងសម្ងាត់ស្ទ្រីម (នាទី)",
        "ko": "스트림 캐시(분)", "lt": "Srauto talpykla (minutės)",
        "ml": "സ്ട്രീം കാഷെ (മിനിറ്റ്)", "ms": "Cache strim (minit)",
        "nb": "Strømcache (minutter)", "nl": "Streamcache (minuten)",
        "pa": "ਸਟਰੀਮ ਕੈਸ਼ (ਮਿੰਟ)", "pl": "Pamięć podręczna strumienia (minuty)",
        "pt": "Cache do stream (minutos)", "ro": "Cache stream (minute)",
        "ru": "Кэш потока (минуты)", "sk": "Vyrovnávacia pamäť streamu (minúty)",
        "sl": "Predpomnilnik pretoka (minute)", "sr": "Кеш тока (минути)",
        "sv": "Strömningscache (minuter)", "ta": "ஸ்ட்ரீம் தற்காலிக சேமிப்பு (நிமிடங்கள்)",
        "te": "స్ట్రీమ్ కాష్ (నిమిషాలు)", "th": "แคชสตรีม (นาที)",
        "tr": "Yayın önbelleği (dakika)", "uk": "Кеш потоку (хвилини)",
        "vi": "Bộ nhớ đệm luồng (phút)", "zh-rCN": "流缓存（分钟）",
        "zh-rTW": "串流快取（分鐘）",
    },
    # --- the whole English sentence "Display the VIVI Wrapped card …" ------
    "wrapped_show_on_home_desc": {
        "as": "হোম স্ক্ৰীনৰ ওপৰত VIVI Wrapped কাৰ্ডখন দেখুৱাওক।",
        "az": "VIVI Wrapped kartını Əsas ekranın yuxarısında göstərin.",
        "be": "Паказваць картку VIVI Wrapped уверсе галоўнага экрана.",
        "bg": "Показване на картата VIVI Wrapped в горната част на началния екран.",
        "bn": "হোম স্ক্রিনের উপরে VIVI Wrapped কার্ডটি দেখান।",
        "bs": "Prikaži VIVI Wrapped karticu na vrhu početnog ekrana.",
        "ca": "Mostra la targeta VIVI Wrapped a la part superior de la pantalla d'inici.",
        "cs": "Zobrazit kartu VIVI Wrapped v horní části domovské obrazovky.",
        "el": "Εμφάνιση της κάρτας VIVI Wrapped στο επάνω μέρος της αρχικής οθόνης.",
        "et": "Kuva VIVI Wrapped kaart avaekraani ülaosas.",
        "eu": "Erakutsi VIVI Wrapped txartela hasierako pantailaren goialdean.",
        "fi": "Näytä VIVI Wrapped -kortti aloitusnäytön yläosassa.",
        "fil": "Ipakita ang VIVI Wrapped card sa itaas ng Home screen.",
        "hi": "होम स्क्रीन के शीर्ष पर VIVI Wrapped कार्ड दिखाएँ।",
        "hr": "Prikaži VIVI Wrapped karticu na vrhu početnog zaslona.",
        "hu": "A VIVI Wrapped kártya megjelenítése a kezdőképernyő tetején.",
        "id": "Tampilkan kartu VIVI Wrapped di bagian atas layar Beranda.",
        "ja": "VIVI Wrapped カードをホーム画面の上部に表示します。",
        "km": "បង្ហាញកាត VIVI Wrapped នៅផ្នែកខាងលើនៃអេក្រង់ដើម។",
        "ko": "홈 화면 상단에 VIVI Wrapped 카드를 표시합니다.",
        "lt": "Rodyti VIVI Wrapped kortelę pagrindinio ekrano viršuje.",
        "ml": "ഹോം സ്ക്രീനിന്റെ മുകളിൽ VIVI Wrapped കാർഡ് കാണിക്കുക.",
        "ms": "Tunjukkan kad VIVI Wrapped di bahagian atas skrin Utama.",
        "nb": "Vis VIVI Wrapped-kortet øverst på startskjermen.",
        "nl": "Toon de VIVI Wrapped-kaart bovenaan het startscherm.",
        "pa": "ਹੋਮ ਸਕ੍ਰੀਨ ਦੇ ਉੱਪਰ VIVI Wrapped ਕਾਰਡ ਦਿਖਾਓ।",
        "pl": "Pokaż kartę VIVI Wrapped na górze ekranu głównego.",
        "ro": "Afișează cardul VIVI Wrapped în partea de sus a ecranului principal.",
        "sk": "Zobraziť kartu VIVI Wrapped v hornej časti domovskej obrazovky.",
        "sl": "Prikaži kartico VIVI Wrapped na vrhu začetnega zaslona.",
        "sr": "Прикажи VIVI Wrapped картицу на врху почетног екрана.",
        "sv": "Visa VIVI Wrapped-kortet högst upp på startskärmen.",
        "ta": "முகப்புத் திரையின் மேல் VIVI Wrapped அட்டையைக் காட்டு.",
        "te": "హోమ్ స్క్రీన్ పైన VIVI Wrapped కార్డ్‌ను చూపించు.",
        "th": "แสดงการ์ด VIVI Wrapped ที่ด้านบนของหน้าจอหลัก",
        "vi": "Hiển thị thẻ VIVI Wrapped ở đầu màn hình Trang chủ.",
    },
    # --- short English words left in 9-33 languages -----------------------
    "wrapped_listening_time": {
        "as": "শুনি আছে…", "be": "Праслухоўванне…", "bg": "Слушане…",
        "bn": "শোনা হচ্ছে…", "bs": "Slušanje…", "el": "Ακρόαση…",
        "et": "Kuulamine…", "eu": "Entzuten…", "fi": "Kuunnellaan…",
        "fil": "Nakikinig…", "hi": "सुन रहे हैं…", "hr": "Slušanje…",
        "hu": "Hallgatás…", "km": "កំពុងស្តាប់…", "ko": "듣는 중…",
        "lt": "Klausoma…", "ml": "കേൾക്കുന്നു…", "ms": "Mendengar…",
        "nb": "Lytter…", "nl": "Luisteren…", "pa": "ਸੁਣ ਰਿਹਾ ਹੈ…",
        "pl": "Słuchanie…", "pt": "A ouvir…", "sk": "Počúvanie…",
        "sl": "Poslušanje…", "sr": "Слушање…", "sv": "Lyssnar…",
        "ta": "கேட்கிறது…", "te": "వినుతోంది…", "uk": "Прослуховування…",
        "vi": "Đang nghe…", "zh-rTW": "聆聽中…",
    },
    "player_background_glow": {
        "as": "জ্যোতি", "be": "Ззянне", "bg": "Сияние", "bn": "আভা",
        "bs": "Sjaj", "et": "Hõõg", "eu": "Distira", "fi": "Hehku",
        "fil": "Kinang", "hi": "चमक", "hr": "Sjaj", "hu": "Izzás",
        "km": "ពន្លឺ", "ko": "발광", "ml": "തിളക്കം", "ms": "Sinaran",
        "nb": "Glød", "pa": "ਚਮਕ", "sr": "Сјај", "sv": "Glöd",
        "ta": "ஒளிர்வு", "te": "ప్రకాశం",
    },
    "player_background_gradient": {
        "as": "গ্ৰেডিয়েণ্ট", "be": "Градыент", "bn": "গ্রেডিয়েন্ট",
        "ca": "Degradat", "et": "Gradient", "fi": "Liukuväri",
        "fil": "Gradasyon", "hi": "ग्रेडिएंट", "km": "ជម្រាលពណ៌",
        "ko": "그라데이션", "ml": "ഗ്രേഡിയന്റ്", "nb": "Gradient",
        "nl": "Gradiënt", "pa": "ਗ੍ਰੇਡੀਐਂਟ", "pl": "Gradient",
        "ro": "Gradient", "sl": "Preliv", "ta": "சாய்வு",
    },
    "mini_player_bg_gradient": {
        "as": "গ্ৰেডিয়েণ্ট", "be": "Градыент", "bn": "গ্রেডিয়েন্ট",
        "ca": "Degradat", "et": "Gradient", "fi": "Liukuväri",
        "fil": "Gradasyon", "hi": "ग्रेडिएंट", "km": "ជម្រាលពណ៌",
        "ko": "그라데이션", "ml": "ഗ്രേഡിയന്റ്", "nb": "Gradient",
        "nl": "Gradiënt", "pa": "ਗ੍ਰੇਡੀਐਂਟ", "pl": "Gradient",
        "ro": "Gradient", "sl": "Preliv", "ta": "சாய்வு",
    },
    "mini_player_bg_blur": {
        "as": "অস্পষ্ট", "be": "Размытасць", "bn": "ব্লার",
        "fi": "Sumennus", "fil": "Malabo", "hi": "धुंधलापन",
        "km": "ព្រិល", "ko": "흐림", "ml": "മങ്ങൽ",
        "pa": "ਧੁੰਦਲਾਪਣ", "ro": "Estompare", "ta": "மங்கல்",
    },
    "integrations_inactive": {
        "as": "বন্ধ", "eu": "Itzalita", "fil": "Naka-off", "fr": "Désactivé",
        "km": "បិទ", "lt": "Išjungta", "ms": "Mati", "sl": "Izklopljeno",
        "sv": "Av", "th": "ปิด",
    },
    "mini_player_pure_black": {
        "as": "শুদ্ধ কলা", "eu": "Beltz purua", "fil": "Purong itim",
        "km": "ខ្មៅសុទ្ធ", "lt": "Grynas juodas", "ms": "Hitam tulen",
        "sl": "Čisto črna", "sv": "Helt svart", "th": "ดำสนิท",
    },
    # --- another language mixed into a translated string -------------------
    # "كلمات أغنية Romanize Kyrgyz" / "Romanize Kyrgyz mahnı sözləri": the words
    # "Romanize Kyrgyz" were never translated, they were pasted in English.
    "romanize_kyrgyz": {
        "ar": "كتابة أغنية قيرغيزية بالحروف اللاتينية",
        "az": "Qırğız mahnı sözlərini latınlaşdır",
    },
    # "APIキーは、 deepl.com/pro-api for free and paid keys で取得できます"
    "ai_provider_deepl_help": {
        "ja": "APIキーは deepl.com/pro-api で取得できます（無料・有料）",
    },
    # "Pro-grade acoustic tuning at mga epekto"
    "vivi_equalizer_desc": {
        "fil": "Tuning na acoustic na pang-propesyonal at mga epekto",
    },
    # "Trey ikonuna sağ klikləyin: Play/Pause, Next, Previous, Open və Quit."
    "tray_menu_desc": {
        "az": "Trey ikonuna sağ klikləyin: Oxut/Fasilə, Növbəti, Əvvəlki, Aç və Çıx.",
        "fil": "I-right-click ang tray icon para sa play/pause, susunod, nauna, "
               "buksan at isara.",
    },
    # Single words that are the English spelling in languages that do have
    # their own ("Model" instead of "Modell", "Status" instead of "Vəziyyət").
    # "Standard", "Volume", "OK", "Karaoke", "Magenta", "Indigo" and the brand
    # names are deliberately identical in those languages and stay as they are.
    "ai_model": {"de": "Modell", "et": "Mudel"},
    "status": {"az": "Vəziyyət", "fil": "Katayuan"},
    "commits": {
        "ca": "Validacions", "el": "Υποβολές", "es": "Confirmaciones",
        "eu": "Bidalketak", "fr": "Validations", "pt": "Confirmações",
    },
    "animation_speed_normal": {"fil": "Karaniwan", "fr": "Normale"},
    # The one the first pass missed. `discord_presence_desc` was English in 36
    # languages too, but with a *different wording* than the English table (the
    # filler wrote "Shows the current track on your Discord profile." while the
    # table says "… (Windows)."), so comparing against the English value found
    # nothing. An English-prose detector did: every Latin word of the value is
    # an English word.
    "discord_presence_desc": {
        "as": "আপোনাৰ Discord প্ৰফাইলত বৰ্তমান গীতটো দেখুৱায়।",
        "az": "Cari mahnını Discord profilinizdə göstərir.",
        "be": "Паказвае бягучы трэк у вашым профілі Discord.",
        "bg": "Показва текущата песен в профила ви в Discord.",
        "bn": "আপনার Discord প্রোফাইলে বর্তমান গানটি দেখায়।",
        "bs": "Prikazuje trenutnu pjesmu na tvom Discord profilu.",
        "ca": "Mostra la cançó actual al teu perfil de Discord.",
        "cs": "Zobrazuje aktuální skladbu na tvém profilu Discord.",
        "el": "Εμφανίζει το τρέχον κομμάτι στο προφίλ σας στο Discord.",
        "et": "Kuvab praegust lugu sinu Discordi profiilis.",
        "eu": "Uneko abestia zure Discord profilean erakusten du.",
        "fi": "Näyttää nykyisen kappaleen Discord-profiilissasi.",
        "fil": "Ipinapakita ang kasalukuyang kanta sa iyong Discord profile.",
        "hi": "आपके Discord प्रोफ़ाइल पर मौजूदा गाना दिखाता है।",
        "hr": "Prikazuje trenutnu pjesmu na tvom Discord profilu.",
        "hu": "A jelenlegi számot jeleníti meg a Discord-profilodon.",
        "id": "Menampilkan lagu saat ini di profil Discord Anda.",
        "ja": "現在再生中の曲を Discord プロフィールに表示します。",
        "km": "បង្ហាញបទចម្រៀងបច្ចុប្បន្ននៅលើប្រវត្តិរូប Discord របស់អ្នក។",
        "ko": "현재 곡을 Discord 프로필에 표시합니다.",
        "lt": "Rodo dabartinę dainą jūsų Discord profilyje.",
        "ml": "നിലവിലെ ഗാനം നിങ്ങളുടെ Discord പ്രൊഫൈലിൽ കാണിക്കുന്നു.",
        "ms": "Menunjukkan lagu semasa pada profil Discord anda.",
        "nb": "Viser den aktuelle sangen på Discord-profilen din.",
        "pa": "ਤੁਹਾਡੇ Discord ਪਰੋਫਾਈਲ 'ਤੇ ਮੌਜੂਦਾ ਗੀਤ ਦਿਖਾਉਂਦਾ ਹੈ।",
        "ro": "Afișează melodia curentă pe profilul tău Discord.",
        "sk": "Zobrazuje aktuálnu skladbu na tvojom profile Discord.",
        "sl": "Prikaže trenutno skladbo v vašem profilu Discord.",
        "sr": "Приказује тренутну песму на твом Discord профилу.",
        "sv": "Visar den aktuella låten på din Discord-profil.",
        "ta": "தற்போதைய பாடலை உங்கள் Discord சுயவிவரத்தில் காட்டுகிறது.",
        "te": "ప్రస్తుత పాటను మీ Discord ప్రొఫైల్‌లో చూపిస్తుంది.",
        "th": "แสดงเพลงปัจจุบันบนโปรไฟล์ Discord ของคุณ",
        "vi": "Hiển thị bài hát hiện tại trên hồ sơ Discord của bạn.",
    },
    "lyrics_apple_blur": {"tr": "Apple Music şarkı sözü bulanıklığı"},
    "pure_black_mini": {"fil": "Purong itim na mini player"},
    # Azerbaijani, word by word: the same machine gluing that put English words
    # inside Azeri sentences ("Aktiv et glowing mahnı sözləri effekt"). Where the
    # string exists in the mobile resources (lyrics_glow_effect, …) it is fixed
    # there and both apps pick it up; these have no Android resource, so the
    # desktop owns them.
    "lyrics_apple_blur": {
        "az": "Apple Music bulanıqlaşdırması",
        "tr": "Apple Music şarkı sözü bulanıklığı",
    },
    "lyrics_apple_blur_desc": {
        "az": "Cari sətrin ətrafındakı sətirləri Apple Music pleyerindəki kimi "
              "bulanıqlaşdırın",
    },
    "lyrics_style_apple_v2": {"az": "Apple Music V2 (hərf-hərf)"},
    "lyrics_style_lyrics_v2": {"az": "Mahnı sözləri V2 (axıcı)"},
    "lyrics_style_vivimusic": {"az": "VIVI Music (axıcı)"},
    "player_design_desc": {"az": "Tam pleyerin düzüm üslubu."},
    "translate_lyrics": {"az": "Mahnı sözlərini AI ilə tərcümə et"},
    "snapshot_received": {"az": "Anlıq görüntü alındı"},
    "pure_black_mini_desc": {
        "az": "Qaranlıq rejimdə mini pleyer üçün həqiqi AMOLED qara fonundan "
              "istifadə edin",
    },
    "welcome_title": {"az": "VIVI Music DE-yə xoş gəlmisiniz"},
    # "Romanize Bulgarian mahnı sözləri": one English verb in front of an Azeri
    # noun phrase, eleven times over.
    "romanize_belarusian": {"az": "Belarus mahnı sözlərini latınlaşdır"},
    "romanize_bulgarian": {"az": "Bolqar mahnı sözlərini latınlaşdır"},
    "romanize_chinese": {"az": "Çin mahnı sözlərini latınlaşdır"},
    "romanize_hindi": {"az": "Hind mahnı sözlərini latınlaşdır"},
    "romanize_japanese": {"az": "Yapon mahnı sözlərini latınlaşdır"},
    "romanize_korean": {"az": "Koreya mahnı sözlərini latınlaşdır"},
    "romanize_macedonian": {"az": "Makedon mahnı sözlərini latınlaşdır"},
    "romanize_punjabi": {"az": "Pəncab mahnı sözlərini latınlaşdır"},
    "romanize_russian": {"az": "Rus mahnı sözlərini latınlaşdır"},
    "romanize_serbian": {"az": "Serb mahnı sözlərini latınlaşdır"},
    "romanize_ukrainian": {"az": "Ukrayna mahnı sözlərini latınlaşdır"},
    # The same four strings again: their Azeri value reaches the desktop from an
    # extra batch rather than from the mobile resource the file above fixes, so
    # they are spelled out here as well.
    "hide_explicit": {"az": "Yetkin məzmunu gizlət"},
    "hide_video_songs": {"az": "Video mahnıları gizlət"},
    "hide_youtube_shorts": {"az": "YouTube Shorts-u gizlət"},
    "yt_sync": {"az": "YouTube Music hesabınızla avtomatik sinxronlaşdır"},
}
