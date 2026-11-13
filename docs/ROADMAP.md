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
1. ~~Troca automática menu/luta~~ feita: `game_state_jni.cpp` lê a cena (`0x8264C228`: 2 menus, 3 luta) e a pausa
   da luta (`[0x8264C230] − 0x47C` = 1; o bloco é do heap e muda de endereço a cada boot, o ponteiro global não),
   achadas comparando dumps de memória (`@dump` no `live_cvars.txt`, `tools/android/memdiff.py`). A `TouchOverlay`
   troca o layout sozinha; o ⇄ continua valendo até a próxima mudança.
2. Conferir no jogo a página que o RT abre (trocar / transformar / fusão) e, se fizer sentido, separar os botões.
3. `--ez play true` vindo de fora é ignorado enquanto a launcher ainda verifica a instalação (esperar a verificação).
4. Menu lateral in-game no visual do Dragon Blast (substituir o `InGameMenu.java`).
5. Medições no Poco: driver do sistema vs PanVK / drivers Mali da lista; o aparelho esquenta.

## Fase 3 — Tenkaichi 3 no controle físico (feito, 2026-11-13)
- Patch 0042 do SDK: `rex::input::SetStateFilter`, chamado com o estado já juntado de cada jogador (controles físicos
  + toque). `android/native/src/bt3_controls.cpp` (cvar `rb2_bt3_controls`, ligada pelo esquema "Tenkaichi 3" no
  arranque e ao vivo pelo menu) reescreve o jogador 1 só com a luta rodando e sem pausa: □ X, △ Y, ○ RB (guarda),
  ✕ A, R1 LB, R2 LT, L2 sozinho = d-pad ↓ (carga, após 180 ms), L2+○ B, L2+△ / ↑△ / →△ / ←△ = analógico direito
  (Super 1–4), L2+↓△ R3 (Ultimate), R3/L3 = RT (losango). L1 não tem equivalente.
- O layout de toque Tenkaichi 3 virou um pad do PS2 nas posições do BT3 (manda os botões crus; arquivo salvo novo
  `fight_bt3pad.json`), com atalhos L2+○, L2+△ e L2+↓△.
- Testado no Poco numa luta real: L2 segurado carrega o ki (aura), L2+△ solta o Super 1 (Kamehameha); ao sair da
  luta para os menus o esquema desliga. Log de depuração: cvar `rb2_bt3_log`.
- Falta conferir no aparelho: a pausa pelo ponteiro `[0x8264C230] − 0x47C` (confirmada em dumps de dois boots, ainda
  não com o jogo rodando), Super 2–4 e Ultimate por direção, Super Rising (R1+R2), losango pelo R3.

## Fase 4 — Renut para o Raging Blast 2 (beta funcionando, 2026-11-13)
- Base: mgr-nx-deko `ba1a837` (v0.2.2) + `patches/renut` 0001–0120 do Dragon Burst + os do RB2:
  - `0200` layout do device e hooks do RB2 (`docs/renut/HOOKMAP_RB2.md`, feito por `tools/renut/rb2_layout_edit.py`);
  - `0201` XenosRecomp: constante com nome de swizzle (`z`) e entrada PSIZE sem location (13 VS do motor);
  - `0202` contadores dos hooks de command buffer (diagnóstico, logados a cada 10 s pela cola);
  - `0203` caches do Renut (`renut_pipelines.bin`, `pipeline_list.bin`) no `cache_root` (`files/cache`).
- Cola: `android/native/src/renut/renut_glue.cpp` (VBlank `8249AED8` a 60 Hz por thread do host; Swap e draws
  partilhados; captura de quadro com `files/capture.req`; despejo de quadro com `--swordman_renut_frame_dump=true` e
  `files/frame_dump.req`), `rb2_renut_app.h`, `compat/` (APIs do SDK do Dragon Burst que o Renut usa).
- **Command buffers do jogo:** o RB2 grava os draws em command buffers em dois devices de tipo 2 e só os executa
  (`823A87D8`) quando o contador do slot (que cresce quando o D3D pede memória ao alocador do jogo) é > 0. O Renut
  grava no host e nunca pede memória: a cola marca o slot vazio com 1 antes da execução. Sem isso, 0 draws e tela
  preta.
- Shaders: `tools/renut/extract_rb2_shaders.py` (XEX + `st_pack_boot` + `st_pack_battle_map`, compressão `0LCS` da
  Spike) → 538 contêineres (249 VS, 289 PS), todos traduzidos.
- Medido no Poco (Mali-G615, resolução 50 % = 640x368, FSR): boot, logos, vídeo, menus, seleção e luta certos;
  luta a 54–55 FPS (GPU 9,2–9,5 ms por quadro, thread de render 4,2 ms, ~290 draws por quadro); menus 60.
- Launcher: Gráficos → Renderizador → Renut (beta) passa `--swordman_renderer=renut`.
- A seguir: otimizar no Mali até 60 estáveis na luta e subir a resolução; contador de FPS do launcher no Renut
  (hoje mostra "--"); depois o RP5 (perfil SD 480+); widescreen nativo; checar efeitos (aura, raios, explosões).
