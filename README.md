# Penboard

Papan tulis stylus bergaya **HiWrite** untuk Android, dengan tema **retro cyberpunk grayscale ePaper**. Tulis dengan Huawei M-Pen (atau stylus apa pun) di papan tulis, lalu tulisan itu berubah menjadi teks dan langsung masuk ke kolom isian aplikasi yang sedang dipakai. Bukan keyboard QWERTY: papan tulisnya lebar di kiri, tombolnya berupa **ikon** dan tersusun rapi 3 kolom di kanan.

Versi terbaru: **3.5.4** (lihat [Releases](https://github.com/retrobit-dev/RetroBit-Penboard/releases) untuk berkas APK).

## Yang baru di 3.5.4

- **Batas tombol atas 5 → 30.** Batas 5 terlalu ketat: ukuran tombol atas mengikuti tombol
  samping, jadi dengan tombol samping **32 dp** satu baris papan bisa memuat **sekitar 10
  tombol** — 20 tombol cukup dua baris. Dengan tombol 96 dp hanya sekitar 3 per baris.
- **Jumlah yang muat dihitung dari geometri**, bukan angka tetap: baris dibungkus dengan
  jarak yang sama seperti tombol samping (`GridMath.bottomRowIndexes`), tetap **rata kiri**.
- **Bawaan bilah atas kembali 8 aksi** (A+, enter, hapus goresan, bersihkan papan, batal,
  ulangi, ganti warna, sembunyikan) supaya slot ke-6–30 tidak kosong; slot di atasnya memakai
  daftar itu secara bergiliran.
- Pratinjau `docs/preview.png` contoh ke-9 kini menampilkan **20 tombol atas** dengan tombol
  samping 32 dp.

## Yang baru di 3.5.3

- **Tombol atas bersebelahan dengan jarak yang sama seperti tombol samping.** Jarak antar
  tombol tidak lagi dilebarkan untuk memenuhi lebar papan; tombolnya **rata kiri**, jadi
  susunannya terlihat sama rapi dengan kolom tombol di kanan.
- **Jumlah tombol atas maksimal 5** saat itu (sebelumnya 16); batas ini dinaikkan lagi
  menjadi **30** di 3.5.4 karena ukuran tombol menentukan berapa yang muat.
- Akibatnya panel lebih ringkas: bilah atas tidak lagi mengambil banyak tinggi, dan area
  tulis tetap lega.
- Pratinjau `docs/preview.png` contoh ke-9 diperbarui: **5 tombol atas rata kiri** dengan
  jarak seragam.

## Yang baru di 3.5.2

- **Baris tombol samping bisa diatur sendiri** (*Area tombol → Baris tombol mengikuti area
  tulis*, matikan lalu geser *Jumlah baris tombol samping*, 1–6). Bawaannya tetap **mengikuti
  baris area tulis**; kalau dimatikan, 3 baris tulisan bisa punya 5 baris tombol, misalnya
  3 kolom x 5 baris = 15 tombol.
- **Kolom tetap 1–6**: tambah/kurangi kolom seperti sebelumnya, dan pemangkasan jumlah tombol
  otomatis mengikuti (kolom x baris).
- **Ukuran tombol otomatis = tinggi satu baris area tulis**, berapa pun baris tombolnya. Jadi
  menambah baris tombol menambah tinggi kolom tombol, tidak mengecilkan tombolnya. Ukuran
  sendiri tetap 32–160 dp dan tetap dibatasi lebar/tinggi supaya tidak terpotong.
- Kartu pratinjau menampilkan jumlah baris yang dipakai; bila baris diatur sendiri, teksnya
  *"✓ 5 baris tombol samping diatur sendiri, area tulis 3 baris (rata atas)"*.
- Pratinjau `docs/preview.png` kini berisi **10 contoh**, termasuk 3 kolom x 5 baris tombol
  dengan area tulis 3 baris.

## Yang baru di 3.5.1

- **Kolom tombol samping tidak lagi terpotong.** Panel sekarang diukur dari **lebar area
  keyboard yang sebenarnya** (bukan lebar layar), lalu sisi tombol dibatasi oleh sisa lebar
  setelah area tulis. Dengan 3 kolom, ketiga kolom selalu tampil utuh — termasuk saat ukuran
  tombol diatur manual besar (mis. 96 dp) atau papan dipersempit.
- **Pengaturan baru: lebar area tulis** (*Jendela papan → Lebar area tulis*, 40–80 %).
  Kolom tombol mengambil sisa ruangnya; menaikkan angka ini membuat area menulis lebih lebar
  dan tombol otomatis mengecil secukupnya. Nilai ini juga menjadi batas aman supaya tombol
  manual tidak menekan area tulis sampai keluar panel (`GridMath.boardWidthFor`,
  `GridMath.maxCellByWidthPx`, diuji unit).
- **Tombol atas bisa ditambah sampai 16** (sebelumnya 8); batas ini kemudian diubah menjadi
  **5 tombol** di 3.5.3 dengan jarak seragam seperti tombol samping.
- **Papan tetap utuh walau tombol atas banyak.** Bila bilah atas tidak muat, bilahnya
  mengecil bertahap (sampai 40 %) sebelum area tulis dikorbankan; kalau tetap tidak muat,
  ukuran tombol samping dibatasi ruang yang tersisa. Jumlah baris bilah dihitung ulang dengan
  ukuran tombol final, jadi tinggi panel tidak pernah "menipu" (`BoardPanel.applyConfig`).
- **Label di pengaturan mengikuti posisi barunya**: kartu "Tombol atas", *Jumlah tombol atas*,
  *Tampilkan bilah tombol atas*.

## Yang baru di 3.5.0

- **Area tulis 3 baris secara bawaan.** Tinggi papan = jumlah baris x jarak baris, jadi 3 baris
  = 3 x jarak baris panduan. Barisnya bisa ditambah (sampai 6) di *Papan tulis → Jumlah baris
  area tulis*; tinggi papan ikut menyesuaikan.
- **Baris tombol samping selalu sama dengan baris area tulis.** Tidak ada lagi pengaturan baris
  tombol yang terpisah: 3 baris tulisan = 3 baris tombol, 6 baris tulisan = 6 baris tombol.
- **Tombol samping sejajar atas saja.** Kolom tombol dirata-ataskan dengan tepi atas area tulis;
  kalau ukurannya lebih kecil dari papan, bagian bawahnya boleh kosong (tidak dipaksa penuh).
- **Tombol bawah dipindah ke ATAS area tulis**, masih di dalam bingkai jendela, tepat di bawah
  bilah judul.
- **Semua tombol bisa diatur ukurannya**: sisi tombol samping (otomatis atau 32–160 dp), serta
  **lebar dan tinggi tiap tombol bawah** (0,6x–4x lebar, 0,6x–2x tinggi dari ukuran tombol
  samping). Ikon menyesuaikan ukuran tombolnya.
- Jarak baris dipangkas otomatis bila ruang keyboard tidak cukup untuk semua baris area tulis,
  dan ukuran tombol otomatis dibatasi sisa ruang itu — tidak ada yang terpotong.

## Yang baru di 3.4.1

- **Perbaikan force close (aplikasi langsung berhenti).** Sejak 3.3.0 tombol grid dipakai ulang
  setiap pengaturan diterapkan, tetapi belum dilepas dari induk lamanya. Android tidak
  mengizinkan satu `View` punya dua induk, jadi `addView` melempar
  `IllegalStateException: The specified child already has a parent` dan aplikasi berhenti.
  Pemicunya bukan satu tombol tertentu: pengaturan diterapkan lebih dari sekali saat layar
  pengaturan dibuka, saat satu pengaturan diubah, dan saat keyboard muncul (tingginya diukur
  lalu pengaturan diterapkan ulang) — karena itu terasa "selalu" force close.
- **Perbaikannya**: tombol grid **dan** tombol bawah sekarang dilepas dulu dari baris lamanya
  sebelum disusun ulang (`removeView` sebelum `addView`).
- **Penjaga regresi**: `tools/check_source.py` punya aturan baru
  `ADDVIEW TANPA REMOVEVIEW` yang menolak view daftar yang dipasang ulang tanpa `removeView`
  lebih dulu. Aturan ini sudah diuji dengan meniru bug lama: salinan yang rusak membuat
  pemeriksa gagal (`exit 1`), versi yang benar lolos.
- Tidak ada perubahan tampilan atau perilaku lain: semua fitur 3.4.0 (baris tulisan = posisi
  teks, nomor baris) tetap sama.

## Yang baru di 3.4.0

- **Baris tempat menulis menentukan posisi teks di kolom isian.** Menulis di **baris pertama**
  papan → teks masuk **tepat di posisi kursor**. Menulis di **baris kedua** → satu baris **di
  bawah** kursor (sama seperti menekan **enter sekali**). Menulis di **baris ketiga** → dua
  baris di bawah kursor (seperti **enter dua kali**), dan seterusnya. Bisa dimatikan di
  *Papan tulis → Posisi teks mengikuti baris tulisan*.
- Barisnya dibaca dari **nilai tengah (`median`) semua titik goresan**, bukan dari ekor huruf —
  jadi huruf seperti "g" atau "y" yang turun ke baris berikutnya tidak salah menaikkan posisi
  (`BoardMath.medianY` + `BoardMath.lineIndexOf`, diuji unit).
- **Nomor baris tercetak tipis di tepi kiri papan** (1, 2, 3, …) supaya kelihatan baris mana yang
  sedang dipakai; nomornya ikut hilang bila *garis panduan* dimatikan.
- Agar tidak membingungkan, **enter hanya dikirim ke kolom isian yang memang bisa multi-baris**.
  Di kolom satu baris (mis. kotak pencarian), baris tulisan tidak menggeser apa pun dan teksnya
  tetap dikirim seperti biasa.
- Lencana hasil di papan ikut menyebut posisinya: `↵ ✓ kata` berarti teks itu turun satu baris
  dari kursor.
- Batas percobaan aman: paling banyak 8 baris (`BoardMath.MAX_LINE_OFFSET`) supaya goresan nyasar
  tidak mengirim deretan enter panjang.

## Yang baru di 3.3.0

- **Papan bisa dipindah ke mana saja**: tarik **bilah judul** di atas papan, persis seperti
  memindahkan jendela melayang. Ke kiri/kanan bebas, dan menariknya ke atas **mengangkat**
  papan dari bawah layar — area keyboard ikut meninggi (ruang gerak di bawah papan), lalu
  mengecil lagi saat papan diturunkan. Tarikan selalu dibatasi supaya papan tidak keluar
  dari area keyboard (`GridMath.clampOffset`), dan posisinya disimpan, jadi tetap di situ
  saat keyboard muncul lagi. Bisa dimatikan di *Jendela papan → Papan bisa dipindah*.
- **Ukuran papan diubah dengan menarik sudut kanan bawah**: ke kanan/kiri melebarkan atau
  menyempitkan papan (area tulis ikut), ke atas/bawah menambah atau mengurangi tinggi papan.
  Sudut itu digambar sebagai tiga garis pixel art supaya jelas bisa ditarik.
- **Semua tombol samping persegi: panjang = lebar.** Tinggi area tulis mengikuti tombol:
  `tinggi papan = jumlah baris x sisi tombol + jarak antar baris` (`GridMath.boardHeightPx`) —
  jadi papan dan kolom tombol selalu sejajar atas-bawah, berapa pun ukuran tombolnya.
- **Ukuran sisi tombol bisa diatur**: *Tombol papan → Ukuran tombol otomatis* (bawaan, dihitung
  dari tinggi papan & lebar yang tersedia) atau disetel sendiri 32–160 dp. Area tulis ikut
  membesar/mengecil mengikuti pilihan itu.
- **Kolom dan baris tombol samping dua-duanya bisa diatur**: 1–6 kolom dan 1–6 baris (sampai
  **36 tombol**, dari sebelumnya 3 x 6 = 18). Tombol bawaan untuk kolom 4–6 sudah disiapkan
  (pilih semua, potong, salin, tempel, kursor ke awal/akhir baris, dan lain-lain).
- **Tombol bawah: tingginya selalu sama dengan tombol samping**, lebarnya bisa diatur sendiri
  per tombol lewat *Lebar tombol* (1x = persegi, sampai 4x) di editor tiap tombol, dan tetap
  berada di dalam bingkai jendela tepat di bawah area menulis.
- Kartu *Pratinjau* sekarang menampilkan dua baris bukti terukur: **sisi tombol** yang benar-benar
  dipakai (mis. *"✓ Tombol persegi 108 px: 3 kolom x 3 baris"*) dan **penjajaran kolom tombol**
  dengan area tulis, plus versi aplikasi dan gaya ikon yang aktif.

## Yang baru di 3.2.3

- **Penjajaran tombol samping sekarang dikunci oleh tata letak, bukan hitungan angka.** Baris-baris tombol memakai `weight` di dalam kolom yang tingginya **sama dengan tinggi papan**, dan tombol di tiap baris memakai `match_parent`. Artinya berapa pun tinggi papan yang akhirnya terukur (termasuk saat dipangkas ruang keyboard), baris atas selalu rata dengan tepi atas papan dan baris bawah rata dengan tepi bawahnya — tidak ada celah sisa.
- **Bukti terukur di layar**: kartu *Pratinjau* menampilkan baris seperti *"✓ Kolom tombol sejajar area tulis: 550 px = 550 px"* (dibaca dari tinggi papan dan tinggi kolom tombol yang benar-benar terpasang), plus **gaya ikon yang sedang dipakai**. Kartu itu juga mencantumkan **versi aplikasi**, jadi mudah memastikan APK mana yang terpasang.
- Bila baris itu menampilkan *⚠ belum sejajar*, angkanya bisa langsung dilaporkan.

## Yang baru di 3.2.2

- **Tombol samping kanan kini benar-benar sejajar dengan area tulis** — atas dan bawahnya rata. Sebelumnya kolom tombol dipusatkan di tengah sehingga tepinya tidak sama dengan tepi papan.
- Tinggi tiap baris tombol dihitung ulang supaya **jumlah tinggi baris + jarak antar baris tepat sama dengan tinggi papan** (`GridMath.rowHeightsPx`; sisa pembagian dibagikan ke baris teratas, diuji unit). Jadi tidak ada celah sisa di bawah, juga saat tingginya tidak habis dibagi (mis. 6 baris di papan 242 px).
- Tombol samping tetap memakai **ikon pixel art** 12 x 12 (bawaan sejak 3.2.0) — sama seperti tombol bawah dan tombol minimize. Kalau di perangkat Anda ikonnya masih bergaris halus, cek kartu **Tampilan → Gaya ikon tombol** dan pilih **Pixel art**.
- Tombol bawah memakai **tinggi baris yang sama** dengan tombol samping, jadi keduanya benar-benar sebangun.

## Yang baru di 3.2.1

- **Tombol minimize tidak lagi ikut hilang saat bingkai jendela dimatikan.** Kalau *Bingkai jendela* dimatikan tetapi *Tombol minimize* menyala, tombolnya tetap ada sebagai **bilah kecil 18 dp di kanan atas papan** (tanpa judul, tanpa garis pemisah, dan tanpa mengubah tinggi papan). Kalau keduanya dimatikan, baris atas benar-benar hilang.
- Tinggi papan menyesuaikan: bilah judul penuh 22 dp, bilah kecil 18 dp, atau 0 — hitungannya di `PenboardConfig.titleBarHeightDp()`/`showsTitleBar` (diuji unit).

## Yang baru di 3.2.0

- **Satu gaya ikon untuk seluruh papan.** Sebelumnya hanya bilah bawah yang berikon pixel art; sekarang **tombol samping (grid), tombol bawah, dan tombol minimize** semuanya bisa memakai pixel art 12 x 12 — bawaan baru.
- **Gaya ikon bisa dipilih** di kartu *Tampilan*: **Pixel art** (bawaan) atau **Vektor** (ikon bergaris halus seperti versi 3.1). Pilihannya disimpan bersama pengaturan lain, dan berkas pengaturan lama tetap terbaca sebagai pixel art.
- Preset **HiWrite** menyalakan gaya pixel art sekaligus.

## Yang baru di 3.1.2

- **Tombol bawah bawaan semuanya benar-benar bekerja.** Tombol "teks kosong" yang tidak melakukan apa-apa diganti **enter** — sekarang bawaan bilah bawah = **A+ (jadikan teks), enter, hapus goresan, bersihkan papan, batal**. Ada tesnya: tidak boleh ada tombol bawah bawaan yang aksinya kosong.
- **Papan tulis tidak lagi terpotong di layar kecil.** Bila ruang yang disediakan keyboard lebih pendek dari tinggi papan + bilah judul + tombol bawah, papan dipangkas otomatis sampai semuanya muat (hitungan di `GridMath.fitBoardHeightPx`, diuji unit). Perilakunya berlaku saat keyboard muncul dan saat ukurannya berubah, termasuk saat orientasi layar berputar.

## Yang baru di 3.1.1

- **Tombol bawah duduk rapi di dalam bingkai**, tepat di bawah area menulis (bukan lagi bilah yang melebar ke seluruh panel). Papan tulis dan tombol bawahnya sekarang satu kolom di dalam bingkai jendela.
- **Ukurannya sama persis dengan tombol samping** (tinggi papan ÷ jumlah baris grid), jadi satu baris tombol bawah terlihat sejajar dengan satu baris tombol kanan.
- Bila satu baris tidak cukup, tombolnya **mengalir ke baris berikutnya** di bawah papan tulis (hitungannya di `GridMath.columnsFor` / `GridMath.wrapRows`, diuji unit).

## Yang baru di 3.1.0

**Papan tulis sekarang berbingkai seperti jendela, dan punya tombol minimize.**

- **Bingkai jendela**: garis tepi lebih tegas, **bilah judul** di atas papan (judulnya bisa diganti, bawaan `PENBOARD`), dan **tombol minimize** di kanan bilah judul. Sekali ketuk minimize, papan langsung disembunyikan (keyboard ditutup) dan layar jadi lega; sentuh kolom isian kalau ingin memunculkannya lagi.
- **Tombol di bawah papan tulis**: tempat menaruh tombol yang bisa diatur — jumlahnya 0–8, tiap tombol bebas dipilih aksinya (dan bisa diberi label serta tahan-untuk-mengulang). Ukurannya sama dengan tombol samping dan mengalir ke baris berikutnya bila tidak muat. Bawaannya: A+ (jadikan teks), enter, hapus goresan, bersihkan papan, batal.
- **Ikonnya pixel art asli**: kanvas 12 x 12 piksel yang digambar kotak demi kotak tanpa anti-aliasing (`util/PixelArt.kt`), bukan ikon vektor yang diperkecil. Semua aksi punya ikonnya sendiri — termasuk tombol minimize.
- Preset **HiWrite** menyalakan bingkai jendela, tombol minimize, dan 5 tombol bawah sekaligus. Keduanya bisa dimatikan di kartu **Jendela papan** dan **Tombol bawah**.

## Yang baru di 3.0.9

**Papan melayang dihapus.** Papan tulis utama tetap bisa dipakai di atas aplikasi lain — sekarang lewat satu jalan saja, yaitu sebagai **keyboard**: pilih Penboard sebagai keyboard, dan panelnya menempel di bawah layar aplikasi apa pun, tanpa izin *tampil di atas aplikasi lain*, tanpa foreground service, dan tanpa notifikasi.

- Papan melayang yang lama tidak hilang fungsinya: yang dihapus hanya jendela kedua yang harus digeser-geser sendiri.
- Ikon **dua jendela** pada grid (dulu untuk menyalakan papan melayang) diganti ikon **papan tombol kecil**: sekali ketuk membuka pemilih keyboard Android, jadi Anda bisa pindah ke tulisan tangan Huawei/Celia dan kembali ke Penboard dengan cepat.
- Layanan aksesibilitas **Penboard Tulis Otomatis** tetap ada sebagai cadangan (kartu **Keyboard Penboard** → Aktifkan Tulis Otomatis) untuk saat keyboard yang aktif bukan Penboard.
- Pengaturan **"Sembunyikan keyboard sistem"** ikut dihapus — dulu hanya perlu karena ada dua papan sekaligus.
- Pratinjau di halaman pengaturan tidak lagi punya bilah pegangan.

## Yang baru di 3.0.8

- **Tidak lagi ada dua keyboard.** Saat papan melayang dipakai, keyboard sistem ditutup otomatis — keyboard Penboard menutup dirinya sendiri, dan keyboard lain (mis. Huawei) ditutup lewat layanan **Penboard Tulis Otomatis**. Pengaturan baru **"Sembunyikan keyboard sistem"** di kartu Papan melayang (bawaan: nyala) bisa dimatikan bila Anda ingin keyboard tetap terbuka.
- **Geser papan melayang sudah benar arahnya**: menarik pegangan **ke atas menaikkan papan**, ke bawah menurunkan — sebelumnya terbalik karena `y` diukur dari tepi bawah layar (`Gravity.BOTTOM`).
- **Papan tidak bisa lagi nyangkut di luar layar**: posisi saat digeser dan posisi yang tersimpan dari sesi sebelumnya selalu dibatasi agar papan tetap terlihat penuh.

## Yang baru di 3.0.7

**Penting untuk perangkat Huawei/Honor tanpa Google Play services.** Mesin pengenalan Penboard (ML Kit) memang **tidak bisa berjalan tanpa Google Play services** — dan di perangkat seperti itu setiap tulisan berakhir dengan pesan "tidak terbaca" yang membingungkan (atau tidak ada pesan sama sekali). Sekarang alurnya jelas:

- **Pemeriksaan perangkat** menyebutkan keadaan Google Play services apa adanya — *tidak terpasang*, *perlu diperbarui*, *dinonaktifkan*, *tidak sah*, atau *sedang diperbarui* — bukan hanya "tidak tersedia".
- **Google Play services versi lama tidak lagi dianggap mati**: Penboard tetap mencoba mengenali, karena GMS versi lama umumnya masih bisa. Kalau ternyata memang perlu diperbarui, muncul tombol **"Perbarui Google Play services"** (membuka Play Store).
- Selalu ada tombol **"Buka keyboard tulisan tangan perangkat"** (pemilih keyboard) supaya Anda bisa berpindah ke tulisan tangan Huawei/Celia — dan papan melayang pun menawarkan itu otomatis saat mesinnya tidak ada.
- Papan tidak lagi menulis "tidak terbaca" untuk masalah mesin: kilasannya **"✗ mesin tidak ada"**, disertai penyebab + saran langkah yang bisa dilakukan.
- Bahasa yang belum didukung mesin diberi pesan sendiri ("pilih bahasa lain di kartu Pengenalan tulisan"), bukan disamakan dengan tulisan yang tidak terbaca.
- Panggilan ke ML Kit dibungkus pengaman: perangkat tanpa GMS tidak lagi bisa membuat aplikasi berhenti atau menggantung.

## Yang baru di 3.0.6

- **Tulisan dikirim ke mesin dalam beberapa versi ukuran.** Sebelumnya tinta selalu diperkecil mengikuti jarak baris panduan, sehingga tulisan kecil bisa jadi terlalu mungil untuk dikenali. Sekarang tinta yang sama disiapkan dalam beberapa versi (normalisasi baris, lalu dipaskan ke kotak tetap tinggi 160 dan 320) dan dicoba berurutan **sampai ada yang terbaca** — jadi ukuran tulisan tidak lagi menentukan berhasil/tidaknya.
- **Mesin diberi keterangan tambahan** (`RecognitionContext`): **ukuran bidang tulisan** (membantu membedakan huruf kecil dan huruf besar) dan **beberapa huruf sebelum kursor** — dipakai untuk memilih kata yang paling cocok dengan kalimat yang sedang ditulis.
- **Diagnostik baru**: kartu pemeriksaan menampilkan **jawaban terakhir mesin** (kandidat teks), dan ada tombol **"Salin laporan mesin"** yang menyalin ringkasan (versi, mesin tersedia/tidak, bahasa, status model, jumlah goresan/titik tinta, kandidat terakhir, pesan terakhir) untuk dilaporkan bila masih gagal.
- Pesan gagal baca kini menyebutkan **apa yang sebenarnya dibaca mesin** bila ada kandidat.

## Yang baru di 3.0.5

- **"✗ tidak terbaca" diperbaiki.** Selama ini Penboard mengirim tulisan ke mesin pengenalan tanpa memastikan **model bahasanya sudah terunduh**; ML Kit menolak permintaan itu, dan Penboard salah menerjemahkannya menjadi "tidak terbaca" — padahal masalahnya bukan tulisan Anda.
  - Sekarang Penboard **memeriksa dulu** status model: kalau sudah ada → langsung dikenali; kalau belum → **diunduh otomatis sekali** (dengan pemberitahuan "±20 MB, tunggu sebentar"), lalu tulisan yang sama langsung dikenali tanpa perlu ditulis ulang.
  - Bila internet tidak ada, muncul pesan yang benar: **"✗ model bahasa belum ada"** + tawaran pindah ke keyboard tulisan tangan perangkat — bukan lagi "tidak terbaca".
- **Alasan teknis ikut ditampilkan** saat pengenalan gagal, dan **kartu pemeriksaan** kini menampilkan status model bahasa (siap / belum diunduh) serta pesan terakhir dari mesin pengenalan — memudahkan menelusuri kalau ada masalah.
- `util/LanguageTags.kt`: mesin pengenalan menerima tag bahasa BCP-47 dengan atau tanpa wilayah (mis. `id-ID` dicoba, lalu `id` — nama model bahasa Indonesia di ML Kit), diuji unit.

## Yang baru di 3.0.4

- **Hasil tulisan langsung masuk ke kolom isian, tidak lagi sekadar tersalin.** Papan melayang bisa menulis lewat dua jalan:
  1. **keyboard Penboard** dipilih sebagai keyboard aktif (cara paling tepat), atau
  2. layanan aksesibilitas **"Penboard Tulis Otomatis"** — dipakai bila Anda memakai keyboard lain (mis. keyboard Huawei) atau tidak memakai keyboard sama sekali. Penboard menyisipkan teks pada posisi kursor tanpa menghapus isi kolom, dan kalau kolomnya tidak mendukung, memakai tempel dengan isi papan klip dikembalikan seperti semula.
- Kartu **Papan melayang** sekarang menampilkan status "Penboard Tulis Otomatis" + tombol untuk membukanya di Pengaturan > Aksesibilitas.
- Tombol kursor, hapus, dan papan klip juga bekerja lewat layanan itu, jadi papan melayang bisa dipakai sendiri tanpa keyboard Penboard.
- Pesan saat teks hanya bisa disalin sekarang menjelaskan **apa yang harus dinyalakan**.

## Yang baru di 3.0.3

- **Lencana hasil di papan lebih mudah dibaca**: teks kilasan ("✓ teks" / "✗ tidak terbaca") sekarang otomatis memilih tinta gelap atau putih sesuai kecerahan warna aksen — pada tema retro yang beraksen terang, teksnya jadi gelap dan tidak lagi tenggelam. (`util/ColorMath.kt`, diuji unit.)
- **Pratinjau susunan** ada di `docs/preview.png` (lihat di bawah) supaya mudah dibandingkan sebelum menyetel.

## Yang baru di 3.0.2

- **Preset HiWrite satu ketukan** (kartu "Mulai cepat" di halaman utama): langsung menyetel papan tinggi, 6 baris tombol berikon, tema retro grayscale ePaper, dan input otomatis 2 detik. Bahasa tulisan dan tombol samping pena tidak diubah.

## Yang baru di 3.0.1

- **Area tombol bertambah sampai 6 baris** (3 kolom tetap) — jadi sampai **18 tombol** yang semuanya bisa diatur, bukan lagi 12.
- Bawaan baris 5 dan 6 kini terisi: tab, hapus huruf di depan kursor, ganti warna pena, tulisan tangan perangkat (Huawei/Celia), pemilih keyboard, dan sembunyikan keyboard — siap dipakai atau tinggal diganti. (Sejak 3.0.9 tidak ada lagi tombol papan melayang.)
- Ukuran tombol tetap otomatis: makin banyak baris, makin kecil tombolnya, tetapi papan tulis tidak pernah kalah lebar (maksimal 34% layar untuk kolom tombol).

## Yang baru di 3.0.0

- **Tombol penempatan kursor**: ← → ↑ ↓ plus ke awal/akhir baris, digambar sebagai ikon panah (bisa ditahan untuk menggeser cepat).
- **Tombol keyboard konvensional**: spasi, enter, hapus (⌫, tahan untuk mengulang), hapus satu kata, hapus huruf di depan kursor, dan tab — semuanya berikon.
- **Tombol papan klip**: pilih semua, potong, salin, tempel.
- **Semua tombol memakai ikon**, bukan tulisan, sehingga ukurannya kecil dan seragam.
- **Jumlah baris area tombol bisa diatur**: 1 baris, 2 baris, sampai 6 baris (3 kolom tetap) — jadi sampai 18 tombol, dan jumlah tombol yang tampil juga bisa diatur (1–18).
- **Tema retro cyberpunk grayscale ePaper**: papan warna kertas, panel hitam bertekstur garis pindaian, garis panduan titik-titik, tombol monokrom, dan huruf mesin tik (monospace). Satu tombol untuk kembali ke tema ini kapan saja.
- **Papan tulis overlay dengan transparansi yang bisa diatur** (panel, papan, dan tombol masing-masing 0–100%).

## Cara pakai singkat

1. Pasang APK, buka Penboard, ikuti **panduan 3 langkah** di halaman utama.
2. Tulis satu kata dengan M-Pen. Begitu tidak ada goresan baru selama **2 detik**, tulisan otomatis menjadi teks + satu spasi, jadi kata berikutnya tinggal ditulis.
3. Mau teksnya masuk di **baris berikutnya**? Tulis saja di **baris kedua** papan (paling banyak 8 baris). Nomor baris tercetak di tepi kiri papan.
4. Butuh memindahkan kursor atau menambah spasi/enter? Pakai tombol ikon di kanan papan.
5. Ingin memaksa jadi teks sekarang? Ketuk tombol ikon **"A + percikan"** (Teks), atau tekan **tombol samping M-Pen**.
6. Mau layar lega? Ketuk tombol **minimize** (garis mendatar) di kanan bilah judul jendela — papan langsung disembunyikan.
7. Papan menutupi bagian yang ingin dilihat? **Tarik bilah judulnya** untuk memindahkan, atau **tarik sudut kanan bawahnya** untuk mengubah lebar dan tingginya.
8. Ingin menulis di atas aplikasi lain? Penboard sudah keyboard: panelnya menempel di atas aplikasi apa pun yang sedang dipakai. Mau pindah ke tulisan tangan Huawei dan kembali? Ketuk tombol ikon **papan tombol kecil** (pemilih keyboard).

## Susunan papan

![Pratinjau susunan panel: bingkai jendela + tombol minimize, bilah tombol pixel art, dan keyboard di atas aplikasi lain](docs/preview.png)

```
+-----------------------------------------------------+-----------------+
|  PENBOARD                               [<-->] [ - ]| tarik = pindah  |
+-----------------------------------------------------+-----------------+
|  [A+][enter][hapus][bersih][batal]... (maks 30)      |  tombol atas    |
+-----------------------------------------------------+-----------------+
|  TEPI ATAS PAPAN = TEPI ATAS TOMBOL        |  [<-]  |  [^]  |  [->]  |   baris 1
|                                            | (kotak | (kotak| (kotak |           <- sisi tombol
|            PAPAN TULIS (kiri)              |  sama) |  sama)|  sama) |   baris 2   = panjang
|      lebarnya = isi panel - kolom tombol   |  [v]   | [spasi]| [enter]|   baris 3   = lebar
|      tingginya = 3 baris (bawaan)          |  [x]   | [A+]  | [tmr]  |   baris 4
|                                            |        |       |        |   (bila ditambah)
+-----------------------------------------------------+-----------------+
        sudut kanan bawah = tarik untuk mengubah lebar & tinggi papan
```


Kolom tombol kanan **rata atas** dengan area tulis. Jumlah barisnya **mengikuti baris area
tulis** secara bawaan, atau **diatur sendiri** 1–6 lewat sakelar di kartu *Area tombol*.
Tombolnya **persegi**; tingginya = sisi tombol (otomatis = tinggi satu baris tulisan, atau
disetel 32–160 dp), jadi kalau baris tombol lebih sedikit dari baris tulisan, kolomnya berhenti
di tengah papan — memang begitu yang diminta (sejajar atas saja).

Kartu *Pratinjau* menampilkan bukti terukur, mis. *"✓ Tombol persegi 108 px: 3 kolom x 3 baris"*
dan *"✓ Baris tombol samping = baris area tulis: 3 = 3 (rata atas)"*. Tarikan pada bilah judul
(pindah) dan sudut kanan bawah (ubah ukuran) juga bisa dicoba langsung di kartu itu; angkanya
diatur di kartu *Jendela papan*.

**Baris tulisan = posisi teks.** Baris panduan papan bernomor (1, 2, 3, …) dan nomor itu
menentukan posisi hasil tulisan: baris 1 = di posisi kursor, baris 2 = satu enter (satu baris
di bawah kursor), baris 3 = dua enter, sampai 8 baris. Di kolom isian satu baris, enter
dilewati. Bisa dimatikan di pengaturan papan.

Bilah judul berisi judul jendela + tombol minimize, dan dipakai sebagai **pegangan untuk
memindahkan papan**. **Tombol atas** berada di dalam bingkai yang sama, **di atas area
menulis** (tepat di bawah bilah judul), sampai **30 tombol**; tombolnya **rata kiri** dengan
jarak yang sama seperti tombol samping (tidak dilebarkan memenuhi papan) dan ukuran dasarnya
mengikuti tombol samping — jadi makin kecil tombol samping, makin banyak yang muat per baris —
tetapi **lebar dan tinggi tiap tombol bisa diatur sendiri**; isinya bebas diatur (lihat
**Pengaturan yang tersedia**).

Tinggi papan menyesuaikan ruang yang disediakan keyboard: kalau layar tidak cukup untuk
bingkai + bilah judul + tombol bawah + semua baris tulisan, **jarak baris dan ukuran tombol
dipangkas otomatis** (jarak baris tidak pernah di bawah 1 px) supaya tidak ada bagian yang
terpotong.

- **Papan tulis** memakai lebar sebanyak mungkin di kiri (sampai menyentuh tombol), dan
  melebar/menyempit mengikuti **Lebar papan** atau tarikan sudut. **Lebar area tulis**
  (40–80%) menjamin area menulis tidak terdesak kolom tombol.
- **Area tulis**: 1–6 baris (bawaan **3**), tinggi = baris x jarak baris.
- **Area tombol**: kolom 1–6 dan baris 1–6; barisnya **mengikuti area tulis** (bawaan) atau
  **diatur sendiri** (sampai 36 tombol persegi).
- **Ukuran tombol**: samping otomatis (sisi = jarak baris) atau 32–160 dp; tombol atas punya
  pengali lebar (1x–4x) dan tinggi (0,6x–2x) sendiri.
- Setiap tombol bebas dipilih aksinya dan bisa diberi label sendiri.

## Daftar tombol (ikon)

| Ikon | Aksi |
| --- | --- |
| panah ← → ↑ ↓ | geser kursor kiri/kanan/atas/bawah |
| panah + garis | kursor ke awal baris / ke akhir baris |
| ␣ | spasi |
| ↵ | enter / baris baru |
| ⌫ | hapus satu huruf (tahan = mengulang) |
| huruf dicoret | hapus satu kata |
| ⌫ terbalik | hapus huruf di depan kursor |
| tab | tab (kolom berikutnya) |
| bingkai putus-putus | pilih semua teks |
| gunting | potong | 
| dua lembar | salin |
| papan klip | tempel |
| pena | ganti warna pena |
| A + percikan | ubah tulisan menjadi teks |
| tong sampah | bersihkan papan |
| penghapus | hapus goresan terakhir |
| panah melengkung | batal (undo) / ulangi (redo) |
| coretan tangan | pindah ke tulisan tangan perangkat (Huawei/Celia) |
| papan tombol kecil | buka pemilih keyboard (mis. pindah ke tulisan tangan Huawei) |
| panah bawah | sembunyikan keyboard |
| garis mendatar (pixel art) | minimize: sembunyikan papan (di bilah judul jendela) |

Semua tombol memakai ikon **pixel art** 12 x 12 dari `util/PixelArt.kt` (setiap aksi
punya polanya sendiri) — bawaan sejak 3.2.0. Bila ingin ikon bergaris halus seperti versi
3.1, ganti **gaya ikon tombol** ke *Vektor* di kartu *Tampilan*.

## Pengaturan yang tersedia

| Bagian | Isi |
| --- | --- |
| Papan tulis | **posisi teks mengikuti baris tulisan** (baris 1 = di kursor, baris 2 = +1 enter, baris 3 = +2 enter), **jumlah baris area tulis 1–6 (bawaan 3)**, jarak garis panduan, tebal pena, 1–6 warna pena, garis panduan (+ nomor baris), ketebalan ikut tekanan stylus, izinkan jari, getaran |
| Area tombol | **jumlah kolom 1–6**, **baris 1–6** (bawaan mengikuti baris area tulis, bisa diatur sendiri), jumlah tombol tampil, **ukuran sisi tombol** (otomatis = tinggi satu baris tulisan, atau 32–160 dp), fungsi + label + teks tiap tombol, ulangi saat ditahan, kembali ke bawaan |
| Pengenalan | input otomatis on/off, jeda 0,5–5 detik (bawaan 2 detik), spasi otomatis, kosongkan papan setelah jadi teks, bahasa tulisan, unduh model bahasa |
| Tampilan | **tema retro cyberpunk grayscale ePaper** (satu tombol), garis pindaian, **gaya ikon tombol (pixel art / vektor)**, transparansi panel/papan/tombol 0–100%, warna panel/papan/garis/tombol/teks tombol/aksen, lengkung sudut |
| Tombol samping pena | aksi klik dan aksi kedua, dari 7 pilihan |
| Jendela papan | bingkai jendela on/off, tombol minimize on/off (tetap ada sebagai bilah kecil bila bingkainya dimatikan), judul jendela (bawaan `PENBOARD`), **papan bisa dipindah / diangkat** (tarik bilah judul), **lebar papan 45–100 %** (atau tarik sudut kanan bawah), **lebar area tulis 40–80 %** |
| Tombol atas | tampil/tidak, **jumlah 0–30**, **rata kiri dengan jarak sama seperti tombol samping** (jumlah yang muat mengikuti ukuran tombol samping), aksi + label + tahan-untuk-mengulang + **lebar 0,6x–4x** dan **tinggi 0,6x–2x** tiap tombol (dasar = ukuran tombol samping, letaknya di dalam bingkai **di atas** area menulis), kembali ke bawaan |

Tombol panah kursor, papan klip, tab, dan hapus di depan kursor bekerja saat keyboard Penboard aktif — semuanya memerlukan kolom isian.

## Mesin pengenalan

Pengenalan memakai **ML Kit Digital Ink Recognition** (Google) yang berjalan **di dalam ponsel dan luring** setelah model bahasanya terunduh. Bawaan: bahasa Indonesia (`id-ID`, sekitar 20 MB). Tersedia juga English US/UK, Melayu, Jawa, Sunda, 中文, 日本語, 한국어, العربية, Русский, हिन्दी, ไทย, Tiếng Việt.

Umpan balik saat menulis:

- Garis **hitung mundur** di papan menunjukkan kapan input otomatis akan berjalan; hitungan berhenti begitu pena kembali menyentuh papan (atau bisa dimatikan).
- Setelah teks terkirim, muncul **kilasan tanda centang + teks**; kalau gagal, muncul **"✗ tidak terbaca"**.
- **Tekanan M-Pen** membuat goresan lebih tebal atau tipis; **ujung penghapus stylus** menghapus goresan di sekitar sentuhan.

### Soal Huawei

Mesin tulisan tangan Huawei (Celia) **tidak bisa dipanggil oleh aplikasi lain** — Huawei hanya menyediakannya lewat keyboard bawaannya, dan HMS ML Kit hanya menyediakan OCR gambar (bukan tulisan tangan). Karena itu Penboard:

1. memakai tombol samping M-Pen sebagai pemicu teks sehingga tidak bentrok dengan pintasan keyboard Huawei;
2. menyediakan tombol **coretan tangan** pada grid untuk berpindah ke keyboard tulisan tangan bawaan, dan bisa kembali ke Penboard kapan saja;
3. mematikan tulisan tangan sistem pada kolom **Uji tulis** (agar goresan tidak muncul dua kali);
4. punya pilihan **pindah otomatis** ke keyboard tulisan tangan perangkat bila mesin pengenalan bawaan tidak ada (mis. perangkat Huawei tanpa Google Play services).

Semua data tetap di ponsel. Tidak ada tulisan yang dikirim ke server mana pun; hanya unduhan model bahasa yang menyentuh internet, itu pun sekali per bahasa.

## Menulis di atas aplikasi lain

Penboard adalah keyboard, jadi papan tulisnya sudah menempel di atas aplikasi apa pun yang sedang dipakai — tidak perlu izin khusus.

- **Keyboard Penboard aktif** (cara utama): tulis di papan, teks masuk ke kolom isian yang sedang fokus.
- **Keyboard lain yang aktif** (mis. keyboard Huawei): nyalakan layanan aksesibilitas **Penboard Tulis Otomatis** (kartu *Keyboard Penboard* → *Aktifkan Tulis Otomatis*). Layanan ini menuliskan hasil tulisan ke kolom isian aplikasi lain tanpa Penboard harus menjadi keyboard.
- **Belum ada keduanya**: teks disalin ke papan klip, dan pesannya menjelaskan apa yang perlu dinyalakan.

Layanan aksesibilitas hanya menulis teks; ia tidak membaca layar untuk hal lain dan tidak mengirim data ke mana pun.

## Bangun sendiri

```bash
./gradlew assembleDebug     # APK debug
./gradlew assembleRelease   # APK rilis (dipakai CI)
./gradlew test              # pengujian unit
```

Butuh JDK 17 dan Android SDK dengan platform 35. Rilis ditandatangani kunci debug kecuali berkas `keystore.properties` disediakan:

```properties
storeFile=...
storePassword=...
keyAlias=...
keyPassword=...
```

Teknologi: Kotlin 2.0, AGP 8.7, Gradle 8.11, `minSdk 26`, `targetSdk 35`, satu modul `app`, tanpa berkas tata letak XML (seluruh tampilan dibuat dari kode, termasuk ikon tombol).

## Struktur berkas

```
app/src/main/java/dev/retrobit/penboard/
├── MainActivity.kt            pengaturan + pratinjau + uji tulis
├── board/                     BoardView (kanvas stylus), Stroke
├── data/                      PenboardConfig + ConfigStore (JSON)
├── ime/                       PenboardInputService (keyboard)
├── overlay/                   PenboardTypingService (layanan aksesibilitas cadangan)
├── ink/                       InkRecognition (ML Kit), InkNormalizer, RecognitionText, RecognitionLanguages
├── ui/                        BoardPanel, KeyButton, KeyIcons, ScanlineDrawable, Ui, ColorPicker
└── util/                      BoardMath, GridMath, PixelArt, StylusCompat, SystemHandwriting, GmsStatus, LanguageTags, AccessibilityIds, TextInsert
```

## Lisensi

MIT — lihat [LICENSE](LICENSE).
