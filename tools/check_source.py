"""Pemeriksaan cepat sebelum push:

1. semua `R.string.x` yang dipakai ada di `strings.xml`, dan tidak ada string yang menganggur;
2. seluruh XML di `res/` (termasuk manifest) bisa diurai;
3. tanda kurung setiap berkas Kotlin seimbang;
4. view yang dipakai ulang (diambil dari daftar, mis. `buttons[index]`) dilepas dulu
   dengan `removeView` sebelum `addView` lagi — kalau tidak, Android melempar
   IllegalStateException "The specified child already has a parent" (force close 3.3.0).

Catatan: string dibuang **sebelum** komentar, karena teks seperti "market://..." atau
"https://..." memuat `//` yang bukan komentar. Kesalahan urutan itu pernah membuat
pemeriksaan melaporkan kurung tidak seimbang pada kode yang sebenarnya benar.
"""
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

STRING_LITERAL = re.compile(r'"(?:[^"\\\n]|\\.)*"')
LINE_COMMENT = re.compile(r"//[^\n]*")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.S)


def strip_noise(text: str) -> str:
    """Buang string lalu komentar, supaya tanda kurung di dalamnya tidak dihitung."""
    text = STRING_LITERAL.sub('""', text)
    text = LINE_COMMENT.sub("", text)
    return BLOCK_COMMENT.sub("", text)


# Jarak pemeriksaan ke belakang untuk mencari `removeView` sebelum `addView` (karakter).
LOOKBACK = 4000

LIST_INDEXED_VIEW = re.compile(r"val (\w+) = (\w+)\[[^\]]+\]")


def reused_view_warnings(root: pathlib.Path) -> list:
    """View yang diambil dari daftar lalu dipasang ulang harus dilepas dari induk lama.

    Android tidak mengizinkan satu View punya dua induk: `addView` pada view yang masih
    menempel di induk lama langsung melempar IllegalStateException ("The specified child
    already has a parent"). Inilah penyebab aplikasi force close pada 3.3.0/3.4.0, saat
    tombol grid dipakai ulang tanpa dilepas lebih dulu.

    Pemeriksaannya berbasis **urutan**: sebelum `addView(<nama>)` harus ada
    `removeView(<nama>)` di bagian kode sebelumnya (jarak maksimum [LOOKBACK] karakter).
    """
    warnings = []
    for path in sorted((root / "app/src").rglob("*.kt")):
        text = path.read_text()
        for name, source in LIST_INDEXED_VIEW.findall(text):
            for match in re.finditer(r"addView\(\s*" + name + r"\s*\)", text):
                window = text[max(0, match.start() - LOOKBACK):match.start()]
                if re.search(r"removeView\(\s*" + name + r"\s*\)", window):
                    continue
                warnings.append(
                    f"{path}: '{name}' dari {source} dipasang ulang tanpa removeView lebih dulu"
                )
                break
    return warnings


def main() -> int:
    root = pathlib.Path(".")
    strings = set()
    bad_xml = []
    for path in sorted((root / "app/src/main/res").rglob("*.xml")):
        try:
            ET.parse(path)
        except Exception as error:  # noqa: BLE001 - laporan sederhana
            bad_xml.append(f"{path}: {error}")
        if path.name == "strings.xml":
            strings |= set(re.findall(r'<string name="([^"]+)"', path.read_text()))
    try:
        ET.parse("app/src/main/AndroidManifest.xml")
    except Exception as error:  # noqa: BLE001
        bad_xml.append(f"AndroidManifest.xml: {error}")

    used = set()
    unbalanced = []
    for path in sorted((root / "app/src").rglob("*.kt")):
        text = path.read_text()
        used |= set(re.findall(r"R\.string\.(\w+)", text))
        clean = strip_noise(text)
        for opening, closing in (("{", "}"), ("(", ")"), ("[", "]")):
            if clean.count(opening) != clean.count(closing):
                unbalanced.append(f"{path}: {opening}{closing} {clean.count(opening)}/{clean.count(closing)}")

    # `android.R.string.ok/cancel` bukan milik aplikasi; dan sebagian string hanya
    # dirujuk dari XML (mis. label/description layanan aksesibilitas).
    used.discard("ok")
    used.discard("cancel")
    referenced_in_xml = set()
    for path in sorted((root / "app/src").rglob("*.xml")):
        referenced_in_xml |= set(re.findall(r"@string/([A-Za-z0-9_]+)", path.read_text()))

    missing = sorted(n for n in used if n not in strings)
    unused = sorted(n for n in strings if n not in used and n not in referenced_in_xml)
    print(f"string dipakai: {len(used)} / didefinisikan: {len(strings)}")
    print("MISSING:", missing)
    print("UNUSED:", unused)
    reused = reused_view_warnings(root)

    print("XML BAD:", bad_xml)
    print("KOTLIN UNBALANCED:", unbalanced)
    print("ADDVIEW TANPA REMOVEVIEW:", reused)
    return 1 if (missing or bad_xml or unbalanced or reused) else 0


if __name__ == "__main__":
    sys.exit(main())
