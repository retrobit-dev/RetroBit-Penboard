#!/usr/bin/env bash
# Mengubah baris penting dari log Gradle yang gagal menjadi anotasi GitHub.
# Anotasi bisa dibaca lewat API check-run, sehingga kegagalan build tetap terlihat
# walau log mentah tidak dapat diunduh.
set -uo pipefail

log_file="${1:-}"
label="${2:-build}"

if [ -z "$log_file" ] || [ ! -f "$log_file" ]; then
  echo "::error title=${label}::berkas log tidak ditemukan"
  exit 0
fi

emit() {
  local line="$1"
  line="${line//%/%25}"
  line="${line//$'\r'/}"
  printf '::error title=%s::%s\n' "$label" "${line:0:900}"
}

pattern='^(e: |w: )|error:|Error:|Expected|AssertionError|FAILED|What went wrong|Execution failed|Caused by|Unresolved reference|Type mismatch'

count=0
while IFS= read -r line; do
  [ -z "${line//[[:space:]]/}" ] && continue
  emit "$line"
  count=$((count + 1))
  [ "$count" -ge 10 ] && break
done < <(grep -E "$pattern" "$log_file" | head -n 10)

if [ "$count" -eq 0 ]; then
  while IFS= read -r line; do
    [ -z "${line//[[:space:]]/}" ] && continue
    emit "$line"
  done < <(tail -n 10 "$log_file")
fi

exit 0
