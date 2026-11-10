# Plano de testes

## Testes unitários da máquina de estados

- Aguardar tela interativa e desbloqueada antes de solicitar entrada.
- Desconectar enquanto aguarda desbloqueio retorna a máquina para `IDLE`.
- Vários controles não iniciam instâncias duplicadas.
- Remover um dentre vários não encerra o modo.
- Remover o último controle inicia a tolerância.
- Reconectar durante a tolerância cancela a saída.
- Reconectar depois de iniciada a navegação HOME solicita nova entrada após a conclusão da navegação.
- Expiração da tolerância sem controles solicita retorno ao HOME.
- Eventos repetidos não causam transições duplicadas.

## Testes unitários do classificador

- Controle externo com fonte GAMEPAD é aceito.
- Controle externo com fonte JOYSTICK é aceito.
- Teclado/mouse sem fontes de jogo são ignorados.
- Dispositivo interno é ignorado mesmo se anunciar fonte de jogo.
- Dispositivo virtual é ignorado.
- Fontes combinadas (gamepad + teclado) continuam elegíveis.

## Teste físico do APK de diagnóstico — aprovado

No Motorola Edge 30 Fusion com LineageOS 24.0, o usuário confirmou que o APK reconhece corretamente o controle ao conectá-lo e desconectá-lo.

## Teste físico do monitor Android reutilizável — aprovado

O usuário instalou e testou o novo APK construído a partir da implementação `runtime` da PR #3 e informou que todos os testes executados foram aprovados no Motorola Edge 30 Fusion com LineageOS 24.0.

A API `isExternal` não identifica o transporte exato: o diagnóstico apenas confirma se o Android classifica o dispositivo como externo. Não interpretar esse campo isoladamente como prova de USB ou Bluetooth.

## Testes do monitor Android

- Enumerar um controle já conectado ao iniciar o monitor.
- Atualizar a lista após adição, remoção e alteração de dispositivo.
- Reenumerar todos os dispositivos em cada evento.
- Confirmar que teclado/mouse sem fontes GAMEPAD/JOYSTICK não contam como controles.
- Confirmar que `stop()` remove o listener e impede novas notificações.
- Confirmar que chamadas repetidas a `start()`/`stop()` não registram ou removem listeners duplicados.

## Testes de ciclo de vida e segurança (serviço futuro)

- Conectar com tela desbloqueada e interativa.
- Conectar com tela apagada: aguardar.
- Conectar com tela bloqueada: não abrir frontend.
- Desbloquear após conexão: iniciar uma única vez.
- Desconectar todos os controles.
- Frontend configurado indisponível.
- Reiniciar o aparelho com controle conectado.
- Alternar rapidamente conexão e desconexão.
- Confirmar que HOME padrão continua sendo o Lawnchair.

## Testes de integração LineageOS

Somente após auditoria da árvore `tundra` na branch `lineage-24.0`:
- validar instalação do pacote na partição escolhida;
- validar allowlist de permissões privilegiadas;
- validar regras SELinux;
- compilar imagem de teste;
- instalar em ambiente recuperável, com backup e imagem de retorno disponíveis;
- coletar logs do serviço e verificar ausência de crashes.

## Critérios de aceite do sistema completo

- Detecção USB e Bluetooth consistente.
- Nenhuma instância duplicada do frontend.
- Retorno ao HOME somente após desconexão do último controle elegível.
- Nenhuma UI é aberta sobre a tela bloqueada.
- Lawnchair permanece como HOME padrão.
- Falhas do frontend não comprometem o launcher ou o boot.


## Testes automatizados do GameModeCoordinator (core)

A suíte usa um scheduler de relógio falso e efeitos injetados; não depende de Android, emulador ou espera real.

- Aguarda tela interativa e desbloqueada antes de iniciar o frontend.
- Mais de um controle não provoca lançamento duplicado.
- Reconexão durante a tolerância cancela o retorno ao HOME.
- A expiração da tolerância solicita HOME uma única vez.
- A conclusão assíncrona do HOME executa a atualização prévia do estado de tela/bloqueio.
- `stop()` cancela a tolerância pendente.
- Callback de temporizador cancelado não pode expirar uma tolerância posterior.
- Callbacks de lançamento e HOME de um ciclo anterior são ignorados após parar e reiniciar.
- Falha no lançamento permite uma nova tentativa sem deixar a máquina presa em `ENTERING`.

A inicialização parcial do `GameModeController` deve remover o receiver e interromper monitor/coordenador quando uma etapa falhar. Foi adicionado um teste instrumentado em emulador para validar `start()`/`stop()` idempotentes, remoção do receiver, reinicialização do controlador e ausência de chamadas de frontend/HOME sem controles. O teste não substitui validação física de controles nem cobre BAL/SELinux. O controlador desta etapa ainda não é um serviço persistente e não deve ser tratado como funcionalidade pronta para uso diário.


## APK de diagnóstico integrado — próxima validação física

O APK agora instancia o `GameModeController` no ciclo de vida da tela de diagnóstico. O controlador usa o `GamepadMonitor` e o `GameModeCoordinator` reais. Para manter o teste seguro, o `GameModeActionHandler` do APK é um executor simulado:
- ao conectar o primeiro controle elegível, registra a solicitação de frontend e confirma sucesso simulado ao coordenador;
- ao desconectar o último controle elegível, respeita a tolerância padrão de 3 segundos e registra o retorno HOME simulado;
- não inicia Activities externas, não resolve nem altera o HOME padrão e não exige permissões privilegiadas;
- interrompe o controlador quando a tela de diagnóstico sai do primeiro plano.

### Roteiro físico
1. Instalar o APK de diagnóstico e abrir sua tela principal.
2. Com a tela desbloqueada, conectar um controle: confirmar a contagem e o evento de solicitação simulada do frontend.
3. Desconectar e reconectar durante os 3 segundos: confirmar que o retorno HOME simulado é cancelado.
4. Desconectar o último controle e aguardar mais de 3 segundos: confirmar o evento de retorno HOME simulado.
5. Repetir com dois controles: remover apenas um e confirmar que o modo permanece ativo; remover o segundo e confirmar o início da tolerância.
6. Sair da tela e voltar: confirmar que o controlador inicia novamente sem listeners ou receivers duplicados.

Este APK valida a integração física do monitor, controlador e coordenador. Ele **não** valida abertura real de frontend, navegação HOME, BAL, SELinux, serviço persistente, comportamento com tela bloqueada/apagada durante uso contínuo ou integração com a ROM.
