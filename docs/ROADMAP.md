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

## Fase 2 — jogo rodando no Poco (próxima)
1. Montar o SDK (`prepare_sdk.sh`, patches 0001–0037 do RB2-Android) e o codegen com o `default.xex` do desenvolvedor
   (ISO USA Rev 1 — ainda não está no PC nem no Poco).
2. Ligar as chaves novas da launcher no `LaunchConfig`/`GameActivity`: orientação, pausa, áudio estéreo, log
   detalhado, args extras, estatísticas da GPU.
3. Overlay in-game: menu lateral adaptado ao celular (resolução ao vivo, FPS, geração de quadros, controles, sair),
   substituindo o `InGameMenu.java`.
4. Controles na tela (não existem no RB2-Android): portar o TouchPadView do DragonRage / os controles do Dragon Burst
   com o visual do Dragon Blast; Pro (analógico dinâmico, áreas maiores, deslizar com faixa neutra e histerese),
   Normal, Casual; troca automática de layout menu/luta pelo estado do jogo.
5. Primeiras medições no Poco (Mali-G615): driver do sistema vs PanVK.

## Fase 3 — esquema Tenkaichi 3 na luta
- Pesquisa: mapa completo dos comandos do RB2 e do BT3 (rush, ki, guarda, dash, carga, transformações, especiais,
  combos de direção); onde o RB2 lê o controle (código gerado: leitura XInput → estado de botões por jogador) e como
  saber que está numa luta (estado do jogo). Remapear só na luta; menus intactos.

## Fase 4 — Renut para o Raging Blast 2
- Seguir `~/Optxbox/docs/pt-BR/RENDERIZADOR_D3D.md` e o Renut do Dragon Burst: mapear a biblioteca D3D do RB2
  (d3dmap), M1–M3. Documentação Mali/Adreno já existente no Optxbox e no Fractal (`docs/MALI-ACHADOS.md`).
