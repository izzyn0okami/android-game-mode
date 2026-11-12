#!/usr/bin/env python3
"""Monta o zip do módulo Magisk: python3 build_magisk_module.py <apk> <saida.zip>"""
import sys, zipfile
from pathlib import Path

apk, out = Path(sys.argv[1]), Path(sys.argv[2])
root = Path(__file__).resolve().parent.parent
xml = root / "service/rom/privapp-permissions-com.izzyn0okami.gamemode.service.xml"
files = {
    "module.prop": root / "magisk/module.prop",
    "customize.sh": root / "magisk/customize.sh",
    "system/system_ext/priv-app/GameModeService/GameModeService.apk": apk,
    "system/system_ext/etc/permissions/" + xml.name: xml,
}
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for name, src in files.items():
        z.write(src, name)
print("ok", out)
