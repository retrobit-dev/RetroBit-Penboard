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
terakhir, bersihkan papan, urungkan, ulangi, geser kursor kiri/kanan, simpan gambar, ganti warna pena,
ganti profil papan, alat pena, alat penghapus, sembunyikan papan.

### Siap pakai & bisa disalin
6 preset bawaan (Catatan Standar, Catatan Ringkas, Angka & Simbol, Alat Cepat/Menggambar, Tema Terang,
Papan Penuh). Semua bisa diubah, disalin (duplikat), diganti nama, dipulihkan ke bawaan, atau dihapus.

### Privasi
- **100% luring.** Tidak ada izin internet, tidak ada analitik, tidak ada pengiriman data.
- Izin hanya `WRITE_EXTERNAL_STORAGE` dengan `maxSdkVersion="28"` untuk menyimpan PNG di Android lama
  (Android 10+ memakai MediaStore tanpa izin tambahan).

---

## 2. Memasang di perangkat

1. Unduh APK dari halaman **[Releases](../../releases)** (dibangun otomatis oleh GitHub Actions),
   atau dari tab **Actions › Android CI › artifact `penboard-apk`**.
2. Pasang APK (`app-debug.apk` atau `app-release.apk`).
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
│   ├── PresetRepository.kt   # simpan/muat profil (SharedPreferences)
│   ├── GlobalPrefs.kt        # preferensi global (getar, panduan)
│   └── Colors.kt             # transparansi, campur warna, palet, hex
├── util/KeyLayout.kt # perhitungan lebar tombol (fungsi murni + unit test)
├── board/           # mesin gambar papan tulis
│   ├── BoardView.kt          # kanvas stylus: tekanan, penghapus, undo/redo, geser otomatis
│   ├── Stroke.kt             # goresan (titik, path, uji tabrak penghapus)
│   ├── KeyButton.kt          # tombol kustom: label/ikon, status aktif, ulangi saat ditahan
│   ├── KeyIcons.kt           # ikon vektor untuk tiap aksi
│   └── BoardExporter.kt      # simpan papan ke PNG (Galeri)
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
# APK debug
./gradlew :app:assembleDebug
# APK rilis (ditandatangani kunci debug bila tidak ada keystore.properties)
./gradlew :app:assembleRelease
# Unit test
./gradlew :app:testDebugUnitTest
```

Hasil: `app/build/outputs/apk/debug/app-debug.apk` dan `app/build/outputs/apk/release/app-release.apk`.

### Build otomatis di GitHub
Workflow `.github/workflows/android.yml` berjalan pada setiap push/PR:
unit test → build APK debug + release → unggah artifact `penboard-apk`.
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

`./gradlew :app:testDebugUnitTest` menjalankan:
- `KeyLayoutTest` — perhitungan lebar tombol (lebar relatif, isi sisa ruang, sisa pembulatan).
- `PenboardConfigTest` — putaran JSON preset, sanitasi nilai ekstrem, duplikasi profil, warna.

---

## 8. Catatan & batasan

- Penboard **tidak mengubah tulisan tangan menjadi teks (tanpa OCR)**. Papan dipakai sebagai media
  menulis stylus, sedangkan tombol mengendalikan teks di aplikasi tujuan. Pengenalan tulisan tangan
  luring direncanakan sebagai langkah berikutnya.
- Pintasan tombol/ujung penghapus pena hanya bekerja bila stylus & aplikasi mengirimkan informasi
  tombol (M-Pencil umumnya mengirim `TOOL_TYPE_ERASER` pada ujung penghapusnya; sebagian stylus
  mengirim `BUTTON_STYLUS_PRIMARY`).
- Papan hanya menggambar di area yang terlihat; konten yang lebih lama tetap tersimpan sampai
  dihapus, dan bisa digeser tampilannya lewat urungkan/ulangi.

## 9. Lisensi

MIT — lihat berkas [LICENSE](LICENSE).

---

### English summary

RetroBit Penboard is a native Android **stylus writing-board keyboard** (not QWERTY), made for
Huawei M-Pencil and similar pens. Everything is configurable — board size, transparency levels,
which buttons appear, and their layout (relative width, flex-to-fill, repeat-on-hold, toolbar
position, rows). It supports pen pressure, side-button undo, eraser-tip erase, palm rejection,
automatic paper shift, undo/redo, and PNG export. 100% offline, no internet permission.
APKs are built and published by GitHub Actions on every push.
