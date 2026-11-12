#!/usr/bin/env bash
# Usage: magisk/build-module.sh <service-apk> <output-zip>
set -euo pipefail
apk="$(realpath "$1")"; out="$(realpath -m "$2")"
root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/system/system_ext/priv-app/GameModeService" "$work/system/system_ext/etc/permissions"
cp "$root/magisk/module.prop" "$work/module.prop"
cp "$apk" "$work/system/system_ext/priv-app/GameModeService/GameModeService.apk"
cp "$root/service/rom/privapp-permissions-com.izzyn0okami.gamemode.service.xml" "$work/system/system_ext/etc/permissions/"
(cd "$work" && zip -qr "$out" .)
