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

Se um controle reaparecer enquanto a navegação HOME já estiver em andamento, a máquina aguarda a conclusão dessa navegação e então solicita nova entrada, desde que a tela continue interativa e desbloqueada. Se o controle desaparecer enquanto a máquina aguarda o desbloqueio, o estado volta para `IDLE`.

### Classificador (`core`)

Um dispositivo é elegível quando é externo, não virtual e anuncia `SOURCE_GAMEPAD` ou `SOURCE_JOYSTICK`. Teclado/mouse sem fontes de jogo são ignorados. Fontes combinadas não desqualificam um controle.

### Monitor Android (`runtime`)

O módulo Android `runtime` contém `GamepadMonitor`, responsável por:
- enumerar os dispositivos já conectados ao iniciar;
- registrar `InputDeviceListener` no `InputManager`;
- reenumerar o conjunto completo após adição, remoção ou alteração;
- classificar cada snapshot com o classificador do núcleo;
- entregar a lista atualizada ao componente proprietário.

O monitor é reutilizável e não contém política de inicialização de frontend, navegação HOME ou serviço persistente. O APK de diagnóstico usa esse mesmo monitor, evitando manter uma segunda implementação da detecção.

`start()` e `stop()` devem ser chamados no thread principal e pareados com o ciclo de vida do proprietário. No produto final, o proprietário será o serviço integrado à ROM.

### Serviço e controlador de produção (futuros)

O serviço persistente ainda não foi criado. Ele deverá:
- possuir o monitor durante a vida do serviço;
- receber o estado de tela interativa e bloqueio;
- serializar os eventos e temporizadores;
- executar as ações da máquina de estados;
- reconstruir o estado após boot enumerando controles já conectados.

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
4. Monitor Android reutilizável — em implementação nesta etapa.
5. Serviço persistente, executor de frontend e integração privilegiada — pendentes.
6. Build de ROM de teste, validação e instruções de recuperação.

Nenhuma fase implica flash automático no aparelho.

### Restrições específicas do Android 17

Um APK comum não tem garantia de poder iniciar uma Activity a partir do background. A permissão `android.permission.START_ACTIVITIES_FROM_BACKGROUND` é protegida pela plataforma (`@SystemApi`/oculta) e não deve ser solicitada como permissão comum de runtime. A estratégia de execução do frontend precisa ser validada para o nível de privilégio e o contexto reais do serviço.

Referências:
- [Android 17 behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17)
- [Background activity starts](https://developer.android.com/guide/components/activities/background-starts)
- [Android 17 framework manifest](https://android.googlesource.com/platform/frameworks/base/+/android17-release/core/res/AndroidManifest.xml)

### Requisitos de ciclo de vida e falhas

- Com a tela apagada, aguardar até que o dispositivo fique interativo.
- Com a tela bloqueada, não iniciar nem sobrepor o frontend à tela de bloqueio.
- Manter o modo ativo enquanto houver ao menos um controle elegível.
- Reconexão durante a tolerância deve cancelar a saída.
- Reconexão depois que a navegação HOME começou deve ser tratada após a conclusão dessa navegação.
- Após boot, enumerar controles já conectados antes de decidir o estado.
- Se o frontend estiver ausente ou falhar ao iniciar, manter o launcher padrão funcional e evitar ciclos de tentativas duplicadas.
- A integração final deve validar também as restrições de inicialização de serviços em background.
