# Integração do serviço na ROM

Estado: **não testado em aparelho.** O CI só compila o APK.

## Arquivos

- APK: `service-debug.apk` (artefato do CI `android-game-mode-service-debug`). Para release, assinar com a chave da ROM.
- Allowlist: `service/rom/privapp-permissions-com.izzyn0okami.gamemode.service.xml`.

## Instalação (ROM própria, LineageOS 24 / Android 17)

1. Copiar o APK para `system_ext/priv-app/GameModeService/GameModeService.apk`.
2. Copiar o XML para `system_ext/etc/permissions/`.
3. Declarar o módulo no device tree (`PRODUCT_PACKAGES`) com `privileged: true` e `system_ext_specific: true`.
4. Compilar a imagem completa e instalar o Argosy como app comum (usuário ou também em `system_ext`).

Sem allowlist, uma permissão privilegiada não concedida pode impedir o boot de ROMs estritas; por isso o XML deve ir junto.

## Validar no aparelho

- `adb shell dumpsys package com.izzyn0okami.gamemode.service | grep START_ACTIVITIES_FROM_BACKGROUND` deve mostrar `granted=true`.
- Conectar um controle com a tela desbloqueada: o Argosy deve abrir.
- Desconectar: após 3 s aparece o diálogo; "Encerrar" volta à HOME, "Ficar" mantém.
- Repetir com a tela em segundo plano (app qualquer aberto) para confirmar que o BAL é permitido.

## Pacote do frontend

`./gradlew :service:assembleDebug -PfrontendPackage=com.nendo.argosy.debug` para o build debug do Argosy.
