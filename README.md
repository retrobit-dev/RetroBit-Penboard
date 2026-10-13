# RetroBit Penboard

**Keyboard papan tulis stylus untuk Android.** Bukan keyboard QWERTY: Penboard adalah papan tulis
(handwriting board) yang muncul seperti keyboard, dirancang untuk **Huawei M-Pencil** dan stylus
lainnya, dan **semuanya bisa diatur**: ukuran, transparansi, tombol apa saja yang ingin ditampilkan,
serta tata letaknya.

> Tulis dengan stylus di papan, lalu gunakan tombol yang Anda susun sendiri untuk
> mengirim spasi/simbol/entri, menghapus huruf, menggeser kursor, menyimpan tulisan sebagai gambar,
> sampai berganti profil papan tanpa keluar dari aplikasi.

[![Android CI](https://github.com/retrobit-dev/RetroBit-Penboard/actions/workflows/android.yml/badge.svg)](https://github.com/retrobit-dev/RetroBit-Penboard/actions/workflows/android.yml)

---

## 1. Fitur

### Baru di v1.2 — pengenalan tulisan (OCR) & dua varian APK
- **Tulisan → teks**: tombol *Tulisan → teks (pengenalan)* mengubah tulisan pada beberapa baris
  terakhir menjadi teks dan langsung mengirimkannya ke kolom aplikasi tujuan (memakai
  **ML Kit Digital Ink** — pengenalan berjalan **di perangkat**).
- **Dua varian** supaya janji ringan & luring tetap utuh:

  | Varian | Isi | Ukuran | Izin |
  | --- | --- | --- | --- |
  | **offline** (bawaan) | papan tulis + tombol, **tanpa** mesin pengenalan | tetap ringan | **tanpa izin internet** |
  | **ink** (*Penboard OCR*) | varian offline **+ pengenalan tulisan** | lebih besar (berisi ML Kit) | `INTERNET` — hanya untuk mengunduh model bahasa |

- Editor mendapat kartu **Pengenalan tulisan**: pilih bahasa (14 bahasa, termasuk Indonesia & Jawa),
  jumlah baris yang dikenali (1–6), opsi hapus tulisan setelah dikenali, dan tombol unduh model.
- Saat tombol pengenalan ditekan dan model bahasa belum ada, aplikasi **mengunduh model otomatis**
  (±20 MB, sekali saja) lalu mengenali tulisan; setelah itu semuanya berjalan luring.

### Baru di v1.1
- **Tulisan tersimpan otomatis per papan** — tulisan tetap ada walau keyboard ditutup, aplikasi
  ditutup, atau HP dimatikan (berkas ringkas di penyimpanan internal, satu berkas per profil).
- **Menelusuri tulisan lama** — tombol *Lihat tulisan lama* / *Kembali ke baris baru* menggeser
  tampilan papan 3 baris setiap kali ditekan (tulisan tidak hilang, hanya tampilannya bergeser).
- **Warna pena tetap** — daftar warna milik profil; tombol warna bergiliran memakai warna tersebut
  dan tetap dipakai sampai ditekan lagi (setelah warna terakhir kembali ke warna profil).
- Tombol **Profil sebelumnya** dan **Profil berikutnya**; tiap profil punya isi papan sendiri.
- 2 preset baru: **Warna Pena Tetap** dan **Papan Tulis Leluasa** (8 preset total).
- Rumus papan (jarak baris, geser otomatis, geser tampilan) dipindah ke `BoardMath` dan **diuji unit test**.

### Papan tulis stylus
- Menggambar dengan stylus secara halus (path kuadratik), **tekanan pena ikut memengaruhi tebal garis**.
- **Tombol samping pena = Urungkan (undo)**, **ujung penghapus pena = mode hapus** (kalau stylus Anda mendukungnya) — perilaku khas stylus EMR seperti M-Pencil.
- **Penolakan telapak tangan**: bila “Terima sentuhan jari” dimatikan, hanya stylus yang menulis.
- **Penghapus goresan** (menyentuh goresan → terhapus) lengkap dengan undo.
- **Geser papan otomatis**: saat baris bawah terisi, tulisan naik satu baris sehingga selalu tersedia baris kosong untuk menulis berikutnya.
- **Urungkan / Ulangi** (undo/redo) untuk goresan, penghapusan, dan pergeseran papan.
- Warna pena berganti cepat lewat tombol (8 warna).
- Simpan tulisan ke **Galeri › Pictures › Penboard** dalam bentuk PNG.

### Keyboard yang bisa diatur total
| Yang bisa diatur | Keterangan |
| --- | --- |
| Tinggi papan | 140–600 dp |
| Jarak baris tulis | 22–96 dp (jumlah baris terlihat dihitung otomatis) |
| Transparansi | panel keyboard, latar papan, garis panduan, latar tombol (0–100%) |
| Warna | panel, latar papan, garis, tinta, tombol, teks tombol, aksen (palet + kode hex) |
| Tombol | tinggi, sudut, ukuran huruf, aksen |
| Pena | warna tinta, tebal pena, ukuran penghapus |
| Tata letak | laci tombol bisa di **bawah / atas / samping papan**, dan bisa disembunyikan |
| Baris tombol | tambah/hapus baris, geser urutan baris, tinggi baris relatif |
| Tombol | aksi, teks, **lebar relatif**, **isi sisa ruang (%)**, ulangi saat ditahan, geser posisi |

Aksi tombol yang tersedia: teks/simbol bebas, spasi, entri (baris baru), hapus huruf, hapus goresan
terakhir, bersihkan papan, urungkan, ulangi, geser kursor kiri/kanan, lihat tulisan lama, kembali ke
baris baru, simpan gambar, warna pena bergiliran, profil sebelumnya/berikutnya, alat pena, alat
penghapus, sembunyikan papan.

### Siap pakai & bisa disalin
8 preset bawaan (Catatan Standar, Catatan Ringkas, Angka & Simbol, Alat Cepat/Menggambar, Tema Terang,
Papan Penuh, Warna Pena Tetap, Papan Tulis Leluasa). Semua bisa diubah, disalin (duplikat),
diganti nama, dipulihkan ke bawaan, atau dihapus.

### Privasi
- **100% luring.** Tidak ada izin internet, tidak ada analitik, tidak ada pengiriman data.
- Izin hanya `WRITE_EXTERNAL_STORAGE` dengan `maxSdkVersion="28"` untuk menyimpan PNG di Android lama
  (Android 10+ memakai MediaStore tanpa izin tambahan).

---

## 2. Memasang di perangkat

1. Unduh APK dari halaman **[Releases](../../releases)** (dibangun otomatis oleh GitHub Actions),
   atau dari tab **Actions › Android CI › artifact `penboard-apk`**. Pilih sesuai kebutuhan:
   - `Penboard-…-offline-release.apk` → ringan, tanpa izin internet (papan tulis + tombol).
   - `Penboard-…-ink-release.apk` → **Penboard OCR**, ada *Tulisan → teks* (unduh model bahasa saat pertama dipakai).
   - akhiran `-debug` untuk versi uji.
2. Pasang APK (kedua varian bisa dipasang berdampingan karena nama paketnya berbeda).
3. Buka aplikasi **Penboard** → kartu *Status keyboard* → **Pengaturan sistem** → aktifkan **Penboard**
   pada daftar keyboard → kembali → **Pilih keyboard** → pilih **Penboard**.
4. Ketuk kolom teks di aplikasi mana pun; papan tulis Penboard akan muncul.
5. Ingin mencoba lebih dulu? Buka **Uji coba** di aplikasi.

> Cara tercepat mengenali papan sudah aktif: notifikasi keyboard akan menampilkan nama aplikasi
> **Penboard**, dan papan tulis muncul di bagian bawah layar.

---

## 3. Cara memakai sehari-hari

- **Menulis**: tempelkan stylus dan tulis di papan. Garis akan naik otomatis setelah baris bawah terisi.
- **Menghapus**: tekan tombol *alat penghapus* (atau balik ujung penghapus stylus), lalu sentuh goresan yang ingin dihapus; atau *hapus goresan terakhir* / *bersihkan papan*.
- **Mengirim teks & simbol**: tombol teks (mis. `.` `,` `?`), spasi, entri, dan hapus huruf bekerja pada
  kolom teks aplikasi yang sedang aktif. Geser kursor juga tersedia.
- **Menyimpan tulisan**: tombol *Simpan gambar* → tersimpan di Galeri.
- **Ganti profil cepat**: tombol *Ganti profil papan* berputar ke profil berikutnya.

---

## 4. Menyesuaikan keyboard

Buka **Penboard → pilih papan → Ubah**. Ada dua tab:

**Tab “Tombol”**
1. Pilih baris yang ingin diedit (*Laci tombol N* atau *Baris bawah papan N*).
2. `Tambah tombol` → pilih **Aksi**, isi **Teks** (untuk aksi teks/simbol), atur **Lebar relatif**
   (mis. 0,7 ×) dan **Isi sisa ruang** (mis. spasi 30%), serta opsi **Ulangi saat ditahan**
   (nyaman untuk hapus huruf).
3. Atur urutan tombol dengan `◀ ▶`, urutan/posisi baris dengan `Naik/Turun`, dan tambah/hapus baris.

**Tab “Tampilan & Ukuran”** — tinggi papan, jarak baris, ukuran tombol, transparansi, warna,
sakelar garis panduan, penolakan jari, geser otomatis, dan posisi laci tombol.
Semua perubahan **langsung tersimpan** dan tampak di pratinjau (pratinjau papan juga bisa ditulisi).

---

## 5. Struktur proyek

```
app/src/main/java/dev/retrobit/penboard/
├── data/            # model pengaturan (profil), preset bawaan, penyimpanan JSON, warna
│   ├── PenboardConfig.kt     # KeyAction, KeyDef, RowDef, PenboardConfig (+ JSON)
│   ├── Presets.kt            # 6 preset siap pakai
│   ├── PresetRepository.kt   # simpan/muat profil (SharedPreferences) + isi papan
│   ├── BoardStore.kt         # berkas goresan per profil (files/boards)
│   ├── GlobalPrefs.kt        # preferensi global (getar, panduan)
│   └── Colors.kt             # transparansi, campur warna, palet, hex
├── util/
│   ├── KeyLayout.kt          # perhitungan lebar tombol (fungsi murni + unit test)
│   └── BoardMath.kt          # jarak baris, geser otomatis, geser tampilan (fungsi murni + unit test)
├── board/           # mesin gambar papan tulis
│   ├── BoardView.kt          # kanvas stylus: tekanan, penghapus, undo/redo, geser otomatis
│   ├── Stroke.kt             # goresan (titik, batas, uji tabrak penghapus) — bebas Android
│   ├── StrokePaths.kt        # cache Path untuk menggambar goresan
│   ├── StrokeCodec.kt        # simpan/muat goresan (format teks ringkas)
│   ├── KeyButton.kt          # tombol kustom: label/ikon, status aktif, ulangi saat ditahan
│   ├── KeyIcons.kt           # ikon vektor untuk tiap aksi
│   └── BoardExporter.kt      # simpan papan ke PNG (Galeri)
├── ink/             # pengenalan tulisan (tulisan → teks)
│   ├── InkCapture.kt           # ambil goresan dari baris terakhir (fungsi murni + unit test)
│   └── RecognitionLanguages.kt # daftar bahasa tulisan
├── ime/             # keyboard-nya sendiri
│   └── PenboardInputService.kt  # InputMethodService + aksi tombol
└── ui/              # tampilan aplikasi
    ├── KeyRowsBuilder.kt     # menyusun baris tombol dari pengaturan
    ├── Preview.kt            # pratinjau papan + tombol
    ├── Ui.kt                 # pembantu tampilan (kartu, slider, sakelar, pilih warna)
    └── ColorPickerDialog.kt  # dialog warna (palet + hex)

MainActivity.kt          # dasbor: status IME, papan aktif, daftar papan, preferensi, panduan
BoardEditorActivity.kt   # editor tombol, ukuran, transparansi, warna, tata letak
TestActivity.kt          # halaman uji coba
```

Alur data: `PresetRepository` (JSON di SharedPreferences) → `PenboardConfig` → dipakai oleh
`PenboardInputService` (keyboard) dan `BoardEditorActivity` (editor). Semua perhitungan lebar tombol
memakai `KeyLayout` sehingga tampilan di editor, pratinjau, dan keyboard selalu identik.

---

## 6. Membangun sendiri

Prasyarat: **JDK 17** dan Android SDK (compileSdk 35, minSdk 26). Wrapper Gradle 8.11.1 sudah disertakan.

```bash
# APK varian ringan (tanpa OCR)
./gradlew :app:assembleOfflineDebug
# APK varian dengan pengenalan tulisan
./gradlew :app:assembleInkRelease
# Unit test kedua varian
./gradlew :app:testOfflineDebugUnitTest :app:testInkDebugUnitTest
```

Hasil ada di `app/build/outputs/apk/<varian>/<tipe>/` — mis. `offline/release/app-offline-release.apk`.

### Build otomatis di GitHub
Workflow `.github/workflows/android.yml` berjalan pada setiap push/PR:
unit test kedua varian → build APK **offline** & **ink** (debug + release) → unggah artifact `penboard-apk`.
Bila sebuah **tag** `v*` di-push, APK otomatis dilampirkan ke halaman Releases.

```bash
git tag v1.0.0 && git push origin v1.0.0     # contoh rilis
```

Penandatanganan rilis dengan kunci sendiri: buat `keystore.properties` di akar proyek
(diabaikan Git):

```properties
storeFile=rilis.jks
storePassword=…
keyAlias=…
keyPassword=…
```

---

## 7. Pengujian

`./gradlew :app:testOfflineDebugUnitTest :app:testInkDebugUnitTest` menjalankan:
- `KeyLayoutTest` — perhitungan lebar tombol (lebar relatif, isi sisa ruang, sisa pembulatan).
- `BoardMathTest` — jumlah baris, geser otomatis, batas geser tampilan, konversi koordinat.
- `StrokeTest` — titik goresan, batas, dan uji tabrak penghapus.
- `StrokeCodecTest` — goresan tetap utuh setelah disimpan/dimuat, data rusak diabaikan dengan aman.
- `InkExtractorTest` — pengambilan goresan untuk pengenalan (baris terakhir, koordinat, batas jumlah).
- `PenboardConfigTest` — putaran JSON preset, sanitasi nilai ekstrem, duplikasi profil, warna.

---

## 8. Catatan & batasan

- **Pengenalan tulisan tersedia pada varian `ink`** dan memakai ML Kit Digital Ink. Agar aplikasi
  bawaan tetap ringan dan sepenuhnya luring, mesin pengenalan **tidak** disertakan pada varian
  `offline`; tombol pengenalan pada varian itu hanya menampilkan keterangan.
- Model bahasa (±20 MB per bahasa) diunduh sekali saat pertama dipakai; pengenalan berikutnya
  berjalan di perangkat **tanpa koneksi**. Tidak ada tulisan yang dikirim ke server.
- Pintasan tombol/ujung penghapus pena hanya bekerja bila stylus & aplikasi mengirimkan informasi
  tombol (M-Pencil umumnya mengirim `TOOL_TYPE_ERASER` pada ujung penghapusnya; sebagian stylus
  mengirim `BUTTON_STYLUS_PRIMARY`).
- Papan menggambar di area yang terlihat; tulisan yang lebih lama tetap tersimpan dan tampilannya
  bisa ditelusuri dengan tombol *Lihat tulisan lama* / *Kembali ke baris baru* (atau urungkan).
- Tulisan disimpan otomatis setiap ada perubahan (goresan, penghapusan, dan pergeseran papan).

## 9. Lisensi

MIT — lihat berkas [LICENSE](LICENSE).

---

### English summary

RetroBit Penboard is a native Android **stylus writing-board keyboard** (not QWERTY), made for
Huawei M-Pencil and similar pens. Everything is configurable — board size, transparency levels,
which buttons appear, and their layout (relative width, flex-to-fill, repeat-on-hold, toolbar
position, rows). It supports pen pressure, side-button undo, eraser-tip erase, palm rejection,
automatic paper shift, undo/redo, and PNG export. Writing is **saved automatically per board**, can be
**scrolled back** to review earlier lines, and each board can hold its **own fixed pen-colour palette**, and can **turn handwriting into text** through ML Kit Digital Ink
(including Indonesian). Two APKs are published: a lean **offline** build (no internet permission) and
an **ink** build with handwriting recognition (downloads a ~20 MB language model once, then works
on-device). APKs are built and published by GitHub Actions on every push.
