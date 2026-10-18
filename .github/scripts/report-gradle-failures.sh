#!/usr/bin/env bash
# Melaporkan penyebab kegagalan build agar mudah dibaca:
#  - anotasi GitHub (maksimal 10 baris penting) yang bisa dibaca lewat API check-run
#  - ringkasan langkah ($GITHUB_STEP_SUMMARY) berisi baris penting + ekor log
set -uo pipefail

log_file="${1:-}"
label="${2:-build}"
skip="${3:-0}"

if [ -z "$log_file" ] || [ ! -f "$log_file" ]; then
  echo "::error title=${label}::berkas log tidak ditemukan"
  exit 0
fi

pattern='^(e: |w: )|error:|Error:|Expected|AssertionError|FAILED|What went wrong|Execution failed|Caused by|Unresolved reference|Type mismatch|Cannot|overload resolution'

emit_annotation() {
  local line="$1"
  line="${line//%/%25}"
  line="${line//$'\r'/}"
  printf '::error title=%s::%s\n' "$label" "${line:0:900}"
}

mapfile -t all_lines < <(grep -E "$pattern" "$log_file" | head -n 200)
mapfile -t important < <(printf '%s\n' "${all_lines[@]:$skip}")

if [ "${#important[@]}" -eq 0 ]; then
  mapfile -t important < <(tail -n 20 "$log_file")
fi

count=0
for line in "${important[@]}"; do
  [ -z "${line//[[:space:]]/}" ] && continue
  emit_annotation "$line"
  count=$((count + 1))
  [ "$count" -ge 10 ] && break
done

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "## Kegagalan: ${label}"
    echo ""
    echo "### Baris penting"
    echo '```text'
    printf '%s\n' "${important[@]:0:60}"
    echo '```'
    echo ""
    echo "### Ekor log"
    echo '```text'
    tail -n 120 "$log_file"
    echo '```'
  } >> "$GITHUB_STEP_SUMMARY"
fi

exit 0
