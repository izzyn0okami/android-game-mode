# Auditoria inicial do device tree: tundra / lineage-24.0

Data da consulta: 2026-11-09.

Fontes examinadas:
- [LineageOS/android_device_motorola_tundra](https://github.com/LineageOS/android_device_motorola_tundra/tree/lineage-24.0), branch `lineage-24.0`.
- [LineageOS/android_device_motorola_sm7325-common](https://github.com/LineageOS/android_device_motorola_sm7325-common/tree/lineage-24.0), branch `lineage-24.0`.

## Evidências verificadas: árvore tundra

- `lineage_tundra.mk` herda `device/motorola/tundra/device.mk` e define `PRODUCT_DEVICE := tundra`.
- `device.mk` herda `device/motorola/sm7325-common/common.mk`.
- `device.mk` já organiza pacotes via `PRODUCT_PACKAGES` e cópias de arquivos via `PRODUCT_COPY_FILES`.
- `BoardConfig.mk` define `TARGET_SYSTEM_EXT_PROP := device/motorola/tundra/system_ext.prop`.
- O arquivo `system_ext.prop` existe, mas contém apenas uma propriedade de display; isso confirma o uso de propriedades system_ext, não a estratégia correta para instalar este serviço.
- `Android.bp` declara um namespace Soong e importa `hardware/motorola`.
- `init/init.mmi.overlay.rc` contém serviços e permissões de sysfs específicos do dispositivo; não há justificativa para inserir o Game Mode nesse init RC neste estágio.

## Evidências verificadas: árvore compartilhada sm7325-common

- `common.mk` ativa partições dinâmicas (`PRODUCT_USE_DYNAMIC_PARTITIONS := true`).
- `BoardConfigCommon.mk` inclui `system_ext` na lista de partições A/B e no grupo de partições dinâmicas `mot_dp_group`.
- A árvore compartilhada define `TARGET_COPY_OUT_SYSTEM_EXT := system_ext` e também usa `TARGET_SYSTEM_EXT_PROP`.
- Existe um XML de exemplo para permissões privilegiadas de hotword em `configs/product_privapp-permissions-hotword.xml`. Sua presença não demonstra, por si só, que esse arquivo seja copiado para a imagem ou que seja um modelo aplicável ao nosso pacote.
- O produto compartilhado instala diversos componentes via `PRODUCT_PACKAGES`; a integração do Game Mode deverá seguir o mecanismo de build adequado, não copiar o APK manualmente.

## Conclusão provisória

A imagem do dispositivo tem partição `system_ext` disponível, mas isso ainda não basta para decidir instalar o Game Mode nela. A escolha depende do tipo de pacote, das permissões realmente exigidas, do local da allowlist e das regras de instalação do build LineageOS.

Não modificar diretamente os repositórios oficiais LineageOS nem assumir que basta copiar um APK para `system_ext`.

O caminho de integração deve ser um overlay/manifesto de produto mantido neste projeto, ou um patch separado e revisável para a árvore de dispositivo. A integração privilegiada deve ser validada em build completo.

## Pontos ainda não confirmados

- Partição final do APK e da allowlist.
- Permissões privilegiadas mínimas necessárias.
- Necessidade real de API privilegiada para a abertura do frontend no Android 17.
- Regras SELinux necessárias (se houver).
- Se o serviço pode operar como app de sistema isolado sem alteração de `system_server`.

## Próximas verificações

1. Examinar a configuração global LineageOS para apps privilegiados e allowlists.
2. Conferir se há uma via suportada para iniciar o frontend sem conceder privilégios excessivos.
3. Construir primeiro a lógica independente e os testes unitários.
4. Só então criar o fragmento de integração de produto e compilar uma imagem de teste.

## Auditoria complementar: instalação e SELinux (2026-11-10)

Foram reexaminados os arquivos da branch `lineage-24.0`:

- `device/motorola/tundra/device.mk` continua sendo o ponto de inclusão específico do produto: herda `sm7325-common/common.mk` e já utiliza `PRODUCT_PACKAGES` e `PRODUCT_COPY_FILES`.
- `device/motorola/tundra/BoardConfig.mk` herda `sm7325-common/BoardConfigCommon.mk` e adiciona propriedades de `system_ext`; não define uma política SELinux específica do dispositivo.
- `sm7325-common/BoardConfigCommon.mk` declara `system_ext` em `AB_OTA_PARTITIONS`, no grupo dinâmico `mot_dp_group` e em `TARGET_COPY_OUT_SYSTEM_EXT := system_ext`. A imagem de `system_ext` usa ext4.
- O mesmo arquivo define `BOARD_VENDOR_SEPOLICY_DIRS += $(COMMON_PATH)/sepolicy/vendor`. A árvore comum consultada contém essa política vendor; não foi encontrada, nesses arquivos de produto, uma declaração equivalente de diretório SELinux para `system_ext`.
- A listagem da raiz de `android_device_motorola_tundra` não apresenta diretório `sepolicy` específico do aparelho. Isso não exclui políticas herdadas de outros repositórios da build.
- O `Android.bp` comum declara namespace Soong; a instalação deve continuar usando o mecanismo de módulos do build Android, não cópia manual de APK.

### Consequências para o Game Mode

1. `system_ext` é tecnicamente uma partição disponível no produto e no grupo dinâmico; isso torna possível avaliar um módulo de aplicativo privilegiado nela, mas não aprova automaticamente essa escolha.
2. Se o APK for instalado em `system_ext/priv-app`, sua allowlist de permissões privilegiadas deverá ser instalada na mesma partição, no diretório de permissões apropriado, e validada pelo build.
3. A árvore de dispositivo consultada não fornece evidência de que seja necessário criar uma regra SELinux nova para um aplicativo que use apenas APIs Android públicas. A necessidade real só poderá ser decidida após escolher o domínio do processo e observar a política efetiva/denials na imagem de teste.
4. Não adicionar serviço ao `init*.rc` do aparelho apenas para iniciar uma Activity. O mecanismo de inicialização e persistência do processo Android ainda precisa ser projetado e testado separadamente.
5. O modelo de persistência (aplicativo persistente, receiver de boot/serviço ou componente nativo) continua em aberto; não assumir que a permissão BAL, isoladamente, mantém o processo vivo.

### Conclusão atualizada

A partição `system_ext` é uma candidata plausível para o pacote e sua allowlist, e `device/motorola/tundra/device.mk` é o ponto de integração específico caso essa escolha seja aprovada. Ainda não há decisão final de instalação, não há alteração nos repositórios oficiais LineageOS e não há patch de SELinux ou init. Antes de implementar, falta fechar o modelo de persistência, conferir o fluxo de permissões privilegiadas do produto LineageOS e validar o domínio SELinux na imagem completa.
