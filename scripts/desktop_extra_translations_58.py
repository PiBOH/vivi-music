# -*- coding: utf-8 -*-
"""Desktop-only translations: relay/LAN sync status strings.

Keys used by the Device sync section (honest status while waiting for a device
to pair, and the note shown while the LAN section is hidden behind a relay
connection). They don't exist verbatim in the Android `strings.xml`, so they are
defined here for all 47 languages.
"""

EXTRA_TRANSLATIONS = {
    "waiting_for_pairing": {
        "ar": "بانتظار اقتران جهاز…", "as": "ডিভাইচ যোৰা কৰাৰ অপেক্ষাত…", "az": "Cihazın qoşulması gözlənilir…",
        "be": "Чаканне падключэння прылады…", "bg": "Изчакване на свързване на устройство…", "bn": "একটি ডিভাইস পেয়ার করার অপেক্ষায়…",
        "bs": "Čeka se povezivanje uređaja…", "ca": "Esperant que un dispositiu es vinculi…", "cs": "Čekání na spárování zařízení…",
        "de": "Warten auf ein Gerät zum Koppeln…", "el": "Αναμονή για σύζευξη συσκευής…", "es": "Esperando a que se empareje un dispositivo…",
        "et": "Oodatakse seadme sidumist…", "eu": "Gailu bat lotzeko zain…", "fi": "Odotetaan laitteen yhdistämistä…",
        "fil": "Naghihintay ng device na i-pair…", "fr": "En attente d'un appareil à appairer…", "hi": "डिवाइस पेयर होने की प्रतीक्षा…",
        "hr": "Čeka se povezivanje uređaja…", "hu": "Eszköz párosítására várunk…", "id": "Menunggu perangkat untuk dipasangkan…",
        "it": "In attesa di un dispositivo da accoppiare…", "ja": "ペアリングするデバイスを待機中…", "km": "កំពុងរង់ចាំឧបករណ៍សម្រាប់ភ្ជាប់…",
        "ko": "연결할 기기 대기 중…", "lt": "Laukiama įrenginio susiejimo…", "ml": "ജോടിയാക്കാനുള്ള ഉപകരണത്തിനായി കാത്തിരിക്കുന്നു…",
        "ms": "Menunggu peranti untuk dipasangkan…", "nb": "Venter på en enhet å pare…", "nl": "Wachten op een apparaat om te koppelen…",
        "pa": "ਡਿਵਾਈਸ ਪੇਅਰ ਹੋਣ ਦੀ ਉਡੀਕ…", "pl": "Oczekiwanie na sparowanie urządzenia…", "pt": "A aguardar um dispositivo para emparelhar…",
        "ro": "Se așteaptă un dispozitiv de împerecheat…", "ru": "Ожидание сопряжения устройства…", "sk": "Čaká sa na spárovanie zariadenia…",
        "sl": "Čakanje na seznanitev naprave…", "sr": "Чека се повезивање уређаја…", "sv": "Väntar på en enhet att para…",
        "ta": "இணைக்க ஒரு சாதனத்திற்காக காத்திருக்கிறது…", "te": "జత చేయడానికి పరికరం కోసం వేచి ఉంది…", "th": "กำลังรออุปกรณ์เพื่อเชื่อมต่อ…",
        "tr": "Eşleştirilecek cihaz bekleniyor…", "uk": "Очікування на пристрій для сполучення…", "vi": "Đang chờ thiết bị để ghép cặp…",
        "zh-rCN": "正在等待设备配对…", "zh-rTW": "正在等待裝置配對…",
    },
    "lan_hidden_while_relay": {
        "ar": "إخفاء مزامنة LAN أثناء الاتصال بالخادم الوسيط. افصل للاستخدام.", "as": "ৰিলে চাৰ্ভাৰত সংযুক্ত হৈ থাকোঁতে LAN চিংক লুকুৱাই থোৱা হয়। ব্যৱহাৰ কৰিবলৈ সংযোগ বিচ্ছিন্ন কৰক।", "az": "Rely serverinə qoşularkən LAN sinxronu gizlədilir. İstifadə etmək üçün əlaqəni kəsin.",
        "be": "Сінхранізацыя LAN схавана, пакуль падключаны да рэлейнага сервера. Адключыцеся, каб выкарыстоўваць.", "bg": "LAN синхронът е скрит, докато сте свързани към релейния сървър. Прекъснете връзката, за да го използвате.", "bn": "রিলে সার্ভারে সংযুক্ত থাকাকালীন LAN সিঙ্ক লুকানো থাকে। ব্যবহার করতে সংযোগ বিচ্ছিন্ন করুন।",
        "bs": "LAN sinhronizacija je skrivena dok ste povezani na relay server. Prekinite vezu da biste je koristili.", "ca": "La sincronització LAN està oculta mentre esteu connectat al servidor de retransmissió. Desconnecteu-vos per utilitzar-la.", "cs": "Synchronizace LAN je skrytá, dokud jste připojeni k přenosovému serveru. Pro použití se odpojte.",
        "de": "Die LAN-Synchronisierung ist ausgeblendet, solange mit dem Relay-Server verbunden. Trennen Sie die Verbindung, um sie zu nutzen.", "el": "Ο συγχρονισμός LAN είναι κρυφός όσο είστε συνδεδεμένοι στον διακομιστή αναμετάδοσης. Αποσυνδεθείτε για να τον χρησιμοποιήσετε.", "es": "La sincronización LAN está oculta mientras esté conectado al servidor de retransmisión. Desconéctese para usarla.",
        "et": "LAN-sünkroonimine on peidetud, kui olete ühendatud vahendusserveriga. Kasutamiseks ühendage lahti.", "eu": "LAN sinkronizazioa ezkutuan dago errele-zerbitzarira konektatuta zauden bitartean. Deskonektatu erabiltzeko.", "fi": "LAN-synkronointi on piilotettu, kun olet yhteydessä välityspalvelimeen. Irrota yhteys käyttääksesi sitä.",
        "fil": "Nakatago ang LAN sync habang nakakonekta sa relay server. Mag-disconnect para magamit.", "fr": "La synchronisation LAN est masquée tant que vous êtes connecté au serveur relais. Déconnectez-vous pour l'utiliser.", "hi": "रिले सर्वर से कनेक्ट होने पर LAN सिंक छिपा रहता है। उपयोग करने के लिए डिस्कनेक्ट करें।",
        "hr": "LAN sinkronizacija je skrivena dok ste povezani na relay poslužitelj. Prekinite vezu da biste je koristili.", "hu": "A LAN-szinkronizálás rejtett, amíg csatlakozik a relé szerverhez. Használatához csatlakozzon le.", "id": "Sinkronisasi LAN disembunyikan saat terhubung ke server relay. Putuskan koneksi untuk menggunakannya.",
        "it": "La sincronizzazione LAN è nascosta mentre sei connesso al server relay. Disconnettiti per usarla.", "ja": "リレーサーバーに接続中はLAN同期が非表示になります。使用するには切断してください。", "km": "ការធ្វើសមកាលកម្ម LAN ត្រូវបានលាក់ ខណៈពេលដែលភ្ជាប់ទៅម៉ាស៊ីនមេ relay។ ផ្តាច់ការតភ្ជាប់ដើម្បីប្រើវា។",
        "ko": "릴레이 서버에 연결되어 있는 동안 LAN 동기화가 숨겨집니다. 사용하려면 연결을 끊으세요.", "lt": "LAN sinchronizacija paslėpta, kol esate prisijungę prie retransliacijos serverio. Atjunkite, kad ją naudotumėte.", "ml": "റിലേ സെർവറുമായി കണക്റ്റ് ചെയ്തിരിക്കുമ്പോൾ LAN സമന്വയം മറച്ചിരിക്കുന്നു. ഉപയോഗിക്കാൻ വിച്ഛേദിക്കുക.",
        "ms": "Sinkronisasi LAN disembunyikan semasa disambungkan ke pelayan relay. Putuskan sambungan untuk menggunakannya.", "nb": "LAN-synkronisering er skjult mens du er koblet til reléserveren. Koble fra for å bruke den.", "nl": "LAN-synchronisatie is verborgen zolang u verbonden bent met de relayserver. Verbreek de verbinding om deze te gebruiken.",
        "pa": "ਰੀਲੇਅ ਸਰਵਰ ਨਾਲ ਕਨੈਕਟ ਹੋਣ ਤੱਕ LAN ਸਿੰਕ ਲੁਕਿਆ ਹੋਇਆ ਹੈ। ਵਰਤਣ ਲਈ ਡਿਸਕਨੈਕਟ ਕਰੋ।", "pl": "Synchronizacja LAN jest ukryta, dopóki jesteś połączony z serwerem przekaźnikowym. Rozłącz się, aby jej użyć.", "pt": "A sincronização LAN fica oculta enquanto estiver ligado ao servidor de retransmissão. Desligue-se para a utilizar.",
        "ro": "Sincronizarea LAN este ascunsă cât timp sunteți conectat la serverul de retransmisie. Deconectați-vă pentru ao folosi.", "ru": "Синхронизация LAN скрыта, пока вы подключены к релейному серверу. Отключитесь, чтобы использовать её.", "sk": "Synchronizácia LAN je skrytá, kým ste pripojení k prenosovému serveru. Odpojte sa, aby ste ju mohli použiť.",
        "sl": "Sinhronizacija LAN je skrita, dokler ste povezani s posredniškim strežnikom. Odklopite se, da jo uporabite.", "sr": "LAN синхронизација је скривена док сте повезани на релеј сервер. Прекините везу да бисте је користили.", "sv": "LAN-synkronisering är dold medan du är ansluten till reläservern. Koppla från för att använda den.",
        "ta": "ரிலே சேவையகத்துடன் இணைந்திருக்கும்போது LAN ஒத்திசைவு மறைக்கப்படும். பயன்படுத்த இணைப்பைத் துண்டிக்கவும்.", "te": "రిలే సర్వర్కు కనెక్ట్ అయి ఉన్నప్పుడు LAN సింక్ దాచబడుతుంది. ఉపయోగించడానికి డిస్కనెక్ట్ చేయండి.", "th": "ซิงค์ LAN จะถูกซ่อนในขณะที่เชื่อมต่อกับเซิร์ฟเวอร์รีเลย์ ตัดการเชื่อมต่อเพื่อใช้งาน",
        "tr": "Röle sunucusuna bağlıyken LAN senkronu gizlenir. Kullanmak için bağlantıyı kesin.", "uk": "Синхронізацію LAN приховано, поки ви підключені до релейного сервера. Відключіться, щоб використати її.", "vi": "Đồng bộ LAN bị ẩn khi bạn kết nối với máy chủ chuyển tiếp. Ngắt kết nối để sử dụng.",
        "zh-rCN": "连接到中继服务器时，局域网同步将隐藏。断开连接即可使用。", "zh-rTW": "連線到中繼伺服器時，LAN 同步會隱藏。中斷連線即可使用。",
    },
}