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
  morta, deslizar entre botões, canhoto, vibração.
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
- Otimização no Poco (2026-11-13), luta CPU × CPU (`build/lab/cpufight.sh`, tempo ∞), 1080p:
  - Perfil de GPU ao vivo: `renut_gpu_profile=8` + `banjo_renut_diag_gpu_shaders=true` em `files/live_cvars.txt`.
  - Mipmaps do host nos resolves (`swordman_renut_resolve_mips`) custavam 3,3–3,6 ms por quadro a 1080p: o jogo
    copia a cena três vezes por quadro (para restaurá-la depois do bloom e dos efeitos a meia resolução) e só a
    cópia do bloom é lida reduzida. Sem eles: GPU 19 → 14 ms.
  - SDK 0043: o laço de eventos do SDL 3.5 nunca bloqueava no Android (o pump reinsere o "poll sentinel" e cada
    evento novo manda um WAKE): um núcleo médio a 96–100 % o tempo todo. Corrigido.
  - Mapa do quadro (shaders por hash `ps`): sombras 256×256 → cena 1280×720 → sombra aplicada `F5C3A164` → cópia
    da cena, limpa, bloom 320×180 (`B1B362B3` redução e restauro, `4DC55DC2` desfoque, `90C56B1D` soma) → efeitos a
    640×360 (`3B9EED21`, `BCAD25CD`, `1ACFCC66`, `056DEAA7`) → frontbuffer. Opções do launcher: brilho e sombras
    (`swordman_renut_skip_ps`, ao vivo pelo patch 0204).
  - Ainda a 56–57 FPS na luta CPU × CPU tanto a 720p como a 1080p: o jogo perde a VBlank em ~5 % dos quadros
    (thread principal ~60 % ocupada, 92 % no código do jogo). A investigar: espera pelos threads de trabalho do jogo,
    afinidade (modo 7 experimental) e o aquecimento (estado térmico 3, 86 °C, depois de ~10 min).
- Opções do launcher testadas no Poco (2026-11-14, luta CPU × CPU, ao vivo pelo menu do jogo):
  - Resolução e escalonador mudam na hora no Renut (log `Render resolution`, capturas em pausa comparadas).
  - Custo do escalonador a 2712×1220 (`present and overlays`): bilinear 1,2 ms, AMD CAS 3,9 ms (novo: patch
    renut/0207 e SDK 0044, `present_effect_live=3`), FSR 1 3,3 ms com origem 540p/360p e 6,2 ms com 720p.
  - GPU por quadro: 1080p bilinear 15,7; 720p bilinear 9,6; 720p CAS 12,5; 540p bilinear 8,4; 540p FSR 10,6;
    360p FSR 8,5 ms. Os FPS ficam em 55–57 em todos: o limite é a thread principal do jogo, não a GPU.
  - Imagem: 720p + CAS fica perto de 1080p na textura da grama; o FSR refaz bordas a 540p/360p mas não detalhe.
    FSR só compensa abaixo de 720p.
  - 120 FPS (geração de quadros do Renut, `dbz3_frame_interpolation`, cvars na cola; a cola passa a taxa da tela
    de `files/display_hz.txt`): funciona (2 quadros por quadro do jogo a 120 Hz), mas no Poco o jogo cai para
    35–42 FPS em qualquer resolução (360p: GPU 9 ms, thread de render ocupada 7,6 ms; o jogo espera 4–8 ms no
    Swap). O bloqueio fica no `acquireTexture` dos dois presents; 4 imagens na swap chain (renut/0208,
    `swordman_renut_swap_images`) não ajudam. No Odin 3 (Adreno) chegava a 117. Marcado experimental.
  - Contador de FPS do launcher no Renut: a cola alimenta `kFrameTimeUs` a cada meio segundo (antes "--").
- A seguir: 60 estáveis na luta CPU × CPU; geração de quadros no Mali; depois o RP5 (perfil SD 480+); checar
  efeitos (aura, raios, explosões).

## Mi A3 — aparelho fraco (2026-11-14)
Snapdragon 665 (4× A73 2,0 GHz + 4× A53), Adreno 610, Armv8.0 sem LSE, 3,6 GB, tela 1560×720 a 60 Hz, LineageOS.
- Motor Armv8.0 (`THPS_MARCH=armv8-a`, SDK 0045): o LSE só fica nos helpers `__aarch64_*`, que escolhem em execução.
- Driver do sistema (Qualcomm V@502, Vulkan 1.1) não tem descriptor indexing: o Renut fecha. Com Turnip funciona:
  desde a 0.3.0 o APK de release não leva o Turnip (o jogador baixa um na aba Drivers; Purple T30 é o mais rápido no
  Adreno 610). `tools/android/fetch_turnip.sh` ainda o embute em builds pessoais, e aí o Automático o usa sozinho no
  Adreno 6xx (`Custom Vulkan driver … turnip … loaded`); sem ele o Automático fica no driver do aparelho. Renut remapeia os atributos de vértice para
  caber em 16 (renut/0209); a cola entrega o loader do Turnip ao Renut (`REX_VULKAN_LOADER_HANDLE`).
- Luta CPU × CPU, Renut, Turnip R8 (frio → quente): 360p bilinear 54–59 FPS → ~40 depois de ~3 min (CPU 77–86 °C,
  GPU 950 → 745 MHz); 360p CAS 48; 480p 43; 540p 39–41; 720p 25. GPU a 360p: R8 e T27 18,5 ms, Purple T30 12,5 ms.
  Afinidade: modo 1 no Renut (modo 2 deixava os trabalhadores nos A53 e a thread principal em `sched_yield`: 30 FPS).
  O RB2 roda a lógica por quadro: 40 FPS = jogo a 67 % da velocidade.
- Padrões para aparelho fraco (`LaunchConfig.weakDevice`: Adreno < 630 ou CPU sem `atomics`): 360p, brilho e sombras
  desligados (Leve). A opção 120 FPS some do launcher e do menu do jogo em telas sem modo ≥ 100 Hz.
- Controles na tela (esquema Raging Blast e BT3) testados botão a botão pelo log `bt3 in` e com dois dedos
  (`build/lab/mia3_mt.py`, `sendevent` no `fts_ts`; o adb do LineageOS é root): RT+Y e BT3 R3+△ transformam
  (Goku → SSJ). LT/RT chegam como gatilhos.
- Xenia no Mi A3: com Turnip R8 e `render_scale=50` caiu com SIGSEGV logo depois do primeiro resolve (compilação de
  shader); com Purple T30 abortou (`AHardwareBuffer` 4×4 falhou, `VulkanPresenter: Failed to submit`). Renut é o
  caminho no Adreno 6xx.
- **Aberto:** na tela Battle (Player / Time Limit / CPU Difficulty) o jogo parou de aceitar entrada duas vezes,
  sempre depois de mexer nas opções com o analógico. O controle virtual entrega tudo (log
  `setprop log.tag.DragonBlastPad V`: `pad [A]` / `pad []` a cada toque) e o jogo lê o pad 60×/s, mas nada mexe;
  abrir e fechar o menu lateral (XN_SYS_UI 1 → 0) destrava na hora. Não é o save (DB_SAVE não foi escrito). Próximo
  passo: logar no SDK os XN_SYS_UI dos diálogos/headless do XAM (`xam_ui.cpp`) e ver se algum fica sem o "0".

## Controle físico e guia dos controles (feito, 2026-11-15)
- Remapeamento do controle físico por esquema (perfil Raging Blast e perfil Tenkaichi 3; o Moderno usa o do Raging
  Blast): tela com desenho do controle, troca por toque + botão, "Trocar A/B X/Y", "Padrão". Patch 0047 do SDK filtra
  cada aparelho antes da junção, então o toque nunca é remapeado. Vale só na luta; menus, pausa e fim de luta usam os
  botões normais (testado no RG Cube: log `pad remap on (fight)` / `off` na pausa e no fim, de novo `on` no "Fight
  Again"). Teste de host do `pad_remap.cpp`: gatilhos analógicos, troca LT↔RB, botão desligado.
- Guia "Como funcionam os controles" com os três esquemas numerados sobre a prévia real.
- Layouts padrão novos (arco em volta dos quatro botões) e `ScreenMap` para telas 20:9 e quadradas; conferido no
  Odin 2 Mini (16:9) e no RG Cube (1:1).
- O toggle antigo "layout Nintendo no controle físico" saiu (virou o "Trocar A/B X/Y" do remapeamento).

## 0.3.1 — issues do GitHub, correções do Zandatsu, botões de deslizar (2026-11-19, sem aparelho)
- #1, #2, #4 (Poco X3 Pro, S20 FE 4G/5G): o Renut recusava o driver Qualcomm do Adreno 640/650
  (`maxDescriptorSetUpdateAfterBindSampledImages` < 8192) e dava `_Exit(3)`. Agora `rb2_renut_app.h` desliga o Renut
  na sessão e o jogo segue no Xenia (`rb2_render_scale` vem da altura do Renut); `files/renut_fallback.txt` com o
  motivo aparece em Gráficos → Renderizador.
- Desde a 0.3.0 o `NativeLibraryDir()` de `android_main.cpp` dava a pasta do libgame decifrado (`files/`): o SDK
  procurava ali o `librexgpu-xenos*.so` (Xenia nunca abria) e o libadrenotools os hooks (drivers da aba Drivers
  falhavam, "the driver found no GPU"). Passou a ser a pasta do `librexruntime` (dladdr num símbolo dele).
- Renut 0210–0216 portados do Zandatsu (0029/0030+0033/0032+0036/0034/0035/0040/0041): Mali de três dígitos (tela
  cortada), guarda de compilação (Mali-G615, PowerVR), constantes finitas (candidato da água do #3, a confirmar com
  `swordman_renut_finite_constants` true/false), versão do driver no log. Não portados: 0042–0045 (pacing/ADPF, em
  conflito com a 0117), GPU reset → launcher (0089ee3, precisa de trabalho na launcher).
- Controles: `ElementKind.RADIAL` virou botão de deslizar (ação segurada + ↑ → ↓ ← + toque, `HelperEngine.swipe*`,
  ação segurada 150 ms antes da direção). Layouts versão 3; os de luta antigos voltam ao padrão. `bt3_controls.cpp`:
  com R3/L3 segurado os botões passam crus (○ = B, casa da direita do losango).
- Falta testar no aparelho: Xenia no Adreno 650 com driver do sistema, os quatro botões de deslizar em luta (tempo do
  RT antes da casa, Ultimate por ↑), Mali-G710/G720.
