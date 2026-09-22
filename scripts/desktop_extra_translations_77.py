# -*- coding: utf-8 -*-
"""Batch 77: the Account screen's "Create on YouTube Music" action.

Four desktop-only sentences (the mobile app creates a playlist on YouTube Music
only from the create dialog's switch, so there is no Android string to reuse):

* `playlists_upload`         — the action's label;
* `playlists_upload_desc`    — what it does, shown under the row;
* `playlists_upload_none`    — shown instead when there is nothing to create;
* `playlists_upload_confirm` — the confirmation, carrying the count.

Language codes are the desktop ones (see DIR_TO_LANG in the generator).
"""

_LANGS = [
    "ar", "as", "az", "sr", "be", "bg", "bn", "bs", "ca", "cs", "de", "el", "es",
    "et", "eu", "fa", "fi", "fil", "fr", "hi", "hr", "hu", "id", "it", "iw", "ja",
    "km", "ko", "lt", "ml", "ms", "nb", "nl", "pa", "pl", "pt", "pt-rBR", "ro",
    "ru", "sk", "sl", "sv", "ta", "te", "th", "tr", "uk", "vi", "zh-rCN", "zh-rTW",
]


def _fill(values):
    """One value per language, asserted complete against [_LANGS]."""
    assert len(values) == len(_LANGS), f"{len(values)} values for {len(_LANGS)} languages"
    return dict(zip(_LANGS, values))


EXTRA_TRANSLATIONS = {
    # The hint the create dialog's sync switch shows without a session: the
    # wording is the app's own (`not_logged_in_youtube`, mapped in the
    # generator) and its resources leave Lithuanian out.
    "not_logged_in_youtube": {
        "lt": "Nesate prisijungę prie „YouTube“",
    },

    # --- "Create on YouTube Music" -------------------------------------------
    "playlists_upload": _fill([
        "إنشاء على YouTube Music", "YouTube Music-ত সৃষ্টি কৰক", "YouTube Music-də yarat", "Napravi na YouTube Music",
        "Стварыць у YouTube Music", "Създай в YouTube Music", "YouTube Music-এ তৈরি করুন", "Napravi na YouTube Music",
        "Crea a YouTube Music", "Vytvořit na YouTube Music", "Auf YouTube Music erstellen", "Δημιουργία στο YouTube Music",
        "Crear en YouTube Music", "Loo YouTube Musicus", "Sortu YouTube Music-en", "ایجاد در YouTube Music",
        "Luo YouTube Musicissa", "Gumawa sa YouTube Music", "Créer sur YouTube Music",
        "YouTube Music पर बनाएँ", "Stvori na YouTube Music", "Létrehozás a YouTube Musicon", "Buat di YouTube Music",
        "Crea su YouTube Music", "צור ב-YouTube Music", "YouTube Music に作成",
        "បង្កើតនៅលើ YouTube Music", "YouTube Music에 만들기", "Kurti „YouTube Music“", "YouTube Music-ൽ സൃഷ്ടിക്കുക", "Cipta di YouTube Music",
        "Opprett på YouTube Music", "Aanmaken op YouTube Music", "YouTube Music 'ਤੇ ਬਣਾਓ", "Utwórz w YouTube Music",
        "Criar no YouTube Music", "Criar no YouTube Music", "Creează în YouTube Music", "Создать в YouTube Music",
        "Vytvoriť v YouTube Music", "Ustvari v YouTube Music", "Skapa på YouTube Music", "YouTube Music-இல் உருவாக்கு",
        "YouTube Musicలో సృష్టించండి", "สร้างใน YouTube Music", "YouTube Music'te oluştur",
        "Створити в YouTube Music", "Tạo trên YouTube Music", "在 YouTube Music 中创建", "在 YouTube Music 中建立",
    ]),

    "playlists_upload_desc": _fill([
        "ينشئ قوائم التشغيل المحلية غير الموجودة على YouTube Music، ويرفع أغانيها ثم يزامن قوائم الحساب.",
        "YouTube Music-ত নথকা স্থানীয় প্লে'লিষ্টসমূহ সৃষ্টি কৰে, গীতসমূহ আপলোড কৰে আৰু তাৰ পিছত একাউণ্টৰ প্লে'লিষ্টসমূহ ছিংক কৰে।",
        "YouTube Music-də olmayan yerli pleylistləri yaradır, mahnılarını yükləyir və sonra hesabdakı pleylistləri sinxronlaşdırır.",
        "Pravi lokalne plejliste kojih nema na YouTube Music, otprema njihove pesme i zatim sinhronizuje plejliste naloga.",
        "Стварае лакальныя плэйлісты, якіх няма на YouTube Music, загружае іх песні і затым сінхранізуе плэйлісты ўліковага запісу.",
        "Създава локалните плейлисти, които още не са в YouTube Music, качва песните им и след това синхронизира плейлистите на акаунта.",
        "যেগুলো এখনও YouTube Music-এ নেই সেগুলো স্থানীয় প্লেলিস্ট তৈরি করে, তাদের গান আপলোড করে এবং তারপর অ্যাকাউন্টের প্লেলিস্ট সিঙ্ক করে।",
        "Pravi lokalne plejliste kojih nema na YouTube Music, otprema njihove pjesme i zatim sinhronizuje plejliste računa.",
        "Crea les llistes locals que encara no són a YouTube Music, puja les seves cançons i després sincronitza les llistes del compte.",
        "Vytvoří místní seznamy, které ještě nejsou na YouTube Music, nahraje jejich skladby a poté synchronizuje seznamy účtu.",
        "Erstellt die lokalen Playlists, die noch nicht auf YouTube Music sind, lädt ihre Songs hoch und synchronisiert danach die Playlists des Kontos.",
        "Δημιουργεί τις τοπικές λίστες που δεν υπάρχουν ακόμη στο YouTube Music, ανεβάζει τα τραγούδια τους και στη συνέχεια συγχρονίζει τις λίστες του λογαριασμού.",
        "Crea las listas locales que aún no están en YouTube Music, sube sus canciones y luego sincroniza las listas de la cuenta.",
        "Loob kohalikud esitusloendid, mida YouTube Musicus veel pole, laadib üles nende lood ja seejärel sünkroonib konto esitusloendid.",
        "Oraindik YouTube Music-en ez dauden zerrenda lokalak sortzen ditu, haien abestiak igotzen ditu eta gero kontuaren zerrendak sinkronizatzen ditu.",
        "فهرست‌های محلی که هنوز در YouTube Music نیستند را می‌سازد، آهنگ‌هایشان را بارگذاری می‌کند و سپس فهرست‌های حساب را همگام می‌کند.",
        "Luo paikalliset soittolistat, joita ei vielä ole YouTube Musicissa, lataa niiden kappaleet ja synkronoi sitten tilin soittolistat.",
        "Gumagawa ng mga lokal na playlist na wala pa sa YouTube Music, ina-upload ang mga kanta nito at pagkatapos ay ini-sync ang mga playlist ng account.",
        "Crée les playlists locales absentes de YouTube Music, envoie leurs titres, puis synchronise les playlists du compte.",
        "जो स्थानीय प्लेलिस्ट अभी YouTube Music पर नहीं हैं उन्हें बनाता है, उनके गाने अपलोड करता है और फिर खाते की प्लेलिस्ट सिंक करता है।",
        "Stvara lokalne playliste kojih nema na YouTube Musicu, prenosi njihove pjesme i zatim sinkronizira playliste računa.",
        "Létrehozza a helyi lejátszási listákat, amelyek még nincsenek a YouTube Musicon, feltölti a számaikat, majd szinkronizálja a fiók listáit.",
        "Membuat playlist lokal yang belum ada di YouTube Music, mengunggah lagunya, lalu menyinkronkan playlist akun.",
        "Crea le playlist locali che non sono ancora su YouTube Music, ne carica i brani e poi sincronizza le playlist dell'account.",
        "יוצר את הפלייליסטים המקומיים שעדיין אינם ב-YouTube Music, מעלה את השירים שלהם ואז מסנכרן את הפלייליסטים של החשבון.",
        "YouTube Music にまだないローカルの再生リストを作成し、曲をアップロードしてからアカウントの再生リストを同期します。",
        "បង្កើតបញ្ជីចាក់ក្នុងម៉ាស៊ីនដែលមិនទាន់មាននៅលើ YouTube Music ផ្ទុកឡើងបទចម្រៀង រួចធ្វើសមកាលកម្មបញ្ជីរបស់គណនី។",
        "YouTube Music에 아직 없는 로컬 재생목록을 만들고 노래를 업로드한 뒤 계정의 재생목록을 동기화합니다.",
        "Sukuria vietinius grojaraščius, kurių dar nėra „YouTube Music“, įkelia jų dainas ir tada sinchronizuoja paskyros grojaraščius.",
        "YouTube Music-ൽ ഇല്ലാത്ത ലോക്കൽ പ്ലേലിസ്റ്റുകൾ സൃഷ്ടിക്കുകയും അവയിലെ ഗാനങ്ങൾ അപ്‌ലോഡ് ചെയ്യുകയും ശേഷം അക്കൗണ്ടിലെ പ്ലേലിസ്റ്റുകൾ സമന്വയിപ്പിക്കുകയും ചെയ്യുന്നു.",
        "Mencipta senarai main tempatan yang belum ada di YouTube Music, memuat naik lagunya, kemudian menyegerakkan senarai main akaun.",
        "Oppretter de lokale spillelistene som ennå ikke er på YouTube Music, laster opp sangene deres og synkroniserer deretter kontoens spillelister.",
        "Maakt de lokale afspeellijsten die nog niet op YouTube Music staan, uploadt hun nummers en synchroniseert daarna de afspeellijsten van het account.",
        "ਉਹ ਸਥਾਨਕ ਪਲੇਲਿਸਟ ਬਣਾਉਂਦਾ ਹੈ ਜੋ ਹਾਲੇ YouTube Music 'ਤੇ ਨਹੀਂ ਹਨ, ਉਨ੍ਹਾਂ ਦੇ ਗੀਤ ਅੱਪਲੋਡ ਕਰਦਾ ਹੈ ਅਤੇ ਫਿਰ ਖਾਤੇ ਦੀਆਂ ਪਲੇਲਿਸਟਾਂ ਸਿੰਕ ਕਰਦਾ ਹੈ।",
        "Tworzy lokalne playlisty, których nie ma jeszcze na YouTube Music, wysyła ich utwory i synchronizuje playlisty konta.",
        "Cria as playlists locais que ainda não estão no YouTube Music, envia as suas músicas e depois sincroniza as playlists da conta.",
        "Cria as playlists locais que ainda não estão no YouTube Music, envia suas músicas e depois sincroniza as playlists da conta.",
        "Creează listele locale care nu sunt încă pe YouTube Music, încarcă melodiile lor și apoi sincronizează listele contului.",
        "Создаёт локальные плейлисты, которых ещё нет в YouTube Music, загружает их песни и затем синхронизирует плейлисты аккаунта.",
        "Vytvorí miestne playlisty, ktoré ešte nie sú na YouTube Music, nahrá ich skladby a potom synchronizuje playlisty účtu.",
        "Ustvari lokalne sezname predvajanja, ki jih še ni na YouTube Music, naloži njihove skladbe in nato sinhronizira sezname računa.",
        "Skapar de lokala spellistorna som ännu inte finns på YouTube Music, laddar upp deras låtar och synkroniserar sedan kontots spellistor.",
        "YouTube Music-இல் இன்னும் இல்லாத உள்ளூர் பிளேலிஸ்ட்களை உருவாக்கி, அவற்றின் பாடல்களை பதிவேற்றி, பின்னர் கணக்கின் பிளேலிஸ்ட்களை ஒத்திசைக்கிறது.",
        "YouTube Musicలో ఇంకా లేని లోకల్ ప్లేలిస్ట్‌లను సృష్టించి, వాటి పాటలను అప్‌లోడ్ చేసి, తర్వాత ఖాతా ప్లేలిస్ట్‌లను సింక్ చేస్తుంది.",
        "สร้างเพลย์ลิสต์ในเครื่องที่ยังไม่มีบน YouTube Music อัปโหลดเพลง แล้วซิงค์เพลย์ลิสต์ของบัญชี",
        "YouTube Music'te olmayan yerel çalma listelerini oluşturur, şarkılarını yükler ve ardından hesabın çalma listelerini eşitler.",
        "Створює локальні плейлисти, яких ще немає в YouTube Music, завантажує їхні пісні й потім синхронізує плейлисти акаунта.",
        "Tạo các danh sách phát cục bộ chưa có trên YouTube Music, tải nhạc của chúng lên rồi đồng bộ danh sách phát của tài khoản.",
        "将尚未在 YouTube Music 上的本地播放列表创建到账号，上传其中的歌曲，然后同步账号的播放列表。",
        "將尚未在 YouTube Music 上的本機播放清單建立到帳戶，上傳其中的歌曲，然後同步帳戶的播放清單。",
    ]),

    "playlists_upload_none": _fill([
        "كل قوائم التشغيل موجودة بالفعل على YouTube Music", "সকলো প্লে'লিষ্ট ইতিমধ্যে YouTube Music-ত আছে",
        "Bütün pleylistlər artıq YouTube Music-dədir", "Sve plejliste su već na YouTube Music",
        "Усе плэйлісты ўжо ёсць на YouTube Music", "Всички плейлисти вече са в YouTube Music",
        "সব প্লেলিস্ট ইতিমধ্যে YouTube Music-এ আছে", "Sve plejliste su već na YouTube Music",
        "Totes les llistes ja són a YouTube Music", "Všechny seznamy už jsou na YouTube Music",
        "Alle Playlists sind bereits auf YouTube Music", "Όλες οι λίστες είναι ήδη στο YouTube Music",
        "Todas las listas ya están en YouTube Music", "Kõik esitusloendid on juba YouTube Musicus",
        "Zerrenda guztiak jada YouTube Music-en daude", "همه فهرست‌ها از قبل در YouTube Music هستند",
        "Kaikki soittolistat ovat jo YouTube Musicissa", "Nasa YouTube Music na ang lahat ng playlist",
        "Toutes les playlists sont déjà sur YouTube Music", "सभी प्लेलिस्ट पहले से YouTube Music पर हैं",
        "Sve su playliste već na YouTube Musicu", "Minden lejátszási lista már fent van a YouTube Musicon",
        "Semua playlist sudah ada di YouTube Music", "Tutte le playlist sono già su YouTube Music",
        "כל הפלייליסטים כבר ב-YouTube Music", "すべての再生リストはすでに YouTube Music にあります",
        "បញ្ជីចាក់ទាំងអស់មាននៅលើ YouTube Music រួចហើយ", "모든 재생목록이 이미 YouTube Music에 있습니다",
        "Visi grojaraščiai jau yra „YouTube Music“", "എല്ലാ പ്ലേലിസ്റ്റുകളും ഇതിനകം YouTube Music-ൽ ഉണ്ട്",
        "Semua senarai main sudah ada di YouTube Music", "Alle spillelistene er allerede på YouTube Music",
        "Alle afspeellijsten staan al op YouTube Music", "ਸਾਰੀਆਂ ਪਲੇਲਿਸਟਾਂ ਪਹਿਲਾਂ ਹੀ YouTube Music 'ਤੇ ਹਨ",
        "Wszystkie playlisty są już na YouTube Music", "Todas as playlists já estão no YouTube Music",
        "Todas as playlists já estão no YouTube Music", "Toate listele sunt deja pe YouTube Music",
        "Все плейлисты уже есть в YouTube Music", "Všetky playlisty už sú na YouTube Music",
        "Vsi seznami so že na YouTube Music", "Alla spellistor finns redan på YouTube Music",
        "அனைத்து பிளேலிஸ்ட்களும் ஏற்கனவே YouTube Music-இல் உள்ளன", "అన్ని ప్లేలిస్ట్‌లు ఇప్పటికే YouTube Musicలో ఉన్నాయి",
        "เพลย์ลิสต์ทั้งหมดอยู่บน YouTube Music แล้ว", "Tüm çalma listeleri zaten YouTube Music'te",
        "Усі плейлисти вже є в YouTube Music", "Mọi danh sách phát đều đã có trên YouTube Music",
        "所有播放列表都已在 YouTube Music 上", "所有播放清單都已在 YouTube Music 上",
    ]),

    "playlists_upload_confirm": _fill([
        "سيؤدي هذا إلى إنشاء %d قائمة تشغيل على حسابك في YouTube Music ورفع أغانيها. هل تريد المتابعة؟",
        "ইয়ে আপোনাৰ YouTube Music একাউণ্টত %d টা প্লে'লিষ্ট সৃষ্টি কৰিব আৰু গীতসমূহ আপলোড কৰিব। আগবাঢ়িব?",
        "Bu, YouTube Music hesabınızda %d pleylist yaradacaq və mahnılarını yükləyəcək. Davam edilsin?",
        "Ovo pravi %d plejlistu/a na tvom YouTube Music nalogu i otprema njihove pesme. Nastaviti?",
        "Гэта створыць %d плэйліст(ы) у вашым уліковым запісе YouTube Music і загрузіць іх песні. Прадоўжыць?",
        "Това ще създаде %d плейлист(а) в акаунта ви в YouTube Music и ще качи песните им. Продължаване?",
        "এটি আপনার YouTube Music অ্যাকাউন্টে %d প্লেলিস্ট তৈরি করবে এবং তাদের গান আপলোড করবে। চালিয়ে যাবেন?",
        "Ovo pravi %d plejlistu/e na tvom YouTube Music računu i otprema njihove pjesme. Nastaviti?",
        "Això crearà %d llista(es) al teu compte de YouTube Music i pujarà les seves cançons. Continuar?",
        "Tím se na tvém účtu YouTube Music vytvoří %d seznam(ů) a nahrají se jejich skladby. Pokračovat?",
        "Dadurch werden %d Playlist(s) in deinem YouTube Music-Konto erstellt und ihre Songs hochgeladen. Fortfahren?",
        "Αυτό θα δημιουργήσει %d λίστα(ες) στον λογαριασμό σου YouTube Music και θα ανεβάσει τα τραγούδια τους. Συνέχεια;",
        "Esto creará %d lista(s) en tu cuenta de YouTube Music y subirá sus canciones. ¿Continuar?",
        "See loob sinu YouTube Musici kontol %d esitusloendit ja laadib üles nende lood. Jätkata?",
        "Honek %d zerrenda sortuko ditu zure YouTube Music kontuan eta haien abestiak igoko ditu. Jarraitu?",
        "این کار %d فهرست را در حساب YouTube Music شما می‌سازد و آهنگ‌هایشان را بارگذاری می‌کند. ادامه می‌دهید؟",
        "Tämä luo %d soittolistaa YouTube Music -tiliisi ja lataa niiden kappaleet. Jatketaanko?",
        "Gagawa ito ng %d playlist sa iyong YouTube Music account at i-a-upload ang mga kanta nito. Magpatuloy?",
        "Cela créera %d playlist(s) sur votre compte YouTube Music et enverra leurs titres. Continuer ?",
        "इससे आपके YouTube Music खाते पर %d प्लेलिस्ट बनेंगी और उनके गाने अपलोड होंगे। जारी रखें?",
        "Ovo stvara %d playlistu/e na tvom YouTube Music računu i prenosi njihove pjesme. Nastaviti?",
        "Ez %d lejátszási listát hoz létre a YouTube Music-fiókodon, és feltölti a számaikat. Folytatod?",
        "Ini akan membuat %d playlist di akun YouTube Music Anda dan mengunggah lagunya. Lanjutkan?",
        "Verranno create %d playlist sul tuo account YouTube Music e ne verranno caricati i brani. Continuare?",
        "פעולה זו תיצור %d פלייליסטים בחשבון YouTube Music שלך ותעלה את השירים שלהם. להמשיך?",
        "YouTube Music アカウントに %d 件の再生リストを作成し、曲をアップロードします。続行しますか？",
        "វានឹងបង្កើតបញ្ជីចាក់ %d នៅលើគណនី YouTube Music របស់អ្នក ហើយផ្ទុកឡើងបទចម្រៀង។ បន្ត?",
        "YouTube Music 계정에 재생목록 %d개를 만들고 노래를 업로드합니다. 계속할까요?",
        "Bus sukurta %d grojaraštis(-iai) jūsų „YouTube Music“ paskyroje ir įkeltos jų dainos. Tęsti?",
        "ഇത് നിങ്ങളുടെ YouTube Music അക്കൗണ്ടിൽ %d പ്ലേലിസ്റ്റ് സൃഷ്ടിക്കുകയും ഗാനങ്ങൾ അപ്‌ലോഡ് ചെയ്യുകയും ചെയ്യും. തുടരണോ?",
        "Ini akan mencipta %d senarai main pada akaun YouTube Music anda dan memuat naik lagunya. Teruskan?",
        "Dette oppretter %d spilleliste(r) på YouTube Music-kontoen din og laster opp sangene deres. Fortsette?",
        "Dit maakt %d afspeellijst(en) aan op je YouTube Music-account en uploadt de nummers. Doorgaan?",
        "ਇਹ ਤੁਹਾਡੇ YouTube Music ਖਾਤੇ 'ਤੇ %d ਪਲੇਲਿਸਟਾਂ ਬਣਾਏਗਾ ਅਤੇ ਉਨ੍ਹਾਂ ਦੇ ਗੀਤ ਅੱਪਲੋਡ ਕਰੇਗਾ। ਜਾਰੀ ਰੱਖਣਾ ਹੈ?",
        "Spowoduje to utworzenie %d playlist(y) na Twoim koncie YouTube Music i wysłanie ich utworów. Kontynuować?",
        "Isto cria %d playlist(s) na sua conta YouTube Music e envia as suas músicas. Continuar?",
        "Isso cria %d playlist(s) na sua conta do YouTube Music e envia suas músicas. Continuar?",
        "Aceasta va crea %d listă(e) în contul tău YouTube Music și va încărca melodiile lor. Continui?",
        "Будет создано %d плейлист(ов) в вашем аккаунте YouTube Music и загружены их песни. Продолжить?",
        "Tým sa na tvojom účte YouTube Music vytvorí %d playlist(ov) a nahrajú sa ich skladby. Pokračovať?",
        "S tem boš ustvaril %d seznam(ov) v svojem računu YouTube Music in naložil njihove skladbe. Nadaljujem?",
        "Detta skapar %d spellista(or) på ditt YouTube Music-konto och laddar upp deras låtar. Fortsätta?",
        "இது உங்கள் YouTube Music கணக்கில் %d பிளேலிஸ்ட்களை உருவாக்கி, அவற்றின் பாடல்களை பதிவேற்றும். தொடரவா?",
        "ఇది మీ YouTube Music ఖాతాలో %d ప్లేలిస్ట్(ల)ను సృష్టించి, వాటి పాటలను అప్‌లోడ్ చేస్తుంది. కొనసాగించాలా?",
        "การดำเนินการนี้จะสร้างเพลย์ลิสต์ %d รายการในบัญชี YouTube Music ของคุณและอัปโหลดเพลง ดำเนินการต่อ?",
        "Bu, YouTube Music hesabınızda %d çalma listesi oluşturur ve şarkılarını yükler. Devam edilsin mi?",
        "Буде створено %d плейлист(ів) у вашому акаунті YouTube Music та завантажено їхні пісні. Продовжити?",
        "Thao tác này sẽ tạo %d danh sách phát trên tài khoản YouTube Music của bạn và tải nhạc của chúng lên. Tiếp tục?",
        "这将在你的 YouTube Music 账号上创建 %d 个播放列表并上传其中的歌曲。继续？",
        "這將在你的 YouTube Music 帳戶上建立 %d 個播放清單並上傳其中的歌曲。繼續？",
    ]),
}
