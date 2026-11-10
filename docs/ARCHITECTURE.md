# Arquitetura

## Escopo inicial

O primeiro alvo é o Motorola Edge 30 Fusion (`tundra`) com LineageOS 24.0 / Android 17 (API 37). O projeto permanece separado do Argosy Launcher. O frontend será selecionável; Argosy é uma opção, não uma dependência obrigatória.

A primeira entrega Android é um APK de diagnóstico para validar a enumeração e os eventos de controles no aparelho. Ele observa dispositivos somente enquanto a tela do aplicativo está ativa. Não inicia frontend, não substitui o launcher e não é a implementação final de inicialização automática em background.

O APK de diagnóstico usa compile/target SDK 36 nesta primeira etapa, embora o dispositivo-alvo execute API 37. As APIs de entrada necessárias já existem desde versões anteriores; não é necessário ativar agora as mudanças de comportamento específicas de apps que miram API 37. A elevação do target será feita quando a integração de produção for preparada e testada.

## Componentes

### GameModeService (futuro)

Serviço persistente iniciado pelo sistema. Mantém o observador de dispositivos de entrada ativo e coordena as transições. A estratégia exata de inicialização e o local de empacotamento serão definidos após a auditoria do produto LineageOS.

### GamepadMonitor

Usa `InputManager` e `InputDeviceListener` para observar adição, remoção e alteração de dispositivos. Na inicialização, enumera os IDs com `getInputDeviceIds()`, consulta cada dispositivo com `getInputDevice(id)` e registra o listener com `registerInputDeviceListener()`.

O classificador independente do Android aceita um dispositivo quando:
- `supportsSource(SOURCE_GAMEPAD)` ou `supportsSource(SOURCE_JOYSTICK)`;
- `isExternal == true`, indicando conexão USB, Bluetooth ou outro barramento periférico;
- `isVirtual == false`.

Teclado ou mouse sem fontes GAMEPAD/JOYSTICK são ignorados. Fontes combinadas não desqualificam um controle. O monitor reavalia o conjunto completo após cada evento, em vez de confiar apenas no último evento recebido.

A API `isExternal` não distingue USB de Bluetooth; ela apenas identifica dispositivos conectados por USB, Bluetooth ou outro barramento periférico. O diagnóstico não deve apresentar o tipo de transporte como se tivesse sido confirmado.

Referências oficiais:
- [InputManager API](https://developer.android.com/reference/android/hardware/input/InputManager)
- [InputDevice API](https://developer.android.com/reference/android/view/InputDevice)

### APK de diagnóstico (implementado nesta fase)

O módulo `app` contém uma Activity simples que:
- enumera dispositivos existentes ao abrir;
- registra `InputDeviceListener` enquanto está em primeiro plano;
- atualiza a lista em adições, remoções e alterações;
- mostra dispositivos aceitos e ignorados, motivo, fontes, IDs de fabricante/produto e indicador externo;
- não solicita permissões de runtime nem executa ações em background.

O módulo `core` contém `ControllerClassifier` e testes unitários para a classificação, sem depender de classes Android.

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

### FrontendLauncher (futuro)

Resolve o frontend configurado e o inicia apenas quando as condições de segurança permitirem. Não presume pacote ou atividade do Argosy sem confirmação da instalação. A seleção do frontend deve ser configurável.

### Retorno ao launcher (futuro)

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

- Diagnóstico: registrar listener em `onStart`, enumerar dispositivos existentes e removê-lo em `onStop`.
- Controle conectado com tela apagada: o serviço futuro deve aguardar a tela ficar interativa.
- Tela bloqueada: não abrir frontend nem sobrepor a tela de bloqueio.
- Vários controles: manter o modo ativo até a remoção do último.
- Reconexão durante o período de tolerância: cancelar a saída.
- Reinicialização: enumerar dispositivos já conectados e reconstruir o estado.
- Frontend ausente ou desinstalado: registrar erro e manter o launcher padrão funcional.
- Falha ao iniciar o frontend: não deixar o controlador preso em estado de entrada.

## Fases

1. Documentação e auditoria do device tree.
2. Núcleo independente com máquina de estados e testes unitários.
3. APK de diagnóstico Android e validação física de controles no Edge 30 Fusion.
4. Integração de build com LineageOS, após validar partição, permissões e SELinux.
5. Testes em dispositivo e refinamento de reconexão/retorno HOME.

Nenhuma fase implica flash automático no aparelho.
