# Penboard 2 — papan tulis stylus sebagai keyboard

**Penboard bukan keyboard QWERTY.** Ini adalah **papan tulis** untuk stylus (mis. Huawei M-Pen):
Anda menulis di papan, tulisan diubah menjadi teks dan langsung masuk ke kolom isian.

Susunannya sederhana dan sesuai permintaan:

```
┌────────────────────────────────┬───────────┐
│                                │ [ Teks ]  [ Spasi ] [ Baris ]   │
│        papan tulis             │ [  ⌫  ]  [  ↶  ]  [ Bersih ]    │
│        (sampai ke tombol)      │ [ Gores] [Huawei] [ Tutup ]     │
└────────────────────────────────┴───────────┘
     kiri: area menulis              kanan: grid tombol 3×3
```

Semua bisa diatur: **tinggi papan**, **tingkat transparansi** (panel, papan, tombol),
**warna** apa pun, **berapa tombol** yang ada, dan **aksi tiap tombol**.

---

## Cara pakai (5 langkah)

1. Pasang APK dari **[Releases](https://github.com/retrobit-dev/RetroBit-Penboard/releases)**.
2. Buka aplikasi **Penboard** → kartu **1. Aktifkan keyboard Penboard** →
   **Pilih keyboard** → pilih **Penboard**.
3. Buka aplikasi apa pun yang punya kolom isian (catatan, chat, pencarian).
   Keyboard Penboard muncul: papan tulis di kiri, tombol 3×3 di kanan.
4. **Tulis satu kata** di papan dengan stylus. Diam sejenak (bawaan **2 detik**) →
   tulisan langsung diinput sebagai teks dan **satu spasi** ditambahkan untuk kata berikutnya.
5. Mau memakai mesin tulisan tangan Huawei? Ketuk tombol **Huawei** pada grid —
   Penboard berpindah ke keyboard tulisan tangan perangkat. Setelah selesai,
   pilih lagi Penboard dari daftar keyboard.

Ada juga **papan melayang**: papan tulis yang muncul di atas aplikasi lain
(kartu **2. Papan melayang**), bisa digeser lewat pegangan di atasnya.

---

## Yang bisa diatur

| Bagian | Pilihan |
|---|---|
| **Papan tulis** | tinggi papan (120–340 dp), jarak garis panduan (28–76 dp), ketebalan pena (1–12 dp), sampai **6 warna pena** (warna apa pun, ada pengatur transparansi tiap warna), tampil/sembunyi garis, izinkan jari menulis, getar |
| **Tombol samping pena** | primer & sekunder: jadikan teks, batal goresan, hapus goresan terakhir, bersihkan papan, tulisan tangan Huawei, papan melayang |
| **Grid tombol 3×3** | jumlah tombol **1–9**, tiap slot bisa diganti aksi + nama + teks sendiri, tombol bisa mengulang bila ditahan |
| **Pengenalan tulisan** | input otomatis on/off, jeda 0,5–5 detik, spasi otomatis, bersihkan papan setelah diinput, 14 bahasa (bawaan **Indonesia**) |
| **Transparansi & warna** | transparansi panel/papan/tombol 0–100 %, warna panel, papan, garis, tombol, tulisan tombol, garis tepi, kebulatan sudut |

Semua pengaturan tersimpan otomatis dan bisa dikembalikan ke bawaan
(**Kembalikan semua pengaturan**).

### Aksi tombol yang tersedia

`abc` (ketik teks sendiri) · Spasi · Baris · `⌫` hapus huruf · `⌫⌫` hapus kata ·
`↶` batal · `↷` ulangi · Bersih · Hapus gores · Warna (ganti pena) ·
Teks (jadikan teks sekarang) · Huawei (pindah ke tulisan tangan perangkat) ·
Layang (papan melayang) · Tutup (sembunyikan keyboard).

---

## Soal tulisan tangan Huawei (jujur apa adanya)

Huawei/Honor **tidak menyediakan API** pengenalan tulisan tangan untuk aplikasi pihak ketiga —
HMS Core hanya menyediakan OCR gambar (teks dari foto), bukan mesin digital ink keyboard Celia.
Karena itu Penboard:

- memakai **ML Kit Digital Ink** bawaan sendiri (berjalan di perangkat, luring setelah model
  terunduh ±20 MB, sama seperti yang dipakai keyboard Google), dan
- menyediakan tombol **Huawei** sebagai **jembatan**: sekali ketuk, Anda berpindah ke keyboard
  tulisan tangan perangkat untuk memakai mesinnya, lalu kembali ke Penboard.

Supaya tidak bertabrakan dengan bantalan tulisan tangan bawaan perangkat, Penboard
**menolak mode tulisan tangan stylus sistem** (`supportsStylusHandwriting="false"`) sehingga
yang muncul selalu papan Penboard, bukan panel bawaan Huawei/Celia.

---

## Membangun sendiri

Butuh JDK 17 + Android SDK (compileSdk 35). Tidak perlu Android Studio.

```bash
./gradlew :app:assembleDebug          # APK debug
./gradlew :app:testDebugUnitTest      # pengujian unit
./gradlew :app:assembleRelease        # APK rilis (ditandatangani kunci debug bila
                                      # keystore.properties tidak ada)
```

Sidik jari rilis: buat `keystore.properties` di akar proyek:

```properties
storeFile=/path/keystore.jks
storePassword=…
keyAlias=…
keyPassword=…
```

APK siap pakai juga dibuat otomatis oleh GitHub Actions
(`.github/workflows/android.yml`) pada setiap push; tag `v*` menerbitkan Release
beserta APK-nya.

---

## Struktur berkas

```
app/src/main/java/dev/retrobit/penboard/
├── board/    Stroke.kt (titik & goresan), BoardView.kt (papan tulis, tombol pena, penghapus)
├── data/     PenboardConfig.kt (semua pengaturan + pembersihan angka), ConfigStore.kt (penyimpanan)
├── ime/      PenboardInputService.kt (keyboard papan tulis)
├── ink/      InkNormalizer.kt, RecognitionText.kt, RecognitionLanguages.kt,
│             InkRecognition.kt (ML Kit), HandwritingInput.kt (input otomatis 2 detik)
├── overlay/  PenboardOverlayService.kt (papan melayang)
├── ui/       BoardPanel.kt (papan kiri + grid 3×3), KeyButton.kt, Ui.kt, ColorPicker.kt
└── util/     GridMath.kt, BoardMath.kt, StylusCompat.kt, SystemHandwriting.kt (jembatan Huawei)
```

## Pengujian

`./gradlew :app:testDebugUnitTest` — 6 berkas pengujian untuk bagian yang murni logika:

- `util/GridMathTest` — ukuran tombol grid 3×3 selalu menyisakan papan lebar di kiri
- `util/BoardMathTest` — pemetaan tulisan ke baris panduan
- `util/SystemHandwritingTest` — pemilihan keyboard tulisan tangan Huawei/Celia
- `data/PenboardConfigTest` — bawaan, simpan/muat, pemangkasan angka, migrasi berkas lama
- `ink/RecognitionTextTest` — satu tulisan = satu kata + spasi otomatis
- `ink/InkNormalizerTest` — baris dipadatkan dan tinggi tulisan dinormalkan

## Privasi

Tidak ada data yang keluar dari perangkat Anda: tidak ada analitik, tidak ada iklan,
tidak ada pengiriman tulisan ke internet. Pengenalan tulisan berjalan di perangkat
(model bahasa diunduh sekali dari Google, setelah itu luring penuh).

## Lisensi

[MIT](LICENSE) © 2026 RetroBit
