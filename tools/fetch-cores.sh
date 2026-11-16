#!/usr/bin/env bash
# Downloads the libretro cores embedded in the APK from the libretro buildbot and verifies them
# against the SHA256 pinned in tools/cores.lock (nightly "latest" is NOT immutable: if a hash changes,
# review the new build, then update cores.lock and THIRD_PARTY.md on purpose).
#   tools/fetch-cores.sh           base cores  -> cores/base/<abi>/lib<core>.so
#   tools/fetch-cores.sh --extra   also SNES / Mega Drive cores -> cores/extra/<abi>/
set -euo pipefail
cd "$(dirname "$0")/.."
BASE_URL="https://buildbot.libretro.com/nightly/android/latest"
# abi:core  (gba: gpsp on armv7, mgba on arm64)
BASE="armeabi-v7a:fceumm armeabi-v7a:gambatte armeabi-v7a:gpsp armeabi-v7a:smsplus armeabi-v7a:mednafen_pce_fast
arm64-v8a:fceumm arm64-v8a:gambatte arm64-v8a:mgba arm64-v8a:smsplus arm64-v8a:mednafen_pce_fast"
EXTRA=""
for abi in armeabi-v7a arm64-v8a; do for c in mednafen_supafaust clownmdemu blastem; do EXTRA="$EXTRA $abi:$c"; done; done
[ "${1:-}" = "--extra" ] && WANT="$BASE $EXTRA" || WANT="$BASE"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
for pair in $WANT; do
  abi="${pair%%:*}"; core="${pair##*:}"
  line="$(grep -E "^$abi $core " tools/cores.lock || true)"
  [ -n "$line" ] || { echo "no pin for $abi/$core in tools/cores.lock" >&2; exit 1; }
  zsha="$(echo "$line" | cut -d' ' -f3)"; ssha="$(echo "$line" | cut -d' ' -f4)"
  case "$core" in mednafen_supafaust|clownmdemu|blastem) out="cores/extra/$abi";; *) out="cores/base/$abi";; esac
  mkdir -p "$out"; dest="$out/lib$core.so"
  if [ -f "$dest" ] && [ "$(sha256sum "$dest" | cut -d' ' -f1)" = "$ssha" ]; then echo "ok (cached) $dest"; continue; fi
  d="$tmp/$abi-$core"; mkdir -p "$d"
  curl -sSfL -o "$d/c.zip" "$BASE_URL/$abi/${core}_libretro_android.so.zip"
  [ "$(sha256sum "$d/c.zip" | cut -d' ' -f1)" = "$zsha" ] || { echo "SHA256 mismatch (zip) for $abi/$core" >&2; exit 1; }
  unzip -q -o "$d/c.zip" -d "$d/x"
  so="$d/x/${core}_libretro_android.so"
  [ "$(sha256sum "$so" | cut -d' ' -f1)" = "$ssha" ] || { echo "SHA256 mismatch (so) for $abi/$core" >&2; exit 1; }
  cp "$so" "$dest"; echo "fetched $dest"
done
