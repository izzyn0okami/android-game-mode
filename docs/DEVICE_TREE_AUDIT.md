# Auditoria inicial do device tree: tundra / lineage-24.0

Data da consulta: 2026-11-09.

Fonte examinada: [LineageOS/android_device_motorola_tundra](https://github.com/LineageOS/android_device_motorola_tundra/tree/lineage-24.0), branch `lineage-24.0`.

## Evidências verificadas

- `lineage_tundra.mk` herda `device/motorola/tundra/device.mk` e define `PRODUCT_DEVICE := tundra`.
- `device.mk` herda `device/motorola/sm7325-common/common.mk`.
- `device.mk` já organiza pacotes via `PRODUCT_PACKAGES` e cópias de arquivos via `PRODUCT_COPY_FILES`.
- `BoardConfig.mk` define `TARGET_SYSTEM_EXT_PROP := device/motorola/tundra/system_ext.prop`.
- O arquivo `system_ext.prop` existe, mas contém apenas uma propriedade de display; isso confirma o uso de propriedades system_ext, não a estratégia correta para instalar este serviço.
- `Android.bp` declara um namespace Soong e importa `hardware/motorola`.
- `init/init.mmi.overlay.rc` contém serviços e permissões de sysfs específicos do dispositivo; não há justificativa para inserir o Game Mode nesse init RC neste estágio.
- O produto herda componentes comuns de `sm7325-common`; portanto, a decisão final de integração depende também dessa árvore compartilhada e da configuração global da ROM.

## Conclusão provisória

Não modificar diretamente o repositório oficial LineageOS nem assumir que basta copiar um APK para `system_ext`.

O caminho de integração deve ser um overlay/manifesto de produto mantido neste projeto, ou um patch separado e revisável para a árvore de dispositivo. Antes disso, inspecionar `sm7325-common`, regras de instalação de apps privilegiados, allowlist e SELinux na branch exata.

A presença de `TARGET_SYSTEM_EXT_PROP` não prova que um app privilegiado deva ser instalado em `system_ext`. O pacote e sua allowlist precisam ser instalados na partição compatível, segundo as regras do build LineageOS usado.

## Pontos ainda não confirmados

- Partição final do APK.
- Permissões privilegiadas mínimas necessárias.
- Necessidade real de API privilegiada para a abertura do frontend no Android 17.
- Regras SELinux necessárias (se houver).
- Se o serviço pode operar como app de sistema isolado sem alteração de `system_server`.

## Próximas verificações

1. Auditar `device/motorola/sm7325-common` na branch correspondente.
2. Examinar os makefiles e exemplos LineageOS de instalação de apps privilegiados e allowlists.
3. Construir primeiro a lógica independente e os testes unitários.
4. Só então criar o fragmento de integração de produto e compilar uma imagem de teste.
