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


## Testes do GameModeController (CI/build e integração futura)

- `start()` registra os observadores uma única vez e faz enumeração inicial dos controles.
- `stop()` remove o listener e cancela o temporizador de saída.
- Mudanças de tela/bloqueio atualizam a máquina de estados.
- Tela apagada ou bloqueada impede a solicitação de entrada até que o aparelho esteja interativo e desbloqueado.
- A remoção do último controle agenda a tolerância padrão de 3 segundos.
- Reconexão durante a tolerância cancela o retorno ao HOME.
- A expiração da tolerância solicita HOME uma única vez.
- Falha síncrona ou assíncrona ao iniciar frontend informa falha à máquina de estados.
- A conclusão do retorno ao HOME é comunicada à máquina; uma reconexão durante a navegação é processada depois da conclusão.
- Os callbacks de efeitos são encaminhados ao thread principal.
- O controlador não inicia Activities diretamente; um executor real só será adicionado após a auditoria de BAL e privilégios Android 17.

O controlador desta etapa ainda não é um serviço persistente e não deve ser tratado como funcionalidade pronta para uso diário.
