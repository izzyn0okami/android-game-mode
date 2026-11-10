# Arquitetura

## Escopo

O primeiro alvo é o Motorola Edge 30 Fusion (`tundra`) com LineageOS 24.0 / Android 17 (API 37). O projeto permanece separado do Argosy Launcher. O frontend será selecionável; Argosy é apenas uma opção, não uma dependência obrigatória.

O APK de diagnóstico foi validado fisicamente no aparelho: a conexão e a desconexão do controle são reconhecidas corretamente. A integração de produção continua separada dessa prova de conceito.

## Componentes

### Núcleo independente (`core`)

A máquina de estados não depende de APIs Android. Ela serializa as decisões de entrada e saída, mantém a tolerância de desconexão e evita duplicação de inicialização.

Estados:

`IDLE -> WAITING_FOR_INTERACTIVE_UNLOCKED -> ENTERING -> ACTIVE -> EXIT_PENDING -> EXITING`

Ações emitidas:
- iniciar frontend;
- agendar/cancelar período de tolerância;
- solicitar retorno à atividade HOME padrão.

### Classificador e monitor Android

O classificador aceita dispositivos externos, não virtuais, que anunciam `SOURCE_GAMEPAD` ou `SOURCE_JOYSTICK`. Teclados e mouses sem fontes de jogo são ignorados.

O módulo `runtime` contém `GamepadMonitor`, que enumera dispositivos existentes, registra `InputDeviceListener`, reenumera o conjunto após alterações e entrega snapshots classificados. O APK de diagnóstico usa esse mesmo monitor.

### GameModeController (implementado; integração de produção pendente)

O `GameModeController` é o adaptador Android: coordena `GamepadMonitor`, observa `SCREEN_ON`, `SCREEN_OFF` e `USER_PRESENT`, consulta `PowerManager.isInteractive` e `KeyguardManager.isDeviceLocked`. O novo `GameModeCoordinator` no módulo `core` concentra transições, callbacks assíncronos e temporizador através de interfaces injetáveis de scheduler e efeitos, permitindo testes determinísticos sem Android.

As operações de frontend e HOME são injetadas por `GameModeActionHandler`. O controlador não inicia Activities diretamente: isso mantém a política de execução separada e permite validar as restrições de background activity launch do Android 17 antes de fornecer um executor privilegiado.

O período padrão de tolerância é 3 segundos e pode ser configurado na construção. `stop()` cancela o temporizador, interrompe o monitor e reinicializa a máquina de estados. Cada ciclo de vida recebe uma geração; callbacks e temporizadores atrasados de ciclos anteriores são descartados. O executor de frontend deve ser idempotente e reutilizar a instância/tarefa existente quando apropriado, pois uma reinicialização do proprietário não deve criar janelas duplicadas. O controlador não cria um serviço persistente e ainda não está ligado ao APK de diagnóstico nem à ROM.

### Serviço persistente e executores (futuros)

O serviço integrado à ROM será o proprietário do monitor e do controlador durante sua vida útil. Deverá reconstruir o estado após boot enumerando controles já conectados.

O executor de frontend resolverá o pacote/atividade configurados, sem depender do Argosy. A navegação de saída usará a resolução da atividade HOME padrão, sem fixar o pacote Lawnchair.

## Segurança e integração com a ROM

Um APK comum não tem garantia de poder iniciar uma Activity a partir do background no Android 17. A permissão `android.permission.START_ACTIVITIES_FROM_BACKGROUND` é protegida pela plataforma e não deve ser solicitada como permissão comum de runtime.

Antes da integração:
- confirmar partição e mecanismo de instalação do pacote;
- confirmar allowlist privilegiada na mesma partição;
- conceder somente permissões justificadas;
- validar SELinux;
- validar regras de início de atividade em background e de serviços no Android 17;
- compilar imagem completa antes de qualquer instalação no aparelho.

A auditoria inicial identificou `system_ext` disponível, mas ainda não decidiu que seja o destino correto. Não alterar `system_server` no MVP e não modificar diretamente os repositórios oficiais LineageOS sem necessidade demonstrada.

## Fases

1. Documentação e auditoria inicial do device tree.
2. Núcleo independente e testes unitários.
3. APK de diagnóstico e validação física do controle — concluídos.
4. Monitor Android reutilizável — concluído e integrado.
5. Coordenador independente testável e adaptador Android — implementados nesta etapa; CI pendente.
6. Serviço persistente, executores seguros e integração privilegiada — pendentes.
7. Build de ROM de teste, validação e instruções de recuperação.

Nenhuma fase implica flash automático no aparelho.

### Referências Android 17

- [Android 17 behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17)
- [Background activity starts](https://developer.android.com/guide/components/activities/background-starts)
- [Android 17 framework manifest](https://android.googlesource.com/platform/frameworks/base/+/android17-release/core/res/AndroidManifest.xml)

## Requisitos de ciclo de vida e falhas

- Com a tela apagada, aguardar até que o dispositivo fique interativo.
- Com a tela bloqueada, não iniciar nem sobrepor o frontend à tela de bloqueio.
- Manter o modo ativo enquanto houver ao menos um controle elegível.
- Reconexão durante a tolerância deve cancelar a saída.
- Reconexão depois que a navegação HOME começou deve ser tratada após a conclusão dessa navegação.
- Após boot, enumerar controles já conectados antes de decidir o estado.
- Se o frontend estiver ausente ou falhar ao iniciar, manter o launcher padrão funcional e evitar ciclos de tentativas duplicadas.
- A integração final deve validar também as restrições de inicialização de serviços em background.
