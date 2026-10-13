# Penboard

Papan tulis stylus untuk Android. Tulis dengan Huawei M-Pen (atau stylus apa pun) di papan tulis, lalu tulisan itu berubah menjadi teks dan langsung masuk ke kolom isian aplikasi yang sedang dipakai. Ini **bukan keyboard QWERTY** — papan tulisnya lebar di kiri, tombol-tombol operasinya kecil dan rapi 3x3 di kanan.

Versi terbaru: **2.0.2** (lihat [Releases](https://github.com/retrobit-dev/RetroBit-Penboard/releases) untuk berkas APK).

## Cara pakai singkat

1. Pasang APK, buka Penboard.
2. **Keyboard Penboard**: ketuk *Pilih keyboard* lalu pilih **Penboard**. Setelah itu, di aplikasi mana pun, papan tulis muncul saat mengetuk kolom isian.
3. Tulis satu kata dengan M-Pen. Begitu tidak ada goresan baru selama **2 detik**, tulisan otomatis diubah menjadi teks + satu spasi, jadi kata berikutnya tinggal ditulis.
4. Ingin memaksa jadi teks sekarang? Ketuk tombol **Teks**, atau tekan **tombol samping M-Pen**.
5. Ingin menulis di atas aplikasi lain? Beri izin *tampil di atas aplikasi lain*, lalu nyalakan **Papan melayang**.

## Susunan papan

```
+----------------------------------------------+---------------+
|                                              |   [1] [2] [3] |
|            PAPAN TULIS (kiri)                |   [4] [5] [6] |
|      dari tepi kiri sampai ke tombol         |   [7] [8] [9] |
+----------------------------------------------+---------------+
```

- Papan tulis memakai lebar sebanyak mungkin di kiri (sampai menyentuh tombol).
- Sebelah kanan berisi grid tombol **3 baris x 3 kolom**.
- Jumlah tombol bisa diatur 1 sampai 9, dan **setiap tombol bebas dipilih fungsinya** serta labelnya.

## Pengaturan yang tersedia

| Bagian | Isi |
| --- | --- |
| Papan tulis | tinggi papan, jarak garis panduan, tebal pena, 1–6 warna pena, garis panduan, ketebalan ikut tekanan stylus, izinkan jari, getaran |
| Tombol 3x3 | jumlah tombol (1–9), fungsi + label + teks tiap tombol, ulangi saat ditahan, kembali ke bawaan |
| Pengenalan | input otomatis on/off, jeda 0,5–5 detik (bawaan 2 detik), spasi otomatis, kosongkan papan setelah jadi teks, bahasa tulisan, unduh model bahasa |
| Tampilan | transparansi panel/papan/tombol (0–100%), warna panel/papan/garis/tombol/teks tombol/aksen, lengkung sudut |
| Tombol samping pena | aksi klik dan aksi kedua, dari 7 pilihan: tidak ada, jadikan teks, batal goresan, hapus goresan, bersihkan papan, tulisan tangan perangkat, papan melayang |

Panel dan papan dibuat tembus pandang supaya tulisan di aplikasi lain tetap terbaca.

## Fungsi tombol

`abc` (teks tertentu) · Spasi · Baris · Hapus satu huruf · Hapus satu kata · Batal (undo) · Ulangi (redo) · Bersihkan · Hapus gores · Warna · Teks (ubah jadi teks) · Huawei (pindah ke tulisan tangan perangkat) · Layang (papan melayang) · Tutup (sembunyikan keyboard).

Umpan balik saat menulis:

- Garis **hitung mundur** di papan menunjukkan kapan input otomatis akan berjalan.
- Setelah teks terkirim, muncul **kilasan tanda centang + teks** yang baru masuk, jadi kelihatan hasil pengenalannya.
- **Tekanan M-Pen** membuat goresan lebih tebal atau tipis (bisa dimatikan).
- **Ujung penghapus stylus** menghapus goresan yang ada di sekitar sentuhan itu.

## Mesin pengenalan

Pengenalan memakai **ML Kit Digital Ink Recognition** (Google) yang berjalan **di dalam ponsel dan luring** setelah model bahasanya terunduh. Bawaan: bahasa Indonesia (`id-ID`, sekitar 20 MB). Tersedia juga English US/UK, Melayu, Jawa, Sunda, 中文, 日本語, 한국어, العربية, Русский, हिन्दी, ไทย, Tiếng Việt.

### Soal Huawei

Mesin tulisan tangan Huawei (Celia) **tidak bisa dipanggil oleh aplikasi lain** — Huawei hanya menyediakannya lewat keyboard bawaannya, dan HMS ML Kit hanya menyediakan OCR gambar (bukan tulisan tangan). Karena itu Penboard:

1. memakai tombol samping M-Pen sebagai pemicu teks sehingga tidak bentrok dengan pintasan keyboard Huawei;
2. menyediakan tombol **Huawei** pada grid untuk berpindah ke keyboard tulisan tangan bawaan, dan bisa kembali ke Penboard kapan saja;
3. mematikan tulisan tangan sistem pada kolom **Uji tulis** (agar goresan tidak muncul dua kali);
4. punya pilihan **pindah otomatis** ke keyboard tulisan tangan perangkat bila mesin pengenalan bawaan tidak ada (mis. perangkat Huawei tanpa Google Play services).

Semua data tetap di ponsel. Tidak ada tulisan yang dikirim ke server mana pun; hanya unduhan model bahasa yang menyentuh internet, itu pun sekali per bahasa.

## Papan melayang

- Perlu izin *tampil di atas aplikasi lain*.
- Bisa digeser lewat pegangan kecil di atas papan; posisinya diingat.
- Bila keyboard Penboard sedang aktif, teks langsung dikirim ke kolom isian; bila tidak, teks disalin ke papan klip dan diumumkan lewat pesan singkat.
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

Teknologi: Kotlin 2.0, AGP 8.7, Gradle 8.11, `minSdk 26`, `targetSdk 35`, satu modul `app`, tanpa berkas tata letak XML (seluruh tampilan dibuat dari kode).

## Struktur berkas

```
app/src/main/java/dev/retrobit/penboard/
├── MainActivity.kt            pengaturan + pratinjau + uji tulis
├── board/                     BoardView (kanvas stylus), Stroke
├── data/                      PenboardConfig + ConfigStore (JSON)
├── ime/                       PenboardInputService (keyboard)
├── overlay/                   PenboardOverlayService (papan melayang)
├── ink/                       InkRecognition (ML Kit), InkNormalizer, RecognitionText, RecognitionLanguages
├── ui/                        BoardPanel, KeyButton, Ui, ColorPicker
└── util/                      BoardMath, GridMath, StylusCompat, SystemHandwriting
```

## Lisensi

MIT — lihat [LICENSE](LICENSE).
