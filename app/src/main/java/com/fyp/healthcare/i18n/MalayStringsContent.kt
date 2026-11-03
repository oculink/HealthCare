package com.fyp.healthcare.i18n

/**
 * Second half of the English -> Bahasa Melayu dictionary: medication, appointments, readings,
 * trends, reports, clinics and the emotional-support page. See [MalayStrings] for the house
 * style and the reason the map is keyed by the English string.
 */
internal object MalayStringsContent {

    val text: Map<String, String> = buildMap {
        // ---------- Medication ----------
        put("Medication", "Ubat")
        put("Medications", "Ubat-ubatan")
        put("Medication Name", "Nama Ubat")
        put("Medication Details", "Butiran Ubat")
        put("Medication Reminder", "Peringatan Ubat")
        put("Medication Reminders", "Peringatan Ubat")
        put("Medication History", "Sejarah Ubat")
        put("Add Medication", "Tambah Ubat")
        put("Add New Medication", "Tambah Ubat Baharu")
        put("+ Add Another Medication", "+ Tambah Ubat Lain")
        put("Edit Medication", "Sunting Ubat")
        put("Choose Medication", "Pilih Ubat")
        put("Choose medication", "Pilih ubat")
        put("Choose from medication list", "Pilih daripada senarai ubat")
        put("Search medications", "Cari ubat")
        put("Enter a medication name", "Masukkan nama ubat")
        put("Tell us about this medication", "Beritahu kami tentang ubat ini")
        put("Type your own · works offline and saves for next time",
            "Taip sendiri · berfungsi luar talian dan disimpan untuk kali seterusnya")
        put("Type your own name and an optional description",
            "Taip nama sendiri dan keterangan pilihan")
        put("How it's taken", "Cara pengambilan")
        put("Amount per consume", "Jumlah setiap pengambilan")
        put("Applications per consume", "Sapuan setiap pengambilan")
        put("Capsules / tablets per consume", "Kapsul / tablet setiap pengambilan")
        put("Drops per consume", "Titisan setiap pengambilan")
        put("Patches per consume", "Tampalan setiap pengambilan")
        put("Puffs per consume", "Sedutan setiap pengambilan")
        put("Sprays per consume", "Semburan setiap pengambilan")
        put("Suppositories per consume", "Suppositori setiap pengambilan")
        put("Tablets per consume", "Tablet setiap pengambilan")
        put("mL per injection", "mL setiap suntikan")
        put("Consumes per day", "Pengambilan sehari")
        put("How many times a day you take it. We'll space the reminders automatically.",
            "Berapa kali sehari anda mengambilnya. Kami akan jarakkan peringatan secara automatik.")
        put("How many you take at each reminder.",
            "Berapa banyak yang anda ambil pada setiap peringatan.")
        put("Reminder Times", "Masa Peringatan")
        put("When should we remind you?", "Bilakah kami patut mengingatkan anda?")
        put("Times are spaced automatically, then can be changed per day.",
            "Masa dijarakkan secara automatik, kemudian boleh diubah mengikut hari.")
        put("Tap a time below to fine-tune it.", "Ketik masa di bawah untuk melarasnya.")
        put("No reminder times set", "Tiada masa peringatan ditetapkan")
        put("No reminder this day", "Tiada peringatan pada hari ini")
        put("Choose a day", "Pilih hari")
        put("Every day", "Setiap hari")
        put("Weekdays", "Hari bekerja")
        put("Weekends", "Hujung minggu")
        put("On one day", "Pada satu hari")
        put("Any day", "Mana-mana hari")
        put("Turn on at least one day.", "Hidupkan sekurang-kurangnya satu hari.")
        put("once a day", "sekali sehari")
        put("Due now", "Tiba masanya")
        put("Taken", "Diambil")
        put("Missed", "Terlepas")
        put("Mark as taken?", "Tanda sebagai diambil?")
        put("Skip this dose?", "Langkau dos ini?")
        put("Take late", "Ambil lewat")
        put("Dose", "Dos")
        put("Doses", "Dos")
        put("logged doses", "dos dicatat")
        put("This removes the medication and cancels its reminder.",
            "Ini akan membuang ubat dan membatalkan peringatannya.")
        put("Back to Reminders", "Kembali ke Peringatan")
        put("Back to Dashboard", "Kembali ke Papan Pemuka")
        put("Medication Added", "Ubat Ditambah")
        put("Medication added", "Ubat ditambah")
        put("Medication Reminder", "Peringatan Ubat")
        put("Reminds you to take your medication on time",
            "Mengingatkan anda mengambil ubat tepat pada masanya")
        put("Nothing logged yet · doses marked Taken or Missed show here.",
            "Belum ada catatan · dos yang ditanda Diambil atau Terlepas akan muncul di sini.")
        put("Mark a dose Taken or Missed on the Medications page and it shows here, ",
            "Tanda dos Diambil atau Terlepas pada halaman Ubat dan ia akan muncul di sini, ")
        put("Medication and appointment reminders. Turning this off silences both ",
            "Peringatan ubat dan temu janji. Mematikannya akan menyenyapkan kedua-duanya ")
        put("Never", "Tidak pernah")
        put("Every", "Setiap")
        put("Choose type", "Pilih jenis")
        put("Change type", "Tukar jenis")
        put("How it's taken", "Cara pengambilan")
        put("I take it", "Saya ambil")
        put("Route", "Cara")
        put("Oral", "Mulut")
        put("Topical (cream / gel)", "Luaran (krim / gel)")
        put("Inhaled", "Sedutan")
        put("Injectable", "Suntikan")
        put("Nasal spray", "Semburan hidung")
        put("Eye / ear drops", "Titisan mata / telinga")
        put("Rectal / vaginal", "Rektum / faraj")
        put("Sublingual / buccal", "Bawah lidah / pipi")
        put("Transdermal patch", "Tampalan transdermal")

        // ---------- Appointments ----------
        put("Appointment Details", "Butiran Temu Janji")
        put("Add Appointment", "Tambah Temu Janji")
        put("Edit Appointment", "Sunting Temu Janji")
        put("Doctor / clinician", "Doktor / klinisi")
        put("Clinic / hospital (optional)", "Klinik / hospital (pilihan)")
        put("Specialty (optional)", "Pakar (pilihan)")
        put("Enter the doctor or clinic name", "Masukkan nama doktor atau klinik")
        put("Add an appointment you've booked · we'll remind you",
            "Tambah temu janji yang anda tempah · kami akan mengingatkan anda")
        put("Doctor, clinic, date and a reminder", "Doktor, klinik, tarikh dan peringatan")
        put("Pick a date and time in the future", "Pilih tarikh dan masa akan datang")
        put("Remind me", "Ingatkan saya")
        put("Reminder", "Peringatan")
        put("Reminders", "Peringatan")
        put("Upcoming", "Akan datang")
        put("Past & cancelled", "Lepas & dibatalkan")
        put("Attended", "Hadir")
        put("Cancelled", "Dibatalkan")
        put("Confirmed", "Disahkan")
        put("Cancel appointment", "Batalkan temu janji")
        put("Clinic", "Klinik")
        put("Doctor", "Doktor")
        put("Doctors", "Doktor")
        put("Hospital", "Hospital")
        put("Pharmacy", "Farmasi")
        put("Directions", "Arah")
        put("See map", "Lihat peta")
        put("Nothing coming up.", "Tiada apa-apa akan datang.")
        put("Appointment Reminders", "Peringatan Temu Janji")
        put("Reminds you about upcoming medical appointments",
            "Mengingatkan anda tentang temu janji perubatan yang akan datang")

        // ---------- Readings ----------
        put("Enter Today's Readings", "Masukkan Bacaan Hari Ini")
        put("Blood Pressure", "Tekanan Darah")
        put("Blood Sugar (mmol/L)", "Gula Darah (mmol/L)")
        put("Blood Sugar", "Gula Darah")
        put("Heart Rate", "Kadar Denyutan Jantung")
        put("Oxygen Level", "Paras Oksigen")
        put("Blood Pressure must look like 120/80 (top 70-250, bottom 40-150)",
            "Tekanan Darah mesti dalam bentuk 120/80 (atas 70-250, bawah 40-150)")
        put("Heart Rate must be a whole number between 20 and 250",
            "Kadar Denyutan Jantung mesti nombor bulat antara 20 dan 250")
        put("Blood Sugar must be between 1.1 and 33.3 mmol/L",
            "Gula Darah mesti antara 1.1 dan 33.3 mmol/L")
        put("Temperature must be a number between 30 and 45",
            "Suhu mesti nombor antara 30 dan 45")
        put("Oxygen Level must be a whole number between 50 and 100",
            "Paras Oksigen mesti nombor bulat antara 50 dan 100")
        put("Height must be 50-250 cm", "Tinggi mesti 50-250 cm")
        put("Weight must be 1-400 kg", "Berat mesti 1-400 kg")
        put("Saved with no values", "Disimpan tanpa nilai")
        put("Readings Saved", "Bacaan Disimpan")
        put("Your readings are saved and checked against the normal ranges.",
            "Bacaan anda disimpan dan disemak dengan julat normal.")
        put("Reading History", "Sejarah Bacaan")
        put("Filter readings", "Tapis bacaan")
        put("Show only these", "Tunjuk hanya ini")
        put("Show everything", "Tunjuk semua")
        put("Show everything again", "Tunjuk semua semula")
        put("Show every kind", "Tunjuk setiap jenis")
        put("No readings match this", "Tiada bacaan sepadan")
        put("Try another day or status.", "Cuba hari atau status lain.")
        put("Newer", "Baharu")
        put("Older", "Lama")
        put("Normal", "Normal")
        put("High", "Tinggi")
        put("Low", "Rendah")
        put("Critical", "Kritikal")
        put("Elevated", "Meningkat")
        put("Warning", "Amaran")
        put("Needs Attention", "Perlu Perhatian")
        put("All Good", "Semua Baik")
        put("Good", "Baik")
        put("Stable", "Stabil")
        put("Fair", "Sederhana")
        put("Poor", "Lemah")
        put("No urgent alerts at this time.", "Tiada amaran mendesak buat masa ini.")
        put("All readings logged in this period are within the normal range.",
            "Semua bacaan yang direkodkan dalam tempoh ini berada dalam julat normal.")
        put("A few readings sit outside the recommended range.",
            "Beberapa bacaan berada di luar julat disyorkan.")
        put("Some readings fall outside the recommended range · see Alerts.",
            "Beberapa bacaan berada di luar julat disyorkan · lihat Amaran.")
        put("No readings were recorded in this period.",
            "Tiada bacaan direkodkan dalam tempoh ini.")
        put("No vitals were logged in this period.",
            "Tiada tanda vital direkodkan dalam tempoh ini.")
        put("No readings in this month", "Tiada bacaan pada bulan ini")
        put("No data yet for this period", "Belum ada data untuk tempoh ini")
        put("No community data yet for this period", "Belum ada data komuniti untuk tempoh ini")
        put("Nothing recorded yet · take your first reading",
            "Belum ada rekod · ambil bacaan pertama anda")
        put("Readings you add on the Record Data page show up here, newest first.",
            "Bacaan yang anda tambah pada halaman Rekod Data akan muncul di sini, terbaharu dahulu.")
        put("Record readings from the home screen so the next report has a full picture.",
            "Rekod bacaan dari halaman utama supaya laporan seterusnya lengkap.")

        // ---------- Trends / analysis ----------
        put("Trends need a few more readings · keep recording",
            "Trend memerlukan beberapa bacaan lagi · terus merekod")
        put("Trends need a few more readings", "Trend memerlukan beberapa bacaan lagi")
        put("Record your vitals to build your own trend line",
            "Rekod tanda vital anda untuk membina garis trend anda sendiri")
        put("Record your vitals to see how your readings line up with the normal ranges.",
            "Rekod tanda vital anda untuk melihat bagaimana bacaan anda berbanding julat normal.")
        put("No readings to analyse yet.", "Belum ada bacaan untuk dianalisis.")
        put("Not enough to judge yet", "Belum cukup untuk dinilai")
        put("Analyze Health Data", "Analisis Data Kesihatan")
        put("Tap to see the analysis", "Ketik untuk melihat analisis")
        put("Your latest analysis", "Analisis terbaharu anda")
        put("Key Insights", "Pandangan Utama")
        put("HEALTH INSIGHTS", "PANDANGAN KESIHATAN")
        put("Health Insights", "Pandangan Kesihatan")
        put("Average Blood Pressure", "Purata Tekanan Darah")
        put("Average Body Temperature", "Purata Suhu Badan")
        put("Average Heart Rate", "Purata Kadar Denyutan Jantung")
        put("Average SpO₂", "Purata SpO₂")
        put("Averages appear as readings come in", "Purata akan muncul apabila bacaan masuk")
        put("Daily Activity Summary", "Ringkasan Aktiviti Harian")
        put("Hourly Activity", "Aktiviti Setiap Jam")
        put("Idle Time", "Masa Rehat")
        put("Movements", "Pergerakan")
        put("Movement", "Pergerakan")
        put("Falls", "Jatuh")
        put("Presence", "Kehadiran")
        put("Sleep Last Night", "Tidur Malam Tadi")
        put("No activity data yet", "Belum ada data aktiviti")
        put("Daily Readings Log", "Log Bacaan Harian")
        put("Daily Readings Log  ·  continued", "Log Bacaan Harian  ·  sambungan")
        put("Showing everyone's average · tap to see just yours",
            "Menunjukkan purata semua orang · ketik untuk lihat anda sahaja")
        put("Showing just your readings · tap to see everyone's average",
            "Menunjukkan bacaan anda sahaja · ketik untuk lihat purata semua orang")
        put("Showing your saved results · service unavailable",
            "Menunjukkan keputusan tersimpan anda · perkhidmatan tidak tersedia")
        put("you vs. everyone", "anda vs. semua orang")
        put("Week", "Minggu")
        put("Month", "Bulan")
        put("Day", "Hari")
        put("Su", "Ah")
        put("Mo", "Is")
        put("Tu", "Se")
        put("We", "Ra")
        put("Th", "Kh")
        put("Fr", "Ju")
        put("Sa", "Sa")
        put("Sun", "Ahd")
        put("Mon", "Isn")
        put("Tue", "Sel")
        put("Wed", "Rab")
        put("Thu", "Kha")
        put("Fri", "Jum")
        put("Sat", "Sab")
        put("Sunday", "Ahad")
        put("Monday", "Isnin")
        put("Tuesday", "Selasa")
        put("Wednesday", "Rabu")
        put("Thursday", "Khamis")
        put("Friday", "Jumaat")
        put("Saturday", "Sabtu")
        put("Morning", "Pagi")
        put("Afternoon", "Tengah hari")
        put("Evening", "Petang")
        put("Night", "Malam")

        // ---------- Reports ----------
        put("Generate Report", "Jana Laporan")
        put("Report Generated", "Laporan Dijana")
        put("Generated Reports", "Laporan Dijana")
        put("Report Period", "Tempoh Laporan")
        put("Report months", "Bulan laporan")
        put("Download PDF", "Muat Turun PDF")
        put("Share with Doctor", "Kongsi dengan Doktor")
        put("Share health report", "Kongsi laporan kesihatan")
        put("Share health reports", "Kongsi laporan kesihatan")
        put("Share Options", "Pilihan Kongsi")
        put("Save a copy to your device", "Simpan salinan dalam peranti anda")
        put("Each PDF covers a full calendar month. The current month runs to today.",
            "Setiap PDF merangkumi sebulan kalendar penuh. Bulan semasa berakhir hari ini.")
        put("One PDF for each calendar month", "Satu PDF untuk setiap bulan kalendar")
        put("Save each month as its own file", "Simpan setiap bulan sebagai fail berasingan")
        put("Save just this month's PDF", "Simpan PDF bulan ini sahaja")
        put("Send every monthly report together", "Hantar semua laporan bulanan sekali gus")
        put("Send just this month's PDF", "Hantar PDF bulan ini sahaja")
        put("Send the PDF by email or another app", "Hantar PDF melalui e-mel atau aplikasi lain")
        put("Couldn't save the report", "Tidak dapat menyimpan laporan")
        put("Couldn't save the reports", "Tidak dapat menyimpan laporan")
        put("Couldn't share the report", "Tidak dapat berkongsi laporan")
        put("Couldn't share the reports", "Tidak dapat berkongsi laporan")
        put("For Medical Professional", "Untuk Profesional Perubatan")
        put("Patient Health Report", "Laporan Kesihatan Pesakit")
        put("Patient ID", "ID Pesakit")
        put("Patient Name", "Nama Pesakit")
        put("Generated by CareRadar. This report isn't a substitute for professional medical advice.",
            "Dijana oleh CareRadar. Laporan ini bukan pengganti nasihat perubatan profesional.")
        put("Tap a month to preview it", "Ketik bulan untuk pratonton")
        put("End of preview", "Akhir pratonton")
        put("Regenerate", "Jana semula")
        put("Health Metrics Summary (Monthly Average)", "Ringkasan Metrik Kesihatan (Purata Bulanan)")
        put("Health Summary Graph", "Graf Ringkasan Kesihatan")
        put("Monthly Summary", "Ringkasan Bulanan")
        put("Prepared for", "Disediakan untuk")
        put("Your Records", "Rekod Anda")
        put("Report", "Laporan")
        put("Page", "Halaman")
        put("Save report", "Simpan laporan")
        put("Save as PDF", "Simpan sebagai PDF")
        put("Your name", "Nama anda")

        // ---------- Clinics / map ----------
        put("Search an address…", "Cari alamat…")
        put("Search an address...", "Cari alamat...")
        put("Type an address…", "Taip alamat…")
        put("Type an address or use the map", "Taip alamat atau guna peta")
        put("Search this area", "Cari kawasan ini")
        put("Your location", "Lokasi anda")
        put("My location", "Lokasi saya")
        put("Picked location", "Lokasi dipilih")
        put("Recenter", "Pusat semula")
        put("Choose location", "Pilih lokasi")
        put("Tap the map to drop a pin", "Ketik peta untuk meletakkan pin")
        put("Tap the map, or change the address above", "Ketik peta, atau ubah alamat di atas")
        put("Tap the map, search above, or use where you are now",
            "Ketik peta, cari di atas, atau guna lokasi anda sekarang")
        put("Turn on Location access · we'll find clinics near you",
            "Hidupkan akses Lokasi · kami akan cari klinik berdekatan anda")
        put("Couldn't find you · turn on Location, then try again",
            "Tidak dapat menemui anda · hidupkan Lokasi, kemudian cuba lagi")
        put("Couldn't load places · check your connection, then retry",
            "Tidak dapat memuatkan tempat · semak sambungan anda, kemudian cuba lagi")
        put("The map is taking a while · hang on", "Peta mengambil masa · harap tunggu")
        put("map service unavailable", "perkhidmatan peta tidak tersedia")
        put("Still searching for more…", "Masih mencari lagi…")
        put("Healthcare", "Penjagaan Kesihatan")

        // ---------- Emotional support ----------
        put("Show me another", "Tunjuk yang lain")
        put("Choose which pictures to see", "Pilih gambar yang ingin dilihat")
        put("Pictures you'd like to see", "Gambar yang anda ingin lihat")
        put("Animals", "Haiwan")
        put("Flowers", "Bunga")
        put("Greenery", "Hijauan")
        put("Tick the kinds you like seeing. Leave them all empty to see every kind.",
            "Tanda jenis yang anda suka lihat. Biarkan kosong untuk melihat semua jenis.")
        put("Photo credits", "Kredit foto")
        put("Photos and the people who shared them", "Foto dan individu yang berkongsinya")
        put("A little rest now is a favour to tomorrow.",
            "Rehat sekejap sekarang ialah hadiah untuk esok.")
        put("Being here is enough. It really is.", "Kehadiran anda sudah cukup. Benar-benar cukup.")
        put("It's alright to ask for company.", "Tidak mengapa untuk meminta ditemani.")
        put("Let yourself enjoy something small.", "Benarkan diri anda menikmati perkara kecil.")
        put("Look at something green for a moment.", "Pandang sesuatu yang hijau sebentar.")
        put("Nobody is too old for a good afternoon.",
            "Tiada siapa terlalu tua untuk petang yang indah.")
        put("No one has to be impressive on a Tuesday.",
            "Tiada siapa perlu tampil hebat pada hari Selasa.")
        put("Rest isn't something you have to earn.", "Rehat bukan sesuatu yang perlu diusahakan.")
        put("Say the kind thing you'd say to a friend.",
            "Ucapkan kata-kata baik yang biasa anda ucapkan kepada kawan.")
        put("Some days the bravest thing is breakfast.",
            "Ada hari, perkara paling berani ialah bersarapan.")
        put("Someone remembers a kindness you showed them.",
            "Seseorang masih ingat kebaikan yang anda tunjukkan.")
        put("Someone, somewhere, is glad you're in the world.",
            "Seseorang, di suatu tempat, gembira anda ada di dunia ini.")
        put("Take the sunny chair. It's yours.", "Duduklah di kerusi yang cerah. Itu untuk anda.")
        put("The afternoon is yours. No one else's.", "Petang ini milik anda. Bukan orang lain.")
        put("There's no prize for pushing through.", "Tiada hadiah kerana memaksa diri.")
        put("Today is a good day to go easy on yourself.",
            "Hari ini hari yang baik untuk bersikap lembut pada diri sendiri.")
        put("You don't have to earn a nice day.", "Anda tidak perlu berusaha untuk hari yang indah.")
        put("You matter to people who don't say it often enough.",
            "Anda penting bagi orang yang jarang meluahkannya.")
        put("You showed up today. That counts.", "Anda hadir hari ini. Itu sudah bermakna.")
        put("You won't get a reminder.", "Anda tidak akan menerima peringatan.")
        put("You're allowed to have a slow day.", "Anda dibenarkan melalui hari yang perlahan.")
        put("You're doing better than you think.", "Anda lebih baik daripada yang anda sangka.")
        put("You're more than your worst week.", "Anda lebih daripada minggu terburuk anda.")
        put("You've gotten through hard days before · this one will pass too.",
            "Anda pernah melalui hari-hari sukar · hari ini juga akan berlalu.")
        put("You've outlasted every hard day so far.",
            "Anda telah melepasi setiap hari sukar setakat ini.")
        put("Your story still has good pages left.", "Kisah anda masih ada halaman indah.")
        put("Call the person you keep meaning to call.",
            "Telefon orang yang selalu anda niatkan untuk telefon.")
        put("A little rest now is a favour to tomorrow.",
            "Rehat sekejap sekarang ialah hadiah untuk esok.")
        put("Nothing wrong with a cup of tea and no plans.",
            "Tiada salahnya dengan secawan teh dan tanpa rancangan.")
        put("This page is a small lift, not counselling. If things ever feel unsafe or too ",
            "Halaman ini sekadar penyeri hari, bukan kaunseling. Jika keadaan terasa tidak selamat atau terlalu ")
        put("heavy, the Emergency page has the numbers that help.",
            "berat, halaman Kecemasan ada nombor yang boleh membantu.")

        // ---------- Misc ----------
        put("Analysis", "Analisis")
        put("Needs update", "Perlu dikemas kini")
        put("Last sync", "Segerak terakhir")
        put("just now", "baru sahaja")
        put("No activity data yet", "Belum ada data aktiviti")
        put("Waiting for radar step data", "Menunggu data langkah radar")
        put("Manual entry", "Kemasukan manual")
        put("From a connected wearable", "Daripada peranti boleh pakai yang disambungkan")
        put("Measured by the mmWave sensor", "Diukur oleh sensor mmWave")
        put("Phone estimate, not the mmWave sensor", "Anggaran telefon, bukan sensor mmWave")
        put("Live Radar Tracking", "Pemantauan Radar Langsung")
        put("Start Live Tracking", "Mula Pemantauan Langsung")
        put("Bluetooth Test — Connect ESP32", "Ujian Bluetooth — Sambung ESP32")
        put("Connect ESP32", "Sambung ESP32")
        put("Disconnect", "Putuskan")
        put("Connections", "Sambungan")
        put("Family Code", "Kod Keluarga")
        put("Search conditions, like asthma or diabetes", "Cari keadaan, seperti asma atau diabetes")
        put("Search foods, pollens, medicines…", "Cari makanan, debunga, ubat…")
        put("Add a condition", "Tambah keadaan")
        put("Add a condition not listed", "Tambah keadaan yang tiada dalam senarai")
        put("Add a medication not listed", "Tambah ubat yang tiada dalam senarai")
        put("Add an allergy", "Tambah alahan")
        put("Add an allergy not listed", "Tambah alahan yang tiada dalam senarai")
        put("Add another allergy", "Tambah alahan lain")
        put("Add another condition", "Tambah keadaan lain")
        put("Add this allergy", "Tambah alahan ini")
        put("Add this condition", "Tambah keadaan ini")
        put("Choose Allergen", "Pilih Alergen")
        put("Choose Condition", "Pilih Keadaan")
        put("Already added", "Sudah ditambah")
        put("add something not listed", "tambah sesuatu yang tiada dalam senarai")
        put("Leave them all unticked for any condition.",
            "Biarkan semuanya tidak bertanda untuk sebarang keadaan.")
        put("Leave them all unticked to see every measurement.",
            "Biarkan semuanya tidak bertanda untuk melihat setiap ukuran.")
        put("The Emergency Profile screen will use these.",
            "Halaman Profil Kecemasan akan menggunakan maklumat ini.")
        put("This helps CareApp and your Emergency Profile. You can change all of it later in Settings.",
            "Ini membantu CareApp dan Profil Kecemasan anda. Anda boleh mengubah semuanya kemudian dalam Tetapan.")
        put("Add your date of birth, gender, height and weight. ",
            "Tambah tarikh lahir, jantina, tinggi dan berat anda. ")
        put("Changes here also update your Emergency Profile.",
            "Perubahan di sini juga mengemas kini Profil Kecemasan anda.")
        put("Saved securely and analyzed for you", "Disimpan dengan selamat dan dianalisis untuk anda")
        put("Your account isn't protected yet · tap to link Google so you can't get locked out",
            "Akaun anda belum dilindungi · ketik untuk pautkan Google agar anda tidak terkunci")
        put("CareApp can't email a reset link to a username account.",
            "CareApp tidak boleh menghantar pautan set semula ke akaun nama pengguna.")
        put("That Google account already has its own CareApp account. Sign in with Google instead, or connect a Google account that's never been used here.",
            "Akaun Google itu sudah mempunyai akaun CareApp sendiri. Log masuk dengan Google, atau sambungkan akaun Google yang belum pernah digunakan di sini.")
        put("This erases the profile, readings, medications and appointments for ",
            "Ini akan memadam profil, bacaan, ubat dan temu janji bagi ")
        put("Any caregiver linked to it stops seeing this account. Reports you've already saved or shared stay on the devices holding them.",
            "Mana-mana penjaga yang dipautkan akan berhenti melihat akaun ini. Laporan yang telah disimpan atau dikongsi kekal dalam peranti masing-masing.")
        put("They'll lose access to your account right away.",
            "Mereka akan kehilangan akses ke akaun anda serta-merta.")
        put("You'll get a new code. Caregivers already linked stay linked, but the old code stops working for new links.",
            "Anda akan mendapat kod baharu. Penjaga yang sudah dipautkan kekal, tetapi kod lama tidak lagi berfungsi untuk pautan baharu.")
        put("Reset code", "Set semula kod")
        put("Reset your code?", "Set semula kod anda?")
        put("Couldn't load your code. Check your connection.", "Tidak dapat memuatkan kod anda. Semak sambungan anda.")
        put("You'll stop managing their account and return to your own.",
            "Anda akan berhenti mengurus akaun mereka dan kembali ke akaun sendiri.")
        put("Unlink from this patient?", "Nyahpaut daripada pesakit ini?")
        put("You're not signed in, so there's nothing to connect.",
            "Anda belum log masuk, jadi tiada apa untuk disambungkan.")
        put("You're not signed in.", "Anda belum log masuk.")
        put("Couldn't connect Google. Please try again.", "Tidak dapat menyambung Google. Sila cuba lagi.")
        put("No Google account on this device. Add one in Settings, then try again.",
            "Tiada akaun Google dalam peranti ini. Tambah satu dalam Tetapan, kemudian cuba lagi.")
        put("Waiting for Google…", "Menunggu Google…")
        put("For your security, sign out and back in, then try deleting the account again.",
            "Demi keselamatan anda, log keluar dan log masuk semula, kemudian cuba padam akaun sekali lagi.")
        put("This account has no username on file.", "Akaun ini tiada nama pengguna dalam rekod.")
        put("This account has no username on file to confirm.", "Akaun ini tiada nama pengguna untuk disahkan.")
        put("This account signs in with Google, so there's no CareApp password to change.",
            "Akaun ini log masuk dengan Google, jadi tiada kata laluan CareApp untuk ditukar.")
        put("Unexpected credential type", "Jenis kelayakan tidak dijangka")
        put("Back to top", "Kembali ke atas")

        // ---------- Emotional support quotes and photo captions ----------
        put("Small steps are still steps.", "Langkah kecil tetap satu langkah.")
        put("This page is a small lift, not counselling. If things ever feel unsafe or too heavy, the Emergency page has the numbers that help.",
            "Halaman ini sekadar penyeri hari, bukan kaunseling. Jika keadaan terasa tidak selamat atau terlalu berat, halaman Kecemasan ada nombor yang boleh membantu.")
        put("A grey cat sitting in green grass", "Kucing kelabu duduk di atas rumput hijau")
        put("A calico cat resting on a stone step", "Kucing belang tiga warna berehat di anak tangga batu")
        put("Two ginger kittens playing in the grass", "Dua anak kucing oren bermain di atas rumput")
        put("A golden dog resting beside a garden cart", "Anjing keemasan berehat di sisi pedati taman")
        put("An older woman smiling with her golden retriever", "Wanita warga emas tersenyum bersama golden retrievernya")
        put("A German shepherd puppy carrying a stick", "Anak anjing gembala Jerman membawa sebatang kayu")
        put("A beagle puppy sitting on the lawn", "Anak anjing beagle duduk di atas rumput")
        put("A grey rabbit grazing in the grass", "Arnab kelabu meragut rumput")
        put("A white and black rabbit beside a wooden bowl", "Arnab putih dan hitam di sisi mangkuk kayu")
        put("A sunflower field at sunset", "Ladang bunga matahari ketika matahari terbenam")
        put("A field of sunflowers under a pale sky", "Ladang bunga matahari di bawah langit pucat")
        put("A single red lily among green leaves", "Sekuntum bunga lili merah di celah daun hijau")
        put("Pink roses in bloom in a garden", "Bunga ros merah jambu sedang mekar di taman")
        put("A meadow covered in white daisies", "Padang rumput dipenuhi bunga daisi putih")
        put("Green ferns growing in a shaded forest", "Paku pakis hijau tumbuh di hutan yang redup")
        put("A carpet of soft green foliage", "Hamparan dedaunan hijau yang lembut")
        put("A path leading through a tall bamboo grove", "Laluan menembusi rimbunan buluh tinggi")
        put("Sunlight falling on moss covering the forest floor", "Cahaya matahari jatuh ke atas lumut di lantai hutan")
        put("A misty green meadow lined with trees", "Padang rumput hijau berkabus di tepi pokok-pokok")
    }
}
