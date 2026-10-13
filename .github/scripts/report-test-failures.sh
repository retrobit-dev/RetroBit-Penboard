#!/usr/bin/env bash
# Menampilkan rincian tiap uji yang gagal (pesan expected/actual) sebagai anotasi.
# Dipakai setelah tugas uji unit gagal, supaya penyebabnya terbaca lewat check-run.
set -uo pipefail

dir="app/build/test-results"
if [ ! -d "$dir" ]; then
  echo "::warning title=uji::direktori hasil uji tidak ada ($dir)"
  exit 0
fi

python3 - "$dir" <<'PY'
import glob
import sys
import xml.etree.ElementTree as ET

found = 0
for path in sorted(glob.glob(sys.argv[1] + "/**/*.xml", recursive=True)):
    try:
        root = ET.parse(path).getroot()
    except Exception:
        continue
    for case in root.iter("testcase"):
        for failure in list(case.findall("failure")) + list(case.findall("error")):
            name = "{} > {}".format(case.get("classname", "?"), case.get("name", "?"))
            message = (failure.get("message") or "")
            if not message:
                message = (failure.text or "").strip().splitlines()[0] if failure.text else ""
            message = " ".join(message.split())
            print("::error title=uji::{}: {}".format(name, message[:700]))
            found += 1

if found == 0:
    print("::warning title=uji::tidak ada berkas hasil uji yang memuat kegagalan")
PY

exit 0
