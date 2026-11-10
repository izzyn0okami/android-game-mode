# Plano de testes

## Testes unitários da máquina de estados

- Nenhum controle conectado mantém estado ocioso.
- Primeiro controle elegível solicita entrada uma única vez.
- Segundo controle não inicia outra instância.
- Remoção de um controle não encerra o modo se outro continuar conectado.
- Remoção do último controle inicia o período de tolerância.
- Reconexão durante a tolerância cancela a saída.
- Expiração da tolerância sem controles solicita retorno ao HOME.
- Eventos repetidos não causam transições duplicadas.

## Testes de classificação de dispositivos

- Gamepad USB.
- Gamepad Bluetooth.
- Joystick que anuncie fonte compatível.
- Teclado USB/Bluetooth: não deve ativar o modo.
- Mouse USB/Bluetooth: não deve ativar o modo.
- Controle com fontes combinadas (gamepad + teclado).
- Dispositivo removido enquanto o serviço inicia.
- Controle já conectado antes da inicialização do serviço.

## Testes de ciclo de vida e segurança

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

## Critérios de aceite do MVP

- Detecção USB e Bluetooth consistente.
- Teclado e mouse não acionam o modo.
- Nenhuma instância duplicada do frontend.
- Retorno ao HOME somente após desconexão do último controle elegível.
- Nenhuma UI é aberta sobre a tela bloqueada.
- Lawnchair permanece como HOME padrão.
- Falhas do frontend não comprometem o launcher ou o boot.
