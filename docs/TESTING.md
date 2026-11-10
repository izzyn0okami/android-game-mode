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

## Testes unitários do classificador

- Controle externo com fonte GAMEPAD é aceito.
- Controle externo com fonte JOYSTICK é aceito.
- Teclado/mouse sem fontes de jogo são ignorados.
- Dispositivo interno é ignorado mesmo se anunciar fonte de jogo.
- Dispositivo virtual é ignorado.
- Fontes combinadas (gamepad + teclado) continuam elegíveis.

## Testes manuais do APK de diagnóstico

1. Instalar e abrir o APK de diagnóstico.
2. Sem controles conectados, confirmar a mensagem de ausência de controle elegível.
3. Conectar um gamepad USB e confirmar atualização da lista sem reiniciar o aplicativo.
4. Desconectar o gamepad e confirmar remoção da lista.
5. Repetir com um controle Bluetooth já pareado.
6. Conectar teclado e mouse e confirmar que não são classificados como controles.
7. Conectar dois controles, remover um e confirmar que o contador permanece em um.
8. Fechar e reabrir o aplicativo com um controle conectado; confirmar que a enumeração inicial o encontra.
9. Registrar nome, fabricante/produto, fontes e resultado mostrado para cada acessório testado.

A propriedade `isExternal` não identifica o transporte exato: o diagnóstico apenas confirma se o Android classifica o dispositivo como externo. Não interpretar esse campo isoladamente como prova de USB ou Bluetooth.

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

## Critérios de aceite do APK de diagnóstico

- APK debug compilado e disponível como artefato da CI.
- Enumeração inicial e eventos de adição/remoção/alteração funcionais.
- Classificador passa nos testes unitários.
- Teclado e mouse não são classificados como controles.
- O aplicativo não inicia frontend, não muda HOME e não exige permissões especiais.

## Critérios de aceite do sistema completo

- Detecção USB e Bluetooth consistente.
- Nenhuma instância duplicada do frontend.
- Retorno ao HOME somente após desconexão do último controle elegível.
- Nenhuma UI é aberta sobre a tela bloqueada.
- Lawnchair permanece como HOME padrão.
- Falhas do frontend não comprometem o launcher ou o boot.
