# Penboard

Papan tulis stylus bergaya **HiWrite** untuk Android, dengan tema **retro cyberpunk grayscale ePaper**. Tulis dengan Huawei M-Pen (atau stylus apa pun) di papan tulis, lalu tulisan itu berubah menjadi teks dan langsung masuk ke kolom isian aplikasi yang sedang dipakai. Bukan keyboard QWERTY: papan tulisnya lebar di kiri, tombolnya berupa **ikon** dan tersusun rapi 3 kolom di kanan.

Versi terbaru: **3.0.5** (lihat [Releases](https://github.com/retrobit-dev/RetroBit-Penboard/releases) untuk berkas APK).

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

- **Preset HiWrite satu ketukan** (kartu "Mulai cepat" di halaman utama): langsung menyetel papan tinggi, 6 baris tombol berikon, tema retro grayscale ePaper, dan input otomatis 2 detik. Bahasa tulisan, tombol samping pena, dan letak papan melayang tidak diubah.

## Yang baru di 3.0.1

- **Area tombol bertambah sampai 6 baris** (3 kolom tetap) — jadi sampai **18 tombol** yang semuanya bisa diatur, bukan lagi 12.
- Bawaan baris 5 dan 6 kini terisi: tab, hapus huruf di depan kursor, ganti warna pena, tulisan tangan perangkat (Huawei/Celia), papan melayang, dan sembunyikan keyboard — siap dipakai atau tinggal diganti.
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
3. Butuh memindahkan kursor atau menambah spasi/enter? Pakai tombol ikon di kanan papan.
4. Ingin memaksa jadi teks sekarang? Ketuk tombol ikon **"A + percikan"** (Teks), atau tekan **tombol samping M-Pen**.
5. Ingin menulis di atas aplikasi lain? Beri izin *tampil di atas aplikasi lain*, lalu nyalakan **Papan melayang**.

## Susunan papan

![Pratinjau susunan panel: keyboard 3 baris, preset HiWrite 6 baris, dan papan melayang 60% transparan](docs/preview.png)

```
+----------------------------------------------+----------------+
|                                              |   [<-] [^] [->] |  baris 1
|            PAPAN TULIS (kiri)                |   [v] [spasi] [!]|  baris 2
|      dari tepi kiri sampai ke tombol         |   [x] [A+] [tmr] |  baris 3
|                                              |   [x2][u] [r]    |  baris 4
|                                              |   [x>][tab][wrn] |  baris 5
|                                              |   [tw][lay][tut] |  baris 6
+----------------------------------------------+----------------+
```

- **Papan tulis** memakai lebar sebanyak mungkin di kiri (sampai menyentuh tombol).
- **Area tombol**: 3 kolom tetap, jumlah baris 1–6 (diatur pengguna), sampai 18 tombol.
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
| dua jendela | nyalakan papan melayang |
| panah bawah | sembunyikan keyboard |

## Pengaturan yang tersedia

| Bagian | Isi |
| --- | --- |
| Papan tulis | tinggi papan, jarak garis panduan, tebal pena, 1–6 warna pena, garis panduan, ketebalan ikut tekanan stylus, izinkan jari, getaran |
| Area tombol | **jumlah baris 1–6**, jumlah tombol tampil 1–18, fungsi + label + teks tiap tombol, ulangi saat ditahan, kembali ke bawaan |
| Pengenalan | input otomatis on/off, jeda 0,5–5 detik (bawaan 2 detik), spasi otomatis, kosongkan papan setelah jadi teks, bahasa tulisan, unduh model bahasa |
| Tampilan | **tema retro cyberpunk grayscale ePaper** (satu tombol), garis pindaian, transparansi panel/papan/tombol 0–100%, warna panel/papan/garis/tombol/teks tombol/aksen, lengkung sudut |
| Tombol samping pena | aksi klik dan aksi kedua, dari 7 pilihan |

Tombol panah kursor, papan klip, tab, dan hapus di depan kursor bekerja saat keyboard Penboard aktif (karena memerlukan kolom isian); papan melayang menulis dan menyalin teks.

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

## Papan melayang

- Perlu izin *tampil di atas aplikasi lain*.
- Bisa digeser lewat pegangan kecil di atas papan; posisinya diingat.
- Transparansinya mengikuti pengaturan tema (panel/papan/tombol).
- Bila keyboard Penboard sedang aktif, teks langsung dikirim ke kolom isian.
- Bila Anda memakai keyboard lain (mis. keyboard Huawei) atau tidak memakai keyboard sama sekali, nyalakan layanan aksesibilitas **Penboard Tulis Otomatis** (kartu Papan melayang → Aktifkan Tulis Otomatis). Dengan begitu tulisan tetap langsung masuk ke kolom isian yang sedang fokus.
- Bila keduanya belum ada, teks disalin ke papan klip dan pesannya menjelaskan apa yang perlu dinyalakan.
- Berjalan sebagai foreground service dengan notifikasi biasa.

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
├── overlay/                   PenboardOverlayService (papan melayang)
├── ink/                       InkRecognition (ML Kit), InkNormalizer, RecognitionText, RecognitionLanguages
├── ui/                        BoardPanel, KeyButton, KeyIcons, ScanlineDrawable, Ui, ColorPicker
└── util/                      BoardMath, GridMath, StylusCompat, SystemHandwriting
```

## Lisensi

MIT — lihat [LICENSE](LICENSE).
