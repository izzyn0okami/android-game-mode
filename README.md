# Android Game Mode

Serviço integrado ao LineageOS para alternar automaticamente entre o launcher habitual e uma interface de jogos quando um gamepad USB ou Bluetooth é conectado.

## Objetivo

- Preservar o Lawnchair como launcher HOME padrão.
- Detectar controles de jogo USB e Bluetooth.
- Iniciar um frontend configurável (Argosy como opção inicial) quando um controle for conectado.
- Retornar ao launcher HOME padrão quando todos os controles forem desconectados.
- Evitar automações de terceiros e modificações no frontend.

## Plataforma-alvo

- Dispositivo inicial: Motorola Edge 30 Fusion (`tundra`, XT2243-1).
- ROM: LineageOS 24.0.
- Base Android: Android 17.

## Estado

Projeto em fase de especificação e MVP. A integração privilegiada com a ROM ainda depende de auditoria do device tree e de testes em build próprio. Nenhuma alteração será aplicada ao aparelho sem validação e instruções de recuperação.

## Documentação

- [Arquitetura](docs/ARCHITECTURE.md)
- [Plano de testes](docs/TESTING.md)

## Princípios

1. Não substituir nem modificar o launcher padrão.
2. Não iniciar interfaces sobre a tela bloqueada.
3. Não abrir instâncias duplicadas do frontend.
4. Só sair do modo jogo quando não houver nenhum gamepad elegível conectado.
5. Aplicar tolerância a desconexões transitórias.
6. Manter o serviço independente do Argosy; qualquer frontend compatível poderá ser configurado.

## Segurança

A integração de sistema exige validação das permissões privilegiadas, da partição de instalação e das restrições de inicialização de atividades em segundo plano da versão Android-alvo. Não conceder permissões amplas sem justificativa técnica.
