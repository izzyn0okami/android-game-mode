# Módulo Magisk (instalação sem alterar a partição)

Estado: **não testado em aparelho.** O CI só gera o zip.

O módulo sobrepõe dois arquivos em `/system_ext` sem gravar na partição:

- `priv-app/GameModeService/GameModeService.apk`
- `etc/permissions/privapp-permissions-com.izzyn0okami.gamemode.service.xml`

O APK do módulo é o build debug (assinado com a chave de debug), suficiente para teste.

## Antes de instalar

1. Guarde a imagem de boot original (`boot.img` ou `init_boot.img`) do build instalado, para restaurar com `fastboot`.
2. Tenha o Argosy instalado.
3. Confirme que o Magisk em uso suporta Android 17 (Magisk v31.0 anuncia suporte).

## Instalar

1. Baixe o artefato `android-game-mode-magisk-module` do CI e extraia `gamemode-service-magisk.zip`.
2. Magisk → Módulos → Instalar do armazenamento → escolha o zip → reinicie.

## Validar

Siga a lista de `docs/ROM_INTEGRATION.md` (permissão `granted=true`, Argosy abre ao conectar, diálogo ao desconectar, teste com outro app aberto).

## Recuperar

- Desative ou remova o módulo no Magisk e reinicie.
- Se o aparelho não der boot: no recovery, apague `/data/adb/modules/gamemode-service` (ou inicie no modo seguro do Magisk) ou grave de volta a imagem de boot original com `fastboot flash`.
