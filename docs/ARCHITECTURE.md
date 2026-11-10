# Arquitetura

## Escopo inicial

O primeiro alvo é o Motorola Edge 30 Fusion (`tundra`) com LineageOS 24.0 / Android 17. O projeto permanece separado do Argosy Launcher. O frontend será selecionável; Argosy é apenas uma opção, não uma dependência obrigatória.

A primeira entrega instalável será um APK de diagnóstico para validar a detecção de controles no aparelho. Esse APK não será tratado como implementação final de inicialização automática em background: a integração de produção depende do componente privilegiado na ROM.

## Componentes

### GameModeService

Serviço persistente iniciado pelo sistema. Mantém o observador de dispositivos de entrada ativo e coordena as transições. A estratégia exata de inicialização e o local de empacotamento serão definidos após a auditoria do produto LineageOS.

### GamepadMonitor

Usa `InputManager` e `InputDeviceListener` para observar adição, remoção e alteração de dispositivos. Na inicialização, enumera os IDs com `getInputDeviceIds()`, consulta cada dispositivo com `getInputDevice(id)` e registra o listener com `registerInputDeviceListener()`.

Critérios para um controle físico elegível:
- `supportsSource(SOURCE_GAMEPAD)` ou `supportsSource(SOURCE_JOYSTICK)`;
- `isExternal == true`, para limitar a USB/Bluetooth e outros barramentos periféricos;
- `isVirtual == false`;
- não considerar teclado ou mouse isoladamente como controle;
- reavaliar o conjunto completo após cada evento, em vez de confiar apenas no último evento recebido.

As fontes GAMEPAD e JOYSTICK podem coexistir no mesmo dispositivo. USB e Bluetooth são tratados pelo mesmo caminho, sem inferir a conexão pelo nome do dispositivo.

Referências oficiais:
- [InputManager API](https://developer.android.com/reference/android/hardware/input/InputManager)
- [InputDevice API](https://developer.android.com/reference/android/view/InputDevice)

### GameModeController

A máquina de estados implementada no módulo `core` usa:

`IDLE -> WAITING_FOR_INTERACTIVE_UNLOCKED -> ENTERING -> ACTIVE -> EXIT_PENDING -> EXITING`

Responsabilidades:
- serializar eventos;
- impedir abertura duplicada;
- aguardar tela interativa e dispositivo desbloqueado;
- aplicar período de tolerância para desconexões transitórias;
- manter o modo ativo enquanto existir ao menos um controle elegível;
- cancelar a saída se outro controle aparecer durante a tolerância;
- retornar a `IDLE` após a navegação HOME ser concluída.

A máquina não depende de APIs Android; temporizadores, listeners e operações de Activity ficam fora do módulo `core`.

### FrontendLauncher

Resolve o frontend configurado e o inicia apenas quando as condições de segurança permitirem. Não presume pacote ou atividade do Argosy sem confirmação da instalação. A seleção do frontend deve ser configurável.

### Retorno ao launcher

Quando nenhum controle elegível permanecer conectado, o serviço solicita a navegação para a atividade HOME padrão. Não fixa o pacote Lawnchair: o usuário mantém o Lawnchair configurado como HOME padrão e o Android resolve o destino.

## Segurança e integração com a ROM

Um APK comum não tem garantia de poder iniciar uma Activity a partir do background no Android 17. No manifesto da plataforma Android 17, `android.permission.START_ACTIVITIES_FROM_BACKGROUND` é uma permissão `@SystemApi @hide` protegida por `signature|privileged|vendorPrivileged|oem|verifier|role`. Portanto, não deve ser solicitada como se fosse uma permissão comum de runtime.

Referência de plataforma:
- [Android 17 framework manifest](https://android.googlesource.com/platform/frameworks/base/+/android17-release/core/res/AndroidManifest.xml)

Antes de escolher `system`, `system_ext` ou outra partição, auditar:
- árvore de produto e makefiles do dispositivo;
- configuração de partições e pacotes privilegiados;
- localização correspondente do XML de allowlist;
- permissões e APIs realmente necessárias;
- regras de SELinux e política de inicialização em background.

Não alterar `system_server` no MVP. Preferir componente isolado e privilegiado se a plataforma permitir. A allowlist deve corresponder à partição em que o pacote for instalado. Não declarar a integração pronta até comprovar isso em build real.

## Comportamento de ciclo de vida

- Controle conectado com tela apagada: aguardar a tela ficar interativa.
- Tela bloqueada: não abrir frontend nem sobrepor a tela de bloqueio.
- Vários controles: manter o modo ativo até a remoção do último.
- Reconexão durante o período de tolerância: cancelar a saída.
- Reinicialização: enumerar dispositivos já conectados e reconstruir o estado.
- Frontend ausente ou desinstalado: registrar erro e manter o launcher padrão funcional.
- Falha ao iniciar o frontend: não deixar o controlador preso em estado de entrada.

## Fases

1. Documentação e auditoria do device tree.
2. Núcleo independente com máquina de estados e testes unitários.
3. APK de diagnóstico Android para testar enumeração e eventos de controles no Edge 30 Fusion.
4. Integração de build com LineageOS, após validar partição, permissões e SELinux.
5. Testes em dispositivo e refinamento de reconexão/retorno HOME.

Nenhuma fase implica flash automático no aparelho.
