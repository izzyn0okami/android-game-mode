# Módulo Magisk (instalação sem compilar a ROM)

Estado: **não testado em aparelho.** O CI só monta o zip.

O módulo coloca o serviço em `system_ext/priv-app` por sobreposição do Magisk. Nada é gravado na partição do sistema.

## Antes de instalar

1. Guarde uma cópia do `boot.img` (ou `init_boot.img`) original da sua versão do LineageOS. Ele é a forma garantida de desfazer tudo (`fastboot flash boot boot.img`).
2. Confirme que a versão do Magisk suporta o seu Android.

## Instalar

1. Baixe o artefato `gamemode-service-magisk-module` do CI e extraia o zip.
2. Magisk → Módulos → Instalar a partir do armazenamento → escolha `gamemode-service-magisk.zip`.
3. Reinicie.
4. Instale o Argosy normalmente.

## Conferir

- `adb shell dumpsys package com.izzyn0okami.gamemode.service | grep START_ACTIVITIES_FROM_BACKGROUND` deve mostrar `granted=true`.
- Conecte um controle com a tela desbloqueada: o Argosy deve abrir.
- Desconecte: depois de 3 s aparece o diálogo.

## Se algo der errado

- Desative o módulo no app do Magisk e reinicie.
- Se o aparelho não ligar: o Magisk desativa módulos no modo seguro do Magisk; se não bastar, reflasheie o `boot.img` original pelo fastboot.
