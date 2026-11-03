package com.fyp.healthcare.i18n

/**
 * English -> Bahasa Melayu text.
 *
 * Keyed by the English source string so screens stay readable: `Text("Get Started")` draws
 * "Get Started" in English and "Mula" in Malay. Anything missing here falls straight through
 * to the English, which keeps untranslated corners usable instead of blank.
 *
 * House style, so the two lists read as one voice:
 *  - "anda" (you), lowercase except at the start of a sentence;
 *  - "log masuk" / "log keluar", not "masuk" / "keluar" on their own;
 *  - medication is "ubat", an appointment is "temu janji", a reading is "bacaan";
 *  - buttons stay one or two words so they fit the same buttons as the English.
 */
internal object MalayStrings {

    private val core: Map<String, String> = buildMap {
        // ---------- Common verbs, buttons and states ----------
        put("Add", "Tambah")
        put("Add ...", "Tambah ...")
        put("Accept", "Terima")
        put("Back", "Kembali")
        put("Cancel", "Batal")
        put("Change", "Tukar")
        put("Clear", "Kosongkan")
        put("Close", "Tutup")
        put("Confirm", "Sahkan")
        put("Continue", "Teruskan")
        put("Copy", "Salin")
        put("Delete", "Padam")
        put("Done", "Selesai")
        put("Edit", "Sunting")
        put("More", "Lagi")
        put("Next", "Seterusnya")
        put("No", "Tidak")
        put("Not now", "Bukan sekarang")
        put("Remove", "Buang")
        put("Reset", "Set semula")
        put("Retry", "Cuba lagi")
        put("Save", "Simpan")
        put("Search", "Cari")
        put("See all", "Lihat semua")
        put("Share", "Kongsi")
        put("Skip", "Langkau")
        put("Stop", "Henti")
        put("Take", "Ambil")
        put("Undo", "Batalkan")
        put("Yes", "Ya")
        put("Go back", "Kembali")
        put("Generate", "Jana")
        put("Remove time", "Buang masa")
        put("Add time", "Tambah masa")
        put("Use this day", "Guna hari ini")
        put("Use this address", "Guna alamat ini")
        put("Try again", "Cuba lagi")
        put("Something went wrong. Try again.", "Sesuatu tidak kena. Cuba lagi.")
        put("Searching nearby…", "Mencari berdekatan…")
        put("Getting the map…", "Memuatkan peta…")

        // ---------- Get Started / sign-in ----------
        put("Get Started", "Mula")
        put("Monitor health, stay safe", "Pantau kesihatan, kekal selamat")
        put("Continue with Google, or use a username and password",
            "Teruskan dengan Google, atau guna nama pengguna dan kata laluan")
        put("Continue with Google", "Teruskan dengan Google")
        put("Continue as", "Teruskan sebagai")
        put("By continuing you agree to CareApp's Terms & Privacy Policy",
            "Dengan meneruskan, anda bersetuju dengan Terma & Dasar Privasi CareApp")
        put("Sign up", "Daftar")
        put("Sign in", "Log masuk")
        put("Sign Up", "Daftar")
        put("Sign In", "Log masuk")
        put("Sign Out", "Log keluar")
        put("Sign out", "Log keluar")
        put("Sign out?", "Log keluar?")
        put("You'll need to sign in again to use CareApp.",
            "Anda perlu log masuk semula untuk menggunakan CareApp.")
        put(" or ", " atau ")
        put("Welcome back", "Selamat kembali")
        put("Create your account", "Cipta akaun anda")
        put("Already have an account? Sign in", "Sudah ada akaun? Log masuk")
        put("New here? Sign up", "Baru di sini? Daftar")

        // ---------- Accounts, usernames, passwords ----------
        put("Username", "Nama pengguna")
        put("Password", "Kata laluan")
        put("Current password", "Kata laluan semasa")
        put("New password", "Kata laluan baharu")
        put("Re-enter password", "Masukkan semula kata laluan")
        put("Repeat new password", "Ulang kata laluan baharu")
        put("Your password", "Kata laluan anda")
        put("Your name", "Nama anda")
        put("Your", "Anda")
        put("Enter your name", "Masukkan nama anda")
        put("Enter your password.", "Masukkan kata laluan anda.")
        put("Enter your current password.", "Masukkan kata laluan semasa anda.")
        put("Show password", "Tunjukkan kata laluan")
        put("Hide password", "Sembunyikan kata laluan")
        put("Choose a username.", "Pilih nama pengguna.")
        put("Sign in with the username and password you chose.",
            "Log masuk dengan nama pengguna dan kata laluan yang anda pilih.")
        put("No account with that username. Sign up first.",
            "Tiada akaun dengan nama pengguna itu. Daftar dahulu.")
        put("That username is taken. Try another.", "Nama pengguna itu telah digunakan. Cuba yang lain.")
        put("That username isn't allowed.", "Nama pengguna itu tidak dibenarkan.")
        put("Wrong username or password.", "Nama pengguna atau kata laluan salah.")
        put("The two passwords don't match.", "Kedua-dua kata laluan tidak sepadan.")
        put("The two new passwords don't match.", "Kedua-dua kata laluan baharu tidak sepadan.")
        put("That's already your password.", "Itu sudah pun kata laluan anda.")
        put("Too many attempts. Wait a minute, then try again.",
            "Terlalu banyak percubaan. Tunggu sebentar, kemudian cuba lagi.")
        put("Couldn't sign you in. Try again.", "Tidak dapat log masuk. Cuba lagi.")
        put("Couldn't reach the server. Check your internet connection and try again.",
            "Tidak dapat menghubungi pelayan. Semak sambungan internet anda dan cuba lagi.")
        put("This account has been disabled.", "Akaun ini telah dinyahdayakan.")
        put("Change password", "Tukar kata laluan")
        put("Confirm your password", "Sahkan kata laluan anda")
        put("Confirm your password to finish connecting.",
            "Sahkan kata laluan anda untuk melengkapkan sambungan.")
        put("For your security, type your password above to confirm it's you, then try again.",
            "Demi keselamatan anda, taip kata laluan anda di atas untuk mengesahkan identiti, kemudian cuba lagi.")
        put("Your username stays the same. You'll need the new password next time you sign in.",
            "Nama pengguna anda kekal sama. Anda perlu kata laluan baharu pada log masuk seterusnya.")
        put("Sign-in and security", "Log masuk dan keselamatan")
        put("Google account", "Akaun Google")
        put("Connect Google", "Sambung Google")
        put("Connected", "Disambungkan")
        put("Not connected", "Tidak disambungkan")
        put("Not connected yet", "Belum disambungkan")
        put("Not signed in", "Belum log masuk")
        put("Not signed in.", "Belum log masuk.")
        put("CareApp account", "Akaun CareApp")
        put("Google is already connected to this account.", "Google sudah disambungkan ke akaun ini.")
        put("Cloud", "Awan")
        put("Offline", "Luar talian")
        put("Online", "Dalam talian")
        put("Deleting…", "Memadam…")
        put("Saving…", "Menyimpan…")
        put("Generating…", "Menjana…")
        put("Connecting...", "Menyambung...")
        put("Connecting…", "Menyambung…")

        // ---------- Delete account ----------
        put("Delete account", "Padam akaun")
        put("Delete this account?", "Padam akaun ini?")
        put("Erases this account and its data", "Padam akaun ini dan datanya")
        put("Type DELETE", "Taip DELETE")
        put("Type DELETE to confirm.", "Taip DELETE untuk mengesahkan.")
        put("Delete this appointment?", "Padam temu janji ini?")

        // ---------- Role select / get started flow ----------
        put("Who's using CareApp?", "Siapa yang menggunakan CareApp?")
        put("Pick the option that fits you. You can change this later in Settings.",
            "Pilih pilihan yang sesuai dengan anda. Anda boleh mengubahnya kemudian dalam Tetapan.")
        put("I'm the Patient", "Saya Pesakit")
        put("I'm a Caregiver", "Saya Penjaga")
        put("Track my own health, medication and emergency profile.",
            "Jejak kesihatan, ubat dan profil kecemasan saya sendiri.")
        put("Link to a family member's account with their code and help manage it.",
            "Pautkan ke akaun ahli keluarga dengan kod mereka dan bantu mengurusnya.")
        put("Style & theme", "Gaya & tema")
        put("System", "Sistem")
        put("Light", "Cerah")
        put("Dark", "Gelap")
        put("Calm", "Tenang")
        put("Standard", "Biasa")
        put("You're signed in with Google · sign out to switch account",
            "Anda log masuk dengan Google · log keluar untuk menukar akaun")

        // ---------- Profile / settings ----------
        put("Profile", "Profil")
        put("Settings", "Tetapan")
        put("Setting", "Tetapan")
        put("Preferences", "Keutamaan")
        put("Language", "Bahasa")
        put("Dark mode", "Mod gelap")
        put("System default", "Lalai sistem")
        put("Look and feel", "Rupa dan rasa")
        put("Card shadows", "Bayang kad")
        put("Patterned background", "Latar bercorak")
        put("Flat colours and thin edges, no texture or depth",
            "Warna rata dan tepi nipis, tanpa tekstur atau kedalaman")
        put("Soft gradients and gentle shadows",
            "Gradien lembut dan bayang halus")
        put("Notifications", "Pemberitahuan")
        put("Allow notifications", "Benarkan pemberitahuan")
        put("On", "Hidup")
        put("Off", "Mati")
        put("Health Profile", "Profil Kesihatan")
        put("Edit Health Profile", "Sunting Profil Kesihatan")
        put("Set Up Your Profile", "Sediakan Profil Anda")
        put("Medical details", "Butiran perubatan")
        put("Reading history", "Sejarah bacaan")
        put("Medication history", "Sejarah ubat")
        put("Emergency Contacts", "Kenalan Kecemasan")
        put("No contacts added", "Tiada kenalan ditambah")
        put("Add people to call in an emergency", "Tambah orang untuk dihubungi semasa kecemasan")
        put("Add at least one emergency contact with a name and phone number",
            "Tambah sekurang-kurangnya satu kenalan kecemasan dengan nama dan nombor telefon")
        put("Daily step goal", "Sasaran langkah harian")
        put("Between 1,000 and 50,000 steps", "Antara 1,000 dan 50,000 langkah")
        put("steps", "langkah")
        put("Patient account · managed by you", "Akaun pesakit · diurus oleh anda")
        put("Added by you", "Ditambah oleh anda")
        put("Save Changes", "Simpan Perubahan")
        put("Save Appointment", "Simpan Temu Janji")
        put("Save Medication", "Simpan Ubat")
        put("Save Readings", "Simpan Bacaan")
        put("Nothing recorded yet · your readings will show here.",
            "Belum ada rekod · bacaan anda akan muncul di sini.")
        put("Nothing recorded yet", "Belum ada rekod")
        put("Nothing logged yet", "Belum ada catatan")
        put("Nothing scheduled for today.", "Tiada jadual untuk hari ini.")
        put("No medications added", "Tiada ubat ditambah")
        put("No medications yet", "Belum ada ubat")
        put("No appointments yet", "Belum ada temu janji")
        put("No Data", "Tiada Data")
        put("No data", "Tiada data")
        put("No readings", "Tiada bacaan")
        put("None", "Tiada")
        put("None on file", "Tiada dalam rekod")
        put("No known allergies recorded", "Tiada alahan diketahui direkodkan")
        put("No conditions recorded", "Tiada keadaan direkodkan")
        put("No caregivers linked yet.", "Belum ada penjaga dipautkan.")
        put("No caregiver linked to this account", "Tiada penjaga dipautkan ke akaun ini")

        // ---------- Profile fields ----------
        put("Name", "Nama")
        put("Date of birth", "Tarikh lahir")
        put("Pick your date of birth", "Pilih tarikh lahir anda")
        put("Gender", "Jantina")
        put("Pick a gender", "Pilih jantina")
        put("Male", "Lelaki")
        put("Female", "Perempuan")
        put("Other", "Lain-lain")
        put("Height", "Tinggi")
        put("Weight", "Berat")
        put("Blood Type", "Jenis Darah")
        put("Blood type", "Jenis darah")
        put("Blood type (optional)", "Jenis darah (pilihan)")
        put("Age", "Umur")
        put("Age / Gender", "Umur / Jantina")
        put("Age & sex not set", "Umur & jantina belum ditetapkan")
        put("Address", "Alamat")
        put("Address (optional)", "Alamat (pilihan)")
        put("Phone number", "Nombor telefon")
        put("Emergency", "Kecemasan")
        put("Emergency Profile", "Profil Kecemasan")
        put("Allergies", "Alahan")
        put("Allergies (optional)", "Alahan (pilihan)")
        put("Medical Conditions", "Keadaan Perubatan")
        put("Medical conditions (optional)", "Keadaan perubatan (pilihan)")
        put("Current Medications", "Ubat Semasa")
        put("Notes (optional)", "Nota (pilihan)")
        put("Description (optional)", "Keterangan (pilihan)")
        put("Relationship (e.g. Wife, Doctor)", "Hubungan (cth. Isteri, Doktor)")
        put("Add contact", "Tambah kenalan")
        put("Remove contact", "Buang kenalan")
        put("Call", "Panggil")
        put("Dials emergency services (999)", "Menghubungi perkhidmatan kecemasan (999)")
        put("EMERGENCY CALL", "PANGGILAN KECEMASAN")
        put("Open Emergency page", "Buka halaman Kecemasan")
        put("Emergency contacts *", "Kenalan kecemasan *")
        put("Medical Background", "Latar Belakang Perubatan")
        put("MEDICAL BACKGROUND", "LATAR BELAKANG PERUBATAN")
        put("Conditions", "Keadaan")
        put("Blood pressure", "Tekanan darah")
        put("Blood sugar", "Gula darah")
        put("Heart rate", "Kadar denyutan jantung")
        put("Temperature", "Suhu")
        put("Oxygen", "Oksigen")
        put("Oxygen Level", "Paras Oksigen")
        put("Oxygen (SpO₂)", "Oksigen (SpO₂)")
        put("Sleep", "Tidur")
        put("Steps Walked", "Langkah Berjalan")
        put("Calories", "Kalori")
        put("Patient", "Pesakit")
        put("Caregiver", "Penjaga")
        put("Family Caregiver", "Penjaga Keluarga")
        put("The patient", "Pesakit")
        put("the patient", "pesakit")
        put("patient", "pesakit")
        put("Guest", "Tetamu")
        put("User", "Pengguna")
        put("All users", "Semua pengguna")
        put("all users", "semua pengguna")

        // ---------- Home / navigation ----------
        put("Home", "Utama")
        put("Health", "Kesihatan")
        put("Reminders", "Peringatan")
        put("Activity", "Aktiviti")
        put("Quick Actions", "Tindakan Pantas")
        put("Today's Summary", "Ringkasan Hari Ini")
        put("Today", "Hari ini")
        put("Yesterday", "Semalam")
        put("Tomorrow", "Esok")
        put("This week", "Minggu ini")
        put("This month", "Bulan ini")
        put("Month to date", "Sehingga kini bulan ini")
        put("Record Data", "Rekod Data")
        put("Record Health Data", "Rekod Data Kesihatan")
        put("Health Report", "Laporan Kesihatan")
        put("Health report", "Laporan kesihatan")
        put("Health reports", "Laporan kesihatan")
        put("Appointments", "Temu Janji")
        put("Appointment", "Temu Janji")
        put("Nearby Clinics", "Klinik Berdekatan")
        put("Emotional Support", "Sokongan Emosi")
        put("Health Trends", "Trend Kesihatan")
        put("Activity Monitoring", "Pemantauan Aktiviti")
        put("Monitoring", "Pemantauan")
        put("Monitoring active", "Pemantauan aktif")
        put("Monitoring not active", "Pemantauan tidak aktif")
        put("Analysis", "Analisis")
        put("Summary", "Ringkasan")
        put("Weekly", "Mingguan")
        put("Monthly", "Bulanan")
        put("Avg", "Purata")
        put("Min", "Min")
        put("Max", "Maks")
        put("Range", "Julat")
        put("Date", "Tarikh")
        put("Time", "Masa")
        put("Type", "Jenis")
        put("Status", "Status")

        // ---------- Linking ----------
        put("Link Account", "Pautkan Akaun")
        put("Link to a patient", "Pautkan ke pesakit")
        put("Your Family Code", "Kod Keluarga Anda")
        put("Share family code", "Kongsi kod keluarga")
        put("Unlink", "Nyahpaut")
        put("Unlink from this patient", "Nyahpaut daripada pesakit ini")
        put("Linked", "Dipautkan")
        put("Linked Caregivers", "Penjaga Dipautkan")
        put("1 linked", "1 dipautkan")
        put("Not linked · tap to add", "Belum dipaut · ketik untuk tambah")
        put("Code copied", "Kod disalin")
        put("That code doesn't exist", "Kod itu tidak wujud")
        put("That code isn't valid", "Kod itu tidak sah")
        put("Couldn't link with that code", "Tidak dapat memaut dengan kod itu")
        put("That's your own code", "Itu kod anda sendiri")
        put("Family code", "Kod keluarga")
        put("CareApp family code", "Kod keluarga CareApp")

        // ---------- Strings that only ever appear mid-sentence ----------
        put("You're viewing ", "Anda melihat akaun ")
        put("'s account · changes save to their profile.",
            " · perubahan disimpan ke dalam profil mereka.")
        put("Managing ", "Mengurus akaun ")
        put("'s account", "")
        put("Monitoring", "Memantau")
        put("live", "langsung")
        put("Good Morning,", "Selamat pagi,")
        put("Good Afternoon,", "Selamat tengah hari,")
        put("Good Evening,", "Selamat petang,")
        put("Good Night,", "Selamat malam,")
        put("You're viewing", "Anda melihat")
        put("the patient", "pesakit")
        put("patient", "pesakit")
        put("8-character code", "kod 8 aksara")
        put("Contact", "Kenalan")
        put("Open", "Buka")
        put("Open now", "Dibuka sekarang")
        put("Closed", "Ditutup")
        put("All", "Semua")
        put("Add your date of birth, gender, height and weight. The Emergency Profile screen will use these.",
            "Tambah tarikh lahir, jantina, tinggi dan berat anda. Halaman Profil Kecemasan akan menggunakan maklumat ini.")
        put("Reopen", "Buka semula")
        put("Mark attended", "Tanda hadir")
        put("Select date", "Pilih tarikh")
        put("Managing", "Mengurus")
        put("Streaming activity data", "Menstrim data aktiviti")
        put("Other Days", "Hari Lain")
        put("Only when it is", "Hanya apabila ia")
        put("Analysing your recorded data…", "Menganalisis data yang direkodkan…")
        put("Tap to set name, dosage & reminder time", "Ketik untuk tetapkan nama, dos & masa peringatan")
        put("Add your first reminder and we'll notify you when it's due.",
            "Tambah peringatan pertama anda dan kami akan memberitahu apabila tiba masanya.")
        put("No sleep captured last night · keep the phone near your bed",
            "Tiada data tidur malam tadi · letakkan telefon berhampiran katil")
        put("At least one person to call from your Emergency Profile",
            "Sekurang-kurangnya seorang untuk dihubungi daripada Profil Kecemasan anda")
        put("Anyone with this code can view and manage your health data, medication and ",
            "Sesiapa yang mempunyai kod ini boleh melihat dan mengurus data kesihatan, ubat dan ")
        put("Share your code with a family member so they can help manage your health.",
            "Kongsi kod anda dengan ahli keluarga supaya mereka boleh membantu mengurus kesihatan anda.")
        put("Their health data saves to their account",
            "Data kesihatan mereka disimpan dalam akaun mereka")
        put("Get the family code from the patient.\nIt's in their Profile · Connections · Family Caregiver.",
            "Dapatkan kod keluarga daripada pesakit.\nIa ada dalam Profil · Sambungan · Penjaga Keluarga.")
        put("Forgotten it? Use Continue with Google if you linked one - ",
            "Terlupa? Guna Teruskan dengan Google jika anda telah memautkannya - ")
        put("Pick a username and password.\nNo email needed · they're only used to sign in to CareApp.",
            "Pilih nama pengguna dan kata laluan.\nTiada e-mel diperlukan · ia hanya digunakan untuk log masuk ke CareApp.")
        put("A username account has no email to send a reset link to,\nso keep your password safe.",
            "Akaun nama pengguna tiada e-mel untuk pautan set semula,\njadi jaga kata laluan anda.")
        put("CareApp · v0.9.0 (FYP build · radar integration pending)",
            "CareApp · v0.9.0 (binaan FYP · integrasi radar belum selesai)")
        put("ESP32 Bluetooth Test", "Ujian Bluetooth ESP32")
        put("Device: HealthCare_Radar", "Peranti: HealthCare_Radar")
        put("Medication\nAdded", "Ubat\nDitambah")
        put("Type it again", "Taip sekali lagi")
        put("Your readings", "Bacaan anda")
        put("Your reminder is set.\nWe'll let you know when it's due.",
            "Peringatan anda telah ditetapkan.\nKami akan memberitahu apabila tiba masanya.")
        put("radar", "radar")
        put("wearable", "boleh pakai")
        put("mmWave Radar Sensor", "Sensor Radar mmWave")
        put("Go", "Pergi")
        put("all", "semua")
        put("← Back", "← Kembali")
        put("Change language", "Tukar bahasa")

        // ---------- Radar / Bluetooth ----------
        put("Connect Radar", "Sambung Radar")
        put("Couldn't connect", "Tidak dapat menyambung")
        put("Connecting...", "Menyambung...")
        put("Bluetooth permission denied. Allow it in Settings.",
            "Kebenaran Bluetooth ditolak. Benarkan dalam Tetapan.")
        put("Not connected", "Tidak bersambung")
        put("Connected, waiting for radar data...", "Disambung, menunggu data radar...")
        put("Nobody detected", "Tiada sesiapa dikesan")
        put("Possible fall detected!", "Kemungkinan jatuh dikesan!")
        put("I'm OK", "Saya OK")
        put("Each ring is 1 metre. The red dot is the sensor.",
            "Setiap cincin ialah 1 meter. Titik merah ialah sensor.")
        put("Test fall alert", "Uji amaran jatuh")
        put("Fall alert detected", "Amaran jatuh dikesan")
        put("The radar detected a possible fall. Please check right away.",
            "Radar mengesan kemungkinan jatuh. Sila semak dengan segera.")
        put("Checking Bluetooth connection...", "Memeriksa sambungan Bluetooth...")
        put("Connected to HealthCare_Radar", "Berjaya disambung ke HealthCare_Radar")
        put("This phone doesn't support Bluetooth.", "Telefon ini tidak menyokong Bluetooth.")
        put("Bluetooth isn't connected", "Bluetooth belum disambungkan")
        put("Bluetooth isn't supported", "Bluetooth tidak disokong")
        put("Bluetooth permission is required", "Kebenaran Bluetooth diperlukan")
        put("Please turn on Bluetooth", "Sila hidupkan Bluetooth telefon")
        put("Bluetooth permission was not granted", "Kebenaran Bluetooth tidak diberikan")
        put("ESP32 isn't paired yet. Pair HealthCare_Radar first.",
            "ESP32 belum dipair. Pair HealthCare_Radar dahulu.")
        put("Bluetooth was disconnected", "Bluetooth telah terputus")
        put("Bluetooth disconnected", "Bluetooth terputus")
        put("Make sure the ESP32 is on and paired with the phone before you tap Connect.",
            "Pastikan ESP32 hidup dan telah dipair dengan telefon sebelum menekan Sambung.")
        put("No community readings for ", "Belum ada bacaan komuniti untuk ")
        put(" in this period yet.", " dalam tempoh ini.")
        put(" readings recorded for this period.", " bacaan direkodkan untuk tempoh ini.")
        put("No ", "Tiada ")
        put("Latest ", "Terkini ")
        put("there", "anda")
        put("you", "anda")
        put("your readings", "bacaan anda")
    }

    private val content: Map<String, String> = MalayStringsContent.text

    private val everything: Map<String, String> = core + content

    /**
     * Number-and-unit sentences that are assembled at runtime ("1,000 steps", "3 readings",
     * "Delete Amlodipine?"). A plain map can't match those because the number varies, so they
     * get a short pattern pass after the exact lookup misses.
     */
    private val patterns: List<Pair<Regex, String>> = listOf(
        Regex("""^([\d,]+) steps$""") to "$1 langkah",
        Regex("""^([\d,]+) (reading|readings)$""") to "$1 bacaan",
        Regex("""^([\d,]+) (day|days) ago$""") to "$1 hari lalu",
        Regex("""^([\d,]+) (min|mins) ago$""") to "$1 min lalu",
        Regex("""^([\d,]+) (hour|hours) ago$""") to "$1 jam lalu",
        Regex("""^In ([\d,]+) (day|days)$""") to "Dalam $1 hari",
        Regex("""^([\d,]+) (time|times) a day$""") to "$1 kali sehari",
        Regex("""^([\d,]+) contacts?$""") to "$1 kenalan",
        Regex("""^([\d,]+) linked$""") to "$1 dipautkan",
        Regex("""^([\d,]+)% of daily goal \((.+)\)$""") to "$1% daripada sasaran harian ($2)",
        Regex("""^Delete (.+)\?$""") to "Padam $1?",
        Regex("""^Remove (.+)\?$""") to "Buang $1?",
        Regex("""^Today · (.+)$""") to "Hari ini · $1",
        Regex("""^Hours: (.+)$""") to "Waktu: $1",
        Regex("""^· page (\d+) of (\d+)$""") to "· halaman $1 daripada $2",
        Regex("""^(\d+)\+ characters$""") to "$1+ aksara",
        Regex("""^(\d+) targets? detected$""") to "$1 sasaran dikesan",
        Regex("""^Alert will be sent in (\d+) seconds unless you tap I'm OK\.$""") to
            "Amaran akan dihantar dalam $1 saat melainkan anda tekan Saya OK.",
        Regex("""^Target (\d+): (.+)$""") to "Sasaran $1: $2",
        Regex("""^Couldn't connect Bluetooth: (.+)$""") to "Tidak dapat menyambung Bluetooth: $1",
        Regex("""^Connected to (.+)$""") to "Berjaya sambung ke $1",
        Regex("""^Guessed from your phone's movement and light, not the mmWave sensor$""") to
            "Dianggar daripada pergerakan dan cahaya telefon anda, bukan sensor mmWave",
        Regex("""^Guessed from (.+)'s phone's movement and light, not the mmWave sensor$""") to
            "Dianggar daripada pergerakan dan cahaya telefon $1, bukan sensor mmWave",
    )

    fun lookup(text: String): String {
        everything[text]?.let { return it }
        patterns.forEach { (pattern, replacement) ->
            if (pattern.matches(text)) return pattern.replace(text, replacement)
        }
        return text
    }
}
