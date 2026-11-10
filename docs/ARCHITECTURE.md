# Arquitetura

## Escopo inicial

O primeiro alvo é o Motorola Edge 30 Fusion (`tundra`) com LineageOS 24.0 / Android 17. O projeto permanece separado do Argosy Launcher. O frontend será selecionável; Argosy é apenas a opção inicial.

## Componentes

### GameModeService

Serviço persistente iniciado pelo sistema. Mantém o observador de dispositivos de entrada ativo e coordena as transições. A estratégia exata de inicialização e o local de empacotamento serão definidos após a auditoria do produto LineageOS.

### GamepadMonitor

Usa `InputManager` e `InputDeviceListener` para observar adição, remoção e alteração de dispositivos. Na inicialização, também enumera os dispositivos já conectados.

Critérios:
- aceitar dispositivos que anunciem `SOURCE_GAMEPAD` ou `SOURCE_JOYSTICK`;
- não classificar teclado ou mouse como gamepad;
- validar capacidades/fontes para reduzir falsos positivos;
- suportar USB e Bluetooth sem depender do tipo de conexão;
- reavaliar o conjunto completo após cada evento.

### GameModeController

Máquina de estados sugerida:

`IDLE -> CONTROLLER_DETECTED -> ENTERING -> ACTIVE -> EXITING -> IDLE`

Responsabilidades:
- serializar eventos;
- impedir abertura duplicada;
- aguardar tela interativa e dispositivo desbloqueado;
- aplicar debounce/histerese para desconexões transitórias;
- manter o modo ativo enquanto existir ao menos um controle elegível;
- cancelar a saída se outro controle aparecer durante a tolerância.

### FrontendLauncher

Resolve o frontend configurado e o inicia apenas quando as condições de segurança permitirem. Não presume pacote ou atividade do Argosy sem confirmação da instalação. A seleção do frontend deve ser configurável.

### Retorno ao launcher

Quando nenhum controle elegível permanecer conectado, o serviço solicita a navegação para a atividade HOME padrão. Não fixa o pacote Lawnchair: o usuário mantém o Lawnchair configurado como HOME padrão e o Android resolve o destino.

## Segurança e integração com a ROM

Um serviço comum de aplicativo não tem garantia de poder iniciar uma atividade a partir do background no Android 17. A integração privilegiada deve ser comprovada em build real.

Antes de escolher `system`, `system_ext` ou outra partição, auditar:
- árvore de produto e makefiles do dispositivo;
- configuração de partições e pacotes privilegiados;
- localização correspondente do XML de allowlist;
- permissões e APIs realmente necessárias;
- regras de SELinux e política de inicialização em background.

Não alterar `system_server` no MVP. Preferir componente isolado e privilegiado se a plataforma permitir. A allowlist deve corresponder à partição em que o pacote for instalado.

## Comportamento de ciclo de vida

- Controle conectado com tela apagada: aguardar a tela ficar interativa.
- Tela bloqueada: não abrir frontend nem sobrepor a tela de bloqueio.
- Vários controles: manter o modo ativo até a remoção do último.
- Reconexão durante debounce: cancelar a saída.
- Reinicialização: enumerar dispositivos já conectados e reconstruir o estado.
- Frontend ausente ou desinstalado: registrar erro e manter o launcher padrão funcional.

## Fases

1. Documentação e auditoria do device tree.
2. MVP Android isolado: detecção, máquina de estados e testes unitários.
3. Integração de build com LineageOS, após validar partição, permissões e SELinux.
4. Testes em dispositivo e refinamento de reconexão/retorno HOME.

Nenhuma fase implica flash automático no aparelho.
