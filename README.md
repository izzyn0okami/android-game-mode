# Android Game Mode

Projeto para integrar ao LineageOS um modo de jogo que alterna entre o launcher habitual e uma interface de jogos quando um controle compatível é conectado.

## Objetivo final

- Preservar o Lawnchair como launcher HOME padrão.
- Detectar controles de jogo USB e Bluetooth.
- Iniciar um frontend configurável (Argosy é uma opção, não uma dependência) quando um controle for conectado.
- Retornar ao launcher HOME padrão quando todos os controles forem desconectados e o usuário confirmar a saída em um diálogo.
- Evitar automações de terceiros e modificações no frontend.

## Plataforma-alvo

- Dispositivo inicial: Motorola Edge 30 Fusion (`tundra`, XT2243-1).
- ROM: LineageOS 24.0.
- Base Android: Android 17 (API 37).

## Estado do projeto

- Núcleo independente: máquina de estados e testes unitários.
- Serviço persistente (`:service`): abre o frontend (Argosy) ao conectar o controle e pergunta se deve encerrar ao desconectar. Feito para `system_ext/priv-app`; ver `docs/ROM_INTEGRATION.md`. Não validado em aparelho.
- APK de diagnóstico: primeira implementação Android para observar dispositivos de entrada enquanto o aplicativo está aberto.
- Integração privilegiada com a ROM: ainda não implementada; depende da auditoria do device tree, permissões, partição e SELinux.

O APK de diagnóstico não inicia frontend em background, não substitui o launcher e não exige permissões especiais. Nenhuma alteração será aplicada ao aparelho sem validação e instruções de recuperação.

## Documentação

- [Arquitetura](docs/ARCHITECTURE.md)
- [Plano de testes](docs/TESTING.md)

## Princípios

1. Não substituir nem modificar o launcher padrão.
2. Não iniciar interfaces sobre a tela bloqueada.
3. Não abrir instâncias duplicadas do frontend.
4. Só sair do modo jogo quando não houver nenhum controle elegível conectado e o usuário confirmar.
5. Aplicar tolerância a desconexões transitórias.
6. Manter o serviço independente do Argosy; qualquer frontend compatível poderá ser configurado.

## Segurança

A integração de sistema exige validação das permissões privilegiadas, da partição de instalação e das restrições de inicialização de atividades em segundo plano da versão Android-alvo. Não conceder permissões amplas sem justificativa técnica.
