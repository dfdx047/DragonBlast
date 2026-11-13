# Dragon Blast — roteiro

## Fase 1 — launcher (2026-11-13, feita, aguardando teste do desenvolvedor no Poco)
- Launcher Compose no template das outras (abas em baixo; trilho lateral deitado): Início, Gráficos, Controles,
  Drivers, Depuração, Sobre. Paleta grafite + âmbar discreto + azul-aço (RB2 em tom sóbrio), fonte Rajdhani (OFL).
- Início: palco animado (`ClashStage.kt`): sem ISO = calmaria (pedras levitando, aura no chão, botão IMPORTAR ISO
  contornado); com o jogo = clash de energia, ondas de choque que tremem a cena e partem as pedras, botão JOGAR
  âmbar. Arquivos do jogo (importar ISO com verificação do default.xex, apagar), orientação (paisagem / retrato /
  sensor), links (YouTube, Discord, Ko-fi, GitHub).
- Gráficos: predefinições, renderizador (Xenia; Renut "em desenvolvimento"), resolução 50–100 %, FSR 1 / bilinear,
  60 / 120 FPS (geração de quadros), mostrar FPS, pausar em segundo plano, áudio estéreo.
- Controles: modo (desligado / automático / sempre), prévia desenhada, layouts Pro / Normal / Casual, estilo Dragon /
  Xbox / PlayStation / Nintendo, esquema da luta RB2 / Tenkaichi 3, opacidade, tamanho, analógico fixo/dinâmico, zona
  morta, deslizar entre botões, canhoto, vibração, layout Nintendo no controle físico.
- Drivers: GPU e driver do sistema, driver em uso (automático / sistema / instalados), importar .zip, listas do GitHub
  por fabricante — Adreno: Mr. Purple, StevenMXZ, K11MCH1, whitebelyash, faux123, ARMSX2; Mali: zenithblue (inclui as
  builds só para G615), JimVulkan, MaliSX2, FristOneRR, MineGraph, faux123 PanVK. Até 40 versões por fonte, cache de
  6 h. Testado no Poco: baixar + ativar + apagar volta para Automático.
- Depuração: exportar logs (zip com device.txt, logs/, perf/, logcat), log detalhado, estatísticas da GPU, opções
  extras de inicialização, limpar cache de shaders, restaurar configurações, relatório do aparelho.
- Cada configuração: legenda curta + botão "!" com a explicação longa. 240 textos em EN / PT-BR / ES.

## Fase 2 — jogo rodando no Poco (2026-11-13, em andamento)
Feito:
- SDK montado em `build/rexglue-sdk-android` (patches 0001–0041; 0038 versão do SDK, 0039/0040 Mali-G615 vindos do
  Dragon Burst, 0041 controle de toque = jogador 1). Codegen com o `default.xex` USA Rev 1 do desenvolvedor.
- O jogo roda no Poco com o driver Mali do sistema: abertura e menus 50–60 fps, luta (Goku x Vegeta, platôs)
  20–40 fps. Chaves da launcher ligadas no `LaunchConfig`/`GameActivity` (orientação, pausa, estéreo, log, overlay,
  args extras).
- Controles na tela (commit 39e8270), base Dragon Burst com visual próprio: analógico dinâmico, Pro / Normal /
  Casual, estilos Dragon / Xbox / PlayStation / Nintendo, canhoto, editor de layout, prévia ao vivo na aba Controles.
  Três esquemas de luta: Raging Blast (botões do jogo; os 4 Super Attacks num disco radial = analógico direito),
  Moderno (um toque: sway, vanishing, counter, agarrar, Ultimate, carga, subir/descer) e Tenkaichi 3 (posições do BT3,
  selos △○✕□). Testado no Poco: menus, seleção de personagens e luta só pelo toque.

Falta:
1. Troca automática menu/luta: achar no código/memória do jogo o estado "em luta" (`NativeGameState.fightFlag`). Hoje
   o botão ⇄ troca o layout à mão.
2. Conferir no jogo a página que o RT abre (trocar / transformar / fusão) e, se fizer sentido, separar os botões.
3. `--ez play true` vindo de fora é ignorado enquanto a launcher ainda verifica a instalação (esperar a verificação).
4. Menu lateral in-game no visual do Dragon Blast (substituir o `InGameMenu.java`).
5. Medições no Poco: driver do sistema vs PanVK / drivers Mali da lista; o aparelho esquenta.

## Fase 3 — Tenkaichi 3 no controle físico
- Na tela o esquema já existe (fase 2). Falta o controle físico: remapear os botões do controle para as posições do
  BT3 só durante a luta.
- Pesquisa: mapa completo dos comandos do RB2 e do BT3 (rush, ki, guarda, dash, carga, transformações, especiais,
  combos de direção); onde o RB2 lê o controle (código gerado: leitura XInput → estado de botões por jogador) e como
  saber que está numa luta (estado do jogo). Remapear só na luta; menus intactos.

## Fase 4 — Renut para o Raging Blast 2
- Seguir `~/Optxbox/docs/pt-BR/RENDERIZADOR_D3D.md` e o Renut do Dragon Burst: mapear a biblioteca D3D do RB2
  (d3dmap), M1–M3. Documentação Mali/Adreno já existente no Optxbox e no Fractal (`docs/MALI-ACHADOS.md`).
