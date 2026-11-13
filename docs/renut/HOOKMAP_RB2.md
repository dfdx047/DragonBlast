# Mapa de hooks do Direct3D do Raging Blast 2 para o Renut

Dragon Ball: Raging Blast 2, `default.xex` USA Rev 1 (SHA-256 `d0291e64…e989c`), XDK 2.0.12625. Análise estática,
13/11/2026. Modelo: `Dragon-Burst/docs/renut/HOOKMAP_BL.md` (Burst Limit, XDK 6683).

- Fonte: o código gerado em `android/codegen/generated-android/default/` (cada `sub_X` traz a desmontagem PPC em
  comentário) e a imagem PE `build/renut/map/rb2.img` (base 0x82000000, fora do git). A imagem confirma os bytes:
  `0x824AA220` = `817F3460 lwz r11,13408(r31)` (KickOff), `0x8249ADAC` = `38605F00 li r3,24320` (CreateDevice).
- Papéis: os 29 do `mgr_hook_map.h` do mgr-nx-deko (MGR, XDK 21173) e as provas de `optxbox/hookmap.py`.
- Guardas (64 bytes, ou a função inteira se for menor, + FNV-32): `rb2_hook_map.h`, nesta pasta.

## Resultado

| | Papéis |
|---|---|
| **Certo, função própria** | 26 |
| **Certo, mas embutido (LTCG) numa função do jogo** | 0 (mas o hook de declarações vai noutro sítio: linha 26) |
| **Provável** (equivalente interno, sem API pública) | 1 (SetRingBufferParameters) |
| **Não existe neste jogo** | 2 (BeginVertices, EngineSwapProgress) |

O que manda no resto: **o device do RB2 não é nenhum dos dois layouts conhecidos.** A parte pública
(0x0000–0x2AA4) é igual nos quatro XDK; o bloco 0x2A80–0x2ABF (qword global, thread, fences, flags) está na posição
do N&B/BL; daí para a frente o RB2 fica entre o N&B e o MGR, com quatro deslocamentos diferentes:

| Bloco | N&B/BL → RB2 | MGR → RB2 |
|---|---|---|
| 10880–10943 (qword global, thread, fences, flags) | +0 | −128 |
| declaração de vértices | +172 | −248 |
| objetos ligados, viewport, scissor, shaders, predicação, tiling, InKickOff (12192–13432 do N&B) | **+176** | −200 |
| command buffer, cópia dos params, anel/segmentos, fence do Present, secções críticas, gama | **+212** | −192 |
| contador de Swap, side buffer, VBlank, cauda de vídeo, −1, frequência (N&B) | **+216** | −192 |
| BehaviorFlags / tamanho do device | +220 / +256 (0x5F00) | −192 / −384 |

Ou seja, o Renut precisa de um **terceiro layout**. A boa notícia é a estrutura do D3D: ao contrário do BL, o RB2 não
foi ligado com LTCG agressivo. `Direct3D_CreateDevice`, `InsertCallback`, `XGRegisterPixelShader`, `IsFencePending` e
`D3DResource_Release` são funções próprias, com as mesmas instruções do MGR (às vezes com os mesmos registos). O
`Sa2CreateDevice` do MGR (original com tipo 2, depois os campos do device do título) volta a servir, com outros offsets.

O casamento automático (`build/renut/map/hookmap.md`, 10 de 29) acertou 9: o `SetRingBufferParameters` = `82510448`
é código do jogo (4788 bytes, fora da biblioteca). Os candidatos múltiplos de InsertCallback e Destroy incluíam os
certos (`824AA520`, `824A7970`).

Os números entre parênteses são offsets decimais no device (`r31`/`r3` = `CDevice*`), como aparecem na desmontagem.

## Tabela

| # | Papel | MGR (21173) | **RB2** | Confiança | Prova (instruções do RB2) |
|---|---|---|---|---|---|
| 1 | Direct3D_CreateDevice | 826B2418 | **8249AD90** | certo | `li r3,24320` (0x5F00) + `li r4,128` → `8249ABC8` (XMemAlloc alinhado + memset), `bl 0x824aba78` (linha 4), `cmpwi cr6,r29,2` (tipo 2: `ori r11,r11,128; stb r11,10940`, `bl 0x824a8c38` anel = side buffer, `bl 0x824abd50` caminho sem hardware), senão `rlwinm. …,0,23,23` / `0,2,7` / `oris r30,r30,3072` e **`stw r30,24200(r31)`** (BehaviorFlags, MGR `24392`), `bl 0x824abae8` (linha 5); falha: `lwz r3,-4(r31)` + XMemFree, `0x8007000E`. As 3 provas do MGR com os valores do RB2. Chamador: `823D9F90` (jogo), que cria **um device do título (tipo 1) e dois de tipo 2** (ponteiros em `0x8264C9EC`, `…9F0`, `…9F4`). |
| 2 | InitializePresentationParameters | 826C0C48 | **824AB5E0** | certo | `lwz r28,0(r4)` largura, `4` altura, `8` formato, `16` multisample, `40` profundidade, `64` formato do front; `stw r3,15040(r29)` (front automático, criado por `824A6838` se `60(r31)`=0), `stw r5,15044(r29)` (back), `stw r4,15036(r29)` (depth); `addi r3,r29,13756; li r5,124; bl memcpy` (MGR `13948`); `stw r11,13820(r29)` (formato do front); vídeo `21760/21764/21768` e `stfiwx → 21772`; gama `824A2D20`/`824A2DF0`; `stw r11,16924(r29)` = −1. |
| 3 | InitializeApiState | 826C0278 | **824AB378** | certo | `lwz r25,13808(r3)` (PresentationInterval, MGR `14000`, mesmo registo), `lwz r24,24200(r3)`; 101 estados (`cmplwi cr6,r30,404`, `(i+16)*4` / `(i+137)*4`, `lwz r11,64(r9); bctrl`), 20 de sampler (`cmplwi 80`, `468(r9)`), 26 texturas (`addi r27,r31,1152`, `cmplwi r28,26`, `bl 0x824a6a90` SetTexture); `stw 12368`=5 / `12372`=1 (declaração padrão), `bl 0x8249f038`; **`stw r23,12888(r31)`** e **`stw r10,12884(r31)`** = −1 (as provas do MGR `13088`/`13084`), `stw r23,12892`, `10932`/`10936` = 0; no fim `bl 0x8249ea70` (0,0,0). |
| 4 | XenonGpu_Initialize | 826C0930 | **824ABA78** | certo | O papel com as provas do MGR (os campos que todo device recebe): `stw r10,60(r3)` = 1 (RefCount), **`std r11,10880(r3)`** (qword global de `0x8260EBB8`: `lis r11,-32159; ld r11,-5192(r11)`), **`stw r3,10888(r31)`** (GetCurrentThreadId), `stw r11,10892` (processo ≠ 2), `li r3,4800` + XMemAlloc + **`stw r3,16944(r31)`** (side buffer). A imagem de registos de arranque (o "XenonGpu_Initialize" do BL) é outra função: **824B9598** (`std r9,12000(r3)` = 0x2E30 do N&B +176, `stw 10428`, `10444`, `10564`/`10568`, `std 0..32` m_Pending, `stw 10916`), chamada pelo setup e pelo caminho sem hardware. |
| 5 | HardwareDeviceSetup_reference | 826C0EC8 | **824ABAE8** | certo | **`addi r3,r3,15156`** e **`addi r3,r31,15184`** + RtlInitializeCriticalSection (MGR `15348`/`15376`), `ori r11,r11,4; stb r11,10942`, `bl 0x824ab9e0` (VdInitializeEngines + **VdSetGraphicsInterruptCallback(0x824A8BA0, device)**, `stw 16928`=0), VdGlobalDevice (`lwz r11,1868(0x82000000)` se o processo ≠ 2, senão `1872`; `stw r31,0(r11)`), bloco de fence 96 B → `10896`, 32 B → `10900`, `addi r4,r29,72; bl 0x824aa578` (anel), `bl 0x8249ac18`/`8249a7f0`/`824b93f8`/`8249ebb8` (buffers extra em `24208`…`24240`), `stw -1` em `21776`/`21780`, `bl 0x824b9598`, `bl 0x824ab5e0` (linha 2), `bl 0x824ab378` (linha 3), **`stfs f13,21804(r31)`** e `21808` (MGR `21996`), `bl 0x824b1e60` (41 × −1 a partir de `23600`). Devolve 1. Único chamador: linha 1. |
| 6 | KickOff | 826B5688 | **824AA1F8** | certo | `lwz r11,10896(r3)`, **`lwz r11,13408(r31)`** (MGR `13600`), `lbz 10940` (0x80), **`bl 0x824aa070`** (KickOffSegment), `lwz r11,10908` + `bl 0x824a8fc8` (BlockOnFence fence−2), `ori r11,r11,2; stb r11,10941`, **`lwz r3,48(r31)`**. Nenhum chamador fora da biblioteca D3D. |
| 7 | KickOffSegment | 826B5348 | **824AA070** | certo | **`lwz r30,15140(r3)`** (início do segmento, MGR `15332`), command buffer `lwz 13728` + `152(r11)` e lista em `13748/13752`, ou lista de segmentos `13568/13580/13584`, ou `bl 0x824a9a30` (SetFence) + `bl 0x824a9b70`; **`stw r10,48(r31)`**, `stw r11,15140`, **`bl 0x824a9370`** (NewSegment) se passar de `56(r31)`. |
| 8 | BeginRingBig | 826B58C0 | **824AA430** | certo | `rlwinm r30,r4,2,0,29`, `lwz 48`/`lwz 52`, `bl 0x824aa1f8` (KickOff), `bl 0x824a9370` (NewSegment), `lwz r3,48(r31)`. As 3 provas do MGR. |
| 9 | RingBufferDeviceAllocate | 826B4078 | **824A8CB0** | certo | `(CDevice*, Flags r4, DWORD *pSize r5, Alignment r6)`: `lwz r11,0(r5)`, **`lwz r11,15128(r3)`** (MGR `15320`), `15116…15148`, `lwz 13408`; no fim `stw r11,0(r29)`. Chamadores: AllocateInlineData (`li r4,1`) e NewSegment (`li r4,2`, `li r6,32`). |
| 10 | AllocateInlineData | 826B44C0 | **824A8EE0** | certo | `rlwinm r11,r4,2,0,29`, **`lwz r3,13728(r31)`** (command buffer, MGR `13920`), `bl 0x824a8cb0`; com command buffer usa `152(r3)` → `824A4068`/`824A4380`; `15144`/`15148`; sem memória `ori 32` em `10941`. |
| 11 | NewSegment | 826B45A8 | **824A9370** | certo | **`stw r11,48(r31)`**, **`stw r10,56(r31)`**, **`stw r11,52(r31)`** (as 3 do MGR, mesmos registos); com `10941 & 0x20` chama `824A8C38` (side buffer `16944`, +4800, guarantee −160, `ori 32`), o caso do `Sa2NewSegment_hook`; `15140` = início; espera `15152` com `bl 0x824a8fc8` (tipo 3). Chamadores: BeginRingBig, KickOffSegment, Swap. |
| 12 | SetRingBufferParameters | 826B5A08 | **não existe como API pública**; equivalente interno **824AA578** | provável | `824AA578(CDevice*, D3DRING_BUFFER_PARAMETERS*)`: se há anel, espera ociosa (`bl 0x824aa4a8` + ciclo `10988`/`10992`), liberta `15048`/`15052` com XMemFree, `stw 48/52` = 0, e com params novos: `MmGetPhysicalAddress`, `VdInitializeRingBuffer`, `VdEnableRingBufferRPtrWriteBack`, ME_INIT, `15116`/`15120`. Chamadores: `824ABAE8` (setup) e `824ABE20` (desmontagem, com `r4 = 0`). Com o `Sa2CreateDevice` no lugar da criação só a desmontagem chega aqui: hook a devolver 0. |
| 13 | BlockOnFence | 826B42A8 | **824A8FC8** | certo | `lwz r11,10896(r3)`, `lwz r10,10908(r3)` (MGR `11024`/`11036`), teste `tag − fence < tag − *completion`, `clrlwi. r11,r7,24` (**NoKick em r7**), `lwz r11,13408(r3)` antes do `bl 0x824aa1f8`, CBlocker (`8249A5B8`) e ciclo **`bl 0x8249a6c8`** (CBlocker::Check), `8249A5E8`. Assinatura `(CDevice*, Fence, Type, Object r6, NoKick r7)`: o Destroy passa o recurso em r6. O hook do Renut ignora tipo e NoKick: serve. |
| 14 | CBlocker_Check | 826B1C88 | **8249A6C8** | certo | `lwz r29,0(r3)`, `lbz 10941(r29)`, **`lwz r11,10896(r29)`**, `bl GetCurrentThreadId` + **`lwz r11,10888(r29)`**, `lwz 11008(r29)`, `cmplwi cr6,r11,5000`, `bl 0x824ab188` (dump da GPU). |
| 15 | IsFencePending | 826B57C8 | **824AA338** | certo | `(Fence)`: device por VdGlobalDevice (`lwz 1868`/`1872`), **`lwz r11,10908(r31)`**, igual → **`bl 0x824aa1f8`**, **`lwz r11,10896(r31)`**, devolve `tag − fence < tag − *completion`. Só chamado por `824B65A8` ← `824AD3F8` (dentro do D3D); o jogo não o usa. Hook opcional. |
| 16 | InsertCallback | 826B59B0 | **824AA520** | certo | **`mr r30,r4; mr r29,r5; mr r28,r6`**, BeginRing em linha (`lwz 48`/`56`, KickOff), **`bl 0x824a9e00`** (escritor do pacote: lê `10900` do device de VdGlobalDevice, `stwu r28`/`r27` = callback e contexto), `stw r3,48(r31)`. Mesma assinatura do MGR: o hook do Renut serve sem invólucro. Chamador: `824BE688` (o leitor de vídeo XMV, `0x824BC000…`). `824A9E00` também é chamado por SynchronizeToPresentationInterval e `824AF928`. |
| 17 | SynchronizeToPresentationInterval | 826B3050 | **8249B258** | certo | `(CDevice*, BOOL)`: **`lwz r11,13808(r31)`**, esperas e callback via `bl 0x824a9e00`, no fim **`ori r11,r11,16`** + **`stb r11,10941(r31)`**. A API pública é `8249B680` (`li r4,0; b 0x8249b258`), que o jogo chama em `823A8888` e a thread `824B91A8`. Hook no interno cobre os dois. |
| 18 | Swap_shared | 826B3510 | **8249B688** | certo | **`lwz r11,16776(r31)`** + `stw` (contador de Swap, MGR `16968`), `addi r4,r27,28; li r5,24; memcpy`, `lwz 13820`, `addi 13852`/`13880`/`13936`, **`addi r26,r31,15216`** (rampa de gama, MGR `15408`), `VdGetSystemCommandBuffer`, `VdSwap`, `lwz r30,10908` + **`stw r30,10928(r31)`** + KickOff, `lwz 16784` vs `16776`, `stw r30,15152` (fence do Present), `ori` em `10941`. Chamadores: o jogo `823A8888` (com o device de `0x8264C9EC`), `8249BD10` (Present do XMV) e a thread `824B91A8`. |
| 19 | DrawVertices_shared | 826BC928 | **824A7FF0** | certo | `(device, PrimitiveType r4→r25, StartVertex r5→**`mr r23,r5`**, VertexCount r6)`. Esvaziamento do estado pendente **em linha** (o `bl 0x826cb580` do MGR não existe): `ld 0..32`, constantes `addi r6,r3,1920; li r5,16384` / `6016; 17408`, pacotes `10548`→0x2200, `10528`→0x2180, `10368`→0x2000, `10444`→0x2100, `10596`, `10832`, `10112`, `10680`; StartVertex por registo (`li r11,8450`); **`lis r11,-16383; ori r11,r11,8705`** = 0xC0012201. Com `10940 & 1` (tiling) usa a lista `13424/13436/13440`. Chamadores: `823E88A8` (jogo) e `824BE688` (XMV). |
| 20 | DrawIndexedVertices_shared | 826BCD40 | **824A8408** | certo | Mesmo esvaziamento; **`lwz r24,12612(r31)`** (index buffer, MGR `lwz r24,12812`, mesmo registo), `lwz r6,0(r24)` (bit 31 = 32 bits), `lwz r10,24(r24)` (endereço), `lis -16381; ori 8705` = 0xC0032201. Argumentos `r4` tipo, `r5` BaseVertexIndex, `r6` StartIndex, `r7` IndexCount. Chamador: `823E8A90` (jogo). |
| 21 | BeginVertices | 826BC468 | **não existe** | não existe | Só três funções escrevem DRAW_INDX (`ori …,8705`): `824A7FF0`, `824A8408` e `8249AC18` (setup). Nenhuma função faz `mullw` de r5×r6 com stride; AllocateInlineData só tem chamadores internos (resolve, clear, command buffer, KickOff). Não há BeginVertices nem DrawVerticesUP; nada lê nem escreve o "fim do anel em linha" (N&B `13444` +176 = `13620`: zero referências). |
| 22 | ClearF | 826BC260 | **824A01C8** | certo | `(device, Flags r4, pRect r5, pColor r6, Z f1, Stencil r8)`: sem rect **`lwz r7,12616(r3)`** (RT0) ou **`lwz r11,12632(r3)`** (depth), tamanho em `+36`; com tiling `12904…12920` e `13356/13360`; **`bl 0x8249fb48`** (o ClearF interno). O jogo chama `D3DDevice_Clear` **824A02F0** (converte a cor e chama este por rect; 7 chamadores do jogo). |
| 23 | Resolve | 826BB0D0 | **8249DA40** | certo | **`lwz r30,260(r1)`**, **`lwz r29,252(r1)`** (com `stwu r1,-160`: palavras +100 e +92 do chamador, as que o hook do Renut lê), `stw r31,92` … **`bl 0x8249c8f0`** (o núcleo, 4432 B). Chamadores: `823A8888` e `823D95F0` (jogo), `8249BD10`, `824AE278`. |
| 24 | XGRegisterVertexShader | 826ACD00 | **8249EE50** | certo | `mr r30,r4; li r5,872; bl memset`, `li r11,6`, **`stw r30,32(r31)`**, **`lwz r11,872(r31)`**. Chamadores: `823EA4E8` (jogo) e `8249EF48` (CreateVertexShader, só o XMV). |
| 25 | XGRegisterPixelShader | 826AC3D8 | **8249E520** | certo | 56 bytes: `std 0..32` = 0, **`li r10,7`**, `stw r9,4` = 1, `stw r8,20` = 0xFFFF0000, **`stw r4,24(r3)`**. Função própria (no BL estava embutida). Chamadores: `823D8BD0` (jogo) e `8249ED20` (CreatePixelShader, só o XMV). |
| 26 | CreateVertexDeclaration | 826AC938 | **8249E9F0** | certo (hook noutro sítio) | **`cmplwi cr6,r9,255`**, **`mulli r11,r11,12`**, `addi r3,r11,56` + XMemAlloc, **`bl 0x8249e900`**. Os mesmos 64 bytes do MGR (FNV `CEF02F60`). **Mas só o XMV (`824BCD38`) a chama**: o jogo aloca as declarações e chama o inicializador **`8249E900(pElements r3, pDecl r4)`** direto, em `823D2AF0` (14 chamadores). `8249E900` grava Common `0x00100005`, contagem em `+24`, elementos a partir de `+52`. **Hook proposto:** cru em `8249E900` (original, depois `RegisterGuestDeclaration(r4, r3)`), além de `8249E9F0`. |
| 27 | D3DResource_Destroy | 826AAF80 | **824A7970** | certo | Device por VdGlobalDevice (`lwz 1868`/`1872`), **`clrlwi r10,r11,28`** + tabela de saltos, `lwz r4,8(r31)` + **`bl 0x824a8fc8`** com tipos 13/9/…, **`bl 0x8218d028`** (XMemFree do jogo); surfaces `lwz r11,44(r31)` / 5120 e `lwz r9,28(r31)`. Único chamador: `824A7ED8` = D3DResource_Release (`lwarx/stwcx.` em `+4`), que o jogo chama em `823DFB38`. |
| 28 | WindowScissor | 826B5FC8 | **824A03B0** | certo | **`mr r25,r4; mr r24,r5; mr r23,r6; mr r22,r7`** (os mesmos registos do MGR), sem tiling **`ori r11,r11,8320`** (0x00022080); com tiling `12924` (TileCount), `addi 12932` (rects, −4), `addi 13172` (origens, −4). |
| 29 | EngineSwapProgress_reference | 826B2FC0 | **não existe** | não existe (motor do MGR) | Nenhuma função fora do D3D lê `16776` e `16784` juntos. O equivalente do RB2 é a espera do próprio jogo em `823A8888`: depois do Swap, `lwz r3,-15112(r30)` (contador de VBlank em `0x8264C4F8`) contra o intervalo pedido, com `NtYieldExecution` (`823F08D0`), e zera o contador. Ver "VBlank" abaixo. |

## Outras funções que o caminho do Renut toca (não estão entre os 29)

| Função | RB2 | Prova curta |
|---|---|---|
| DrawVerticesUP / BeginVertices | não existem | linha 21 |
| esvaziamento de estado no draw (o `sub_826CB580` do MGR) | em linha nos dois draws | `824B45E8` (constantes), `824B5410`, `824B3EB8`, `824B4250` (pacotes), `824B4490`, `824B4310` |
| imagem de registos de arranque | 824B9598 | linha 4 |
| D3DDevice_Clear (pública) | 824A02F0 | linha 22 |
| D3DDevice_SynchronizeToPresentationInterval (pública) | 8249B680 | linha 17 |
| D3DDevice_Present (Resolve + Swap) | 8249BD10 | chamada só pelo XMV (`824CCA90`) |
| CreateVertexShader / CreatePixelShader | 8249EF48 / 8249ED20 | chamam as linhas 24/25; só o XMV (`824BEA80`) |
| inicializador de declaração | 8249E900 | linha 26; o jogo chama-o em `823D2AF0` |
| SetVertexShader / SetPixelShader | 8249E718 / 8249E558 | `stw r29,12872(r30)` / `stw r29,12868(r30)`; ambos `lwz r11,10912(r30); and.` |
| SetVertexDeclaration | 8249E8E8 | 20 B: `stw r4,11992(r3)` e `oris …,8` em `+16` |
| SetTexture | 824A6A90 | `addi r11,r4,3180` (×4 = `12720+4i`), `addi r11,r4,48; mulli 24` (fetch em 1152) |
| SetStreamSource | 824A2A60 | `addi r11,r29,3159` (×4 = `12636+4i`), `subfic 17; addi 222` (×8 = fetch em 1776), stride `stb` em `12704+i` |
| SetIndices | 824A2C08 | `stw …,12612` |
| SetRenderTarget / SetDepthStencilSurface / SetSurfaces | 824A33A8 / 824A36F8 / 824A39D0 | `(i+3154)*4` = `12616+4i`; `stw 12632`; SetSurfaces grava `12616…12632` e `12900…12920` |
| SetScissorRect / SetViewport | 824A3268 (→ 824A2968) / 824A3010 | `stw r4,12032` + rect em `12852…12864`; `stfs 12824…12844`, `stw 12848` |
| Begin/End/RunCommandBuffer | 824A44D0 / 824A4A28 / 824A4E48 | `stw r30,13728(r31)` / `stw r25,13728` = 0 / `INDIRECT_BUFFER`; jogo: `823A4928` grava, `823A87D8` executa |
| Acquire/ReleaseThreadOwnership | 8249AA10 / 8249AA50 | `stw …,10888` (GetCurrentThreadId / 0) |
| UnsetAll | 8249AA60 | chama SetRenderTarget, SetDepth, Set*Shader, SetVertexDeclaration, SetIndices, SetStreamSource, SetTexture com 0 |
| SetFence | 824A9A30 | `lwz r10,15132` (`& 3 \| m_pRing`, a `0x3A48` do N&B), `lwz 10908`, `lwz 10896`, `stw 13416/13420` |
| D3DResource_Release | 824A7ED8 | linha 27 |
| D3DDevice_Release / desmontagem | 8249A9B8 → 824ABE20 | `lwz r11,60(r3)`; em 1: `bl 0x824abe20` (UnsetAll, VdShutdownEngines, `824AA578` com 0, XMemFree de `10900`/`10896`) e XMemFree do bloco `-4`; jogo: `823D9DF8` |
| XMemAlloc / XMemFree do jogo | 8218CF70 / 8218D028 | alocam o device, o side buffer, as fences, as declarações; Destroy liberta com `8218D028` |
| VdGlobalDevice (slot do import) | **0x8200074C** (título) / 0x82000750 (sistema) | a imagem tem `0x000101BE` / `0x000101BF`; o D3D escolhe pelo `KeGetCurrentProcessType() == 2` |
| qword global copiado para o device | 0x8260EBB8 | `824ABA88 lis r11,-32159` + `ld r11,-5192(r11)` |
| device do jogo (ponteiro global) | **0x8264C9EC** (título); 0x8264C9F0 / 0x8264C9F4 (os dois de tipo 2) | `823DA138 stw r11,-13844(r10)` com `lis r10,-32155`; `823A8888 lwz r3,-13844(r11)` |
| callback de interrupção da GPU | 0x824A8BA0 | registado em `824AB9E0` (`VdSetGraphicsInterruptCallback`, contexto = device); tipo 1 chama `[[10900]+16]([[10900]+20])`, tipo 0 → VBlank |
| tratador de VBlank do D3D | 8249AED8 | spinlock `16928`, `16760`++ (contador de VBlank), `16764` = timebase, fila de flips `16788…`/`16916`/`16920`, `16784`++ (Swaps mostrados), chama `[16756]` com `{16760, 16784, 0}`, `stw 21892` |
| D3DDevice_SetVerticalBlankCallback | 8249B250 | 8 B: `stw r4,16756(r3)`; o jogo (`823A8940`) regista **0x823A8510** |
| callback de VBlank do jogo | 0x823A8510 | 20 B: `0x8264C4F8` += 1 |
| thread de Swap do D3D | 824B91A8 | sem chamadores (início de thread): `KeWaitForSingleObject`, `8249B680`, `8249B688` |
| leitor XMV (biblioteca estática) | 0x824BC000–0x824CD000 | `824BE688` (DrawVertices, InsertCallback, Set*), `824CCA90` (thread: Clear, Present) |

**O jogo marca o tempo pelo VBlank, como o BL.** `823A8888` (o Present do jogo, chamado por tabela virtual) faz
SynchronizeToPresentationInterval, Resolve para o front buffer, Swap, e depois fica em `NtYieldExecution` até o
contador `0x8264C4F8` (que só o callback de VBlank `0x823A8510` incrementa) chegar ao intervalo pedido (`r4`), e zera
o contador. No Renut não há interrupção da GPU (o `824AB9E0` não corre), por isso a cola tem de bater o VBlank a
60 Hz: chamar `8249AED8(device)` (o equivalente do `820B5CF8` do BL; precisa do spinlock `16928` zerado, o que o
`Sa2CreateDevice` deve fazer como o BL fazia com `16712`) ou chamar `[device+16756]` direto. Sem isso o jogo prende
em `823A8888` sempre que pedir intervalo > 1.

## Offsets do device que o código do Renut usa: RB2 contra N&B, BL e MGR

Situação: **igual** = os quatro iguais; **N&B** = o RB2 tem o valor do N&B/BL; **novo** = nem N&B nem MGR (valor do
RB2 provado); **pos.** = posto pelo deslocamento do bloco, sem leitura direta; **?** = não provado.

### Parte pública (`d3d.h` `struct Device`, `d3d_native.h` `off::`)

| Campo | N&B = BL = MGR | RB2 | Situação | Prova no RB2 |
|---|---|---|---|---|
| `m_Pending[5]`, `PredicatedPending2` | 0x00–0x27, 0x28 | igual | igual | draws `ld 0..32`; `824B9598 std 0..32`; `824AC6B0 ld/std 40` |
| `m_pRing` / `Limit` / `Guarantee` | 0x30/0x34/0x38 | igual | igual | BeginRingBig `48`/`52`; NewSegment `48/52/56` |
| `m_ReferenceCount` | 0x3C | igual | igual | `824ABA78 stw r10,60`; Release `lwz 60` |
| tabelas Set/Get Render/SamplerStateCall | 0x40/0x224/0x1D4/0x3B8 | igual | igual | InitializeApiState `(i+16)`/`(i+137)`/`(i+117)`/`(i+238)`×4, `lwz 64(r9)`/`468(r9)` |
| `TextureFetch[26]` / `VertexFetch[18]` | 0x480 / 0x6F0 | igual | igual | SetTexture `(i+48)*24`; SetStreamSource `addi 222` ×8 |
| `VertexShaderF` / `PixelShaderF` / `…B…I` | 0x780 / 0x1780 / 0x2780 | igual | igual | draws `1920;16384`, `6016;17408`, `10112;18688` |
| `m_DestinationPacket` … `m_PointPacket`, Clip/ModeControl, `m_WindowPacket` | 0x2880…0x2A50 | igual | igual | draws `10368/10444/10528/10548/10596/10680/10832`; `824B9598` `10564`/`10568`; BeginCB lê `10436/10440` |
| tesselação (`0x2978…0x2980`), bias (`0x2A50…0x2A5C`) | igual | igual | igual | `824A11A8`/`824A1270` `lfs/stfs 10832…10844` |

### Parte privada

| Constante do Renut (ficheiro) | MGR | N&B | BL | **RB2** | Situação | Prova no RB2 |
|---|---|---|---|---|---|---|
| `kDeviceAllocSize` (d3d.h) | 0x6080 | 0x5E00 | 0x5780 | **0x5F00** | novo | CreateDevice `li r3,24320` |
| `kDeviceOffsetGlobalQword` / `kGlobalQwordVa` (device.cpp) | 11008 / 0x834FB7E0 | 10880 / 0x82F39B38 | 10880 / 0x827252E8 | **10880 / 0x8260EBB8** | N&B / dado do jogo | `824ABA9C std r11,10880(r3)` |
| `CreationThreadId` = `OwnerThread` (0x2A88) | 11016 | 10888 | 10888 | **10888** | N&B | `824ABAA4`; CBlocker `lwz 10888(r29)`; `8249AA10` |
| `IsTitleProcess` | 11020 | 10892 | 10892 | **10892** | N&B | `824ABAC0 stw r11,10892` |
| `FenceCompletion` | 11024 | 10896 | 10896 | **10896** | N&B | KickOff, BlockOnFence, CBlocker |
| bloco de interrupção/Swap (`Sa2CreateDevice` grava `11028`; BL `kBlDeviceOffsetSwapScratch`) | 11028 | 10900 | 10900 | **10900** | N&B | setup aloca 32 B → `10900`; `824A8BA0` chama `[[10900]+16]`; `824A9E00 lwz 10900` |
| `FenceTag` | 11036 | 10908 | 10908 | **10908** | N&B | KickOff, BlockOnFence, IsFencePending |
| `CommandBufferTagMask` | 11040 | 10912 | 10912 | **10912** | N&B | Set*Shader `lwz 10912; and.`; BeginCB `stw r28,10912` / `lis 64` |
| `SwapFenceTag` | 11056 | 10928 | 10928 | **10928** | N&B | Swap `stw r30,10928` |
| `TilePredication` / `RunPredication` | 11060/11064 | 10932/10936 | igual N&B | **10932/10936** | N&B | InitializeApiState `stw r23,10932/10936` |
| `Flags` … `Flags3` (0x80 sem HW; 0x02/0x10/0x20; 4) | 11068–11071 | 10940–10943 | igual N&B | **10940–10943** | N&B | CreateDevice `ori 128`; KickOff `ori 2`; SyncPI `ori 16`; `824A8C38 ori 32`; setup `ori 4` em `10942` |
| (byte novo de flags, não usado pelo Renut) | — | — | — | 10944 | novo | `824A9E00`/`824A9A30 lbz 10944 & 0x80` |
| `kDeviceOffsetVertexDeclaration` | 12240 | 11820 | 11820 | **11992** | novo (N&B +172) | SetVertexDeclaration `stw r4,11992(r3)` |
| qword 0x2E30 da imagem de registos | — | 11824 | 11824 | **12000** | +176 | `824B95B8 std r9,12000` |
| declaração padrão (0x2FA0, 0x2FA4) | — | 12192/12196 | igual | **12368/12372** | +176 | InitializeApiState `stw 12368`=5, `12372`=1 |
| `0x2F9B` (byte de MSAA lido pelos draws, d3d_native_state) | — | 12187 | — | **12363** | +176 | draws e InitializeApiState `lbz 12363` |
| `kDeviceOffsetIndexBuffer` | 12812 | 12436 | 12436 | **12612** | +176 | DrawIndexedVertices `lwz r24,12612`; SetIndices |
| `RenderTargets[4]` / `DepthStencil` | 12816 / 12832 | 12440 / 12456 | igual N&B | **12616 / 12632** | +176 | ClearF `lwz r7,12616`, `lwz r11,12632`; SetRenderTarget `(i+3154)*4` |
| `StreamSources[16]` / `StreamStrides` | 12836 / 12904 | 12460 / 12528 | igual N&B | **12636 / 12704** | +176 | SetStreamSource `(i+3159)*4`, `stb …,12704` |
| `Textures[26]` | 12920 | 12544 | 12544 | **12720** | +176 | SetTexture `(i+3180)*4` |
| viewport (`0x3168`, literal em draw.cpp/state.cpp/render) | (literal N&B) | 12648 | 12648 | **12824** | +176 | SetViewport `stfs 12824…12844` |
| `ScissorRect` | 13052 | 12676 | 12676 | **12852** | +176 | SetScissorRect `addi r4,r3,12852` |
| `PixelShader` / `VertexShader` | 13068 / 13072 | 12692 / 12696 | igual N&B | **12868 / 12872** | +176 | `8249E558`/`8249E718` |
| `Predication` / `RunPredicationState` / `PredicationSet` | 13084/13088/13092 | 12708/12712/12716 | igual N&B | **12884/12888/12892** | +176 | InitializeApiState `stw r10,12884` (−1), `12888`, `12892` |
| `kDeviceSnapshotSize` (até a predicação, inclusive) | 13088 | 0x31A8 | 12712 | **12888** | +176 | dos campos acima |
| `0x31A0` (BeginCB, Z de DepthControl) | — | 12704 | — | **12880** | +176 | `824AC6B0 stw r10,12880` |
| `0x31B0` / `0x31B4` (literais de tiling, draw.cpp/BeginCB) | — | 12720 / 12724 | — | **12896 / 12900** | +176 | BeginCB `stw r11,12896` = 2; SetSurfaces `stw r11,12900` |
| `TilingSurfaces[5]` / `TileCount` / `TileRects` / `TileOrigins` | 13104/13124/13128/13368 | 12728/12748/12752/12992 | igual N&B | **12904/12924/12928/13168** | +176 | SetSurfaces `12904…12920`; BeginCB `stw r24,12924`, rects em `12924+4`, origens `13164+4` |
| `TilingExtent` / `0x3384` | 13556 / — | 13180 / 13188 | — | **13356 / 13364** | +176 | BeginCB `stw r6,13356`, `r7,13360`, `r29,13364` |
| `0x3378` (BeginCB) | — | 13176 | — | **13352** | +176 | `824AC6B0 lwz r9,13352` |
| `0x3390` / `0x33A0` / `0x33A4` (literais da EndTiling, draw.cpp) | — | 13200/13216/13220 | — | 13376/13392/13396 | pos. (?) | o RB2 não tem BeginTiling/EndTiling |
| `kDeviceOffsetInKickOff` | 13600 | 13232 | 13232 | **13408** | +176 | KickOff `lwz r11,13408` |
| `kDeviceOffsetVerticesRingEnd` | 13844 | 13444 | 13444 | **não existe** | — | sem BeginVertices (linha 21) |
| `kDeviceOffsetCommandBuffer` | 13920 | 13516 | 13516 | **13728** | +212 | AllocateInlineData `lwz r3,13728`; BeginCB `stw r30,13728` |
| `0x34D0…0x34E4` (BeginCB zera) | — | 13520…13540 | — | **13732…13752** | +212 | BeginCB `stw r29,13732…13752` |
| cópia de `D3DPRESENT_PARAMETERS` | 13948 | 13544 | 13544 | **13756** | +212 | InitPP `addi r3,r29,13756; li r5,124` |
| `kDeviceOffsetPresentationInterval` (sa2_renut_pacing.cpp) | 14000 | 13596 | 13596 | **13808** | +212 | InitializeApiState `lwz r25,13808`; SyncPI |
| formato do front buffer (0x3528) | — | 13608 | 13608 | **13820** | +212 | InitPP `stw r11,13820`; Swap `lwz 13820` |
| `AutoDepthStencil` / `AutoFrontBuffer` / `AutoBackBuffer` | 15228/15232/15236 | 14824/14828/14832 | igual N&B | **15036/15040/15044** | +212 | InitPP `stw r4,15036`, `stw r3,15040`, `stw r5,15044` |
| `0x3A40` / `0x3A44` / `0x3A50` (BeginCB: tamanho, alocador, início do segmento) | — / 15320 / 15332 | 14912/14916/14928 | igual N&B | **15124/15128/15140** | +212 | BeginCB `stw r11,15124`, `stw r29,15128`, `stw r9,15140`; RBDA `lwz 15128`; KickOffSegment `lwz 15140` |
| `0x3A48` (SetFence) | — | 14920 | 14920 | **15132** | +212 | SetFence `lwz r10,15132` |
| `kDeviceOffsetsMinusOne[2]` | 15340 | 14936 | 14936 | **15148** | +212 | `824ABD50 stw r11,15148` |
| `kDeviceOffsetPresentFenceTag` | 15344 | 14940 | 14940 | **15152** | +212 | Swap `stw r30,15152`; NewSegment espera `15152` |
| `kDeviceOffsetCriticalSection0/1` | 15348/15376 | 14944/14972 | igual N&B | **15156/15184** | +212 | setup `addi r3,r3,15156`, `addi r3,r31,15184` |
| rampa de gama (`kSa2DeviceOffsetGammaRamp`; `kDeviceOffsetGammaRamp` 0x3A9C) | 15408 | 15004 | 15004 | **15216** | +212 | Swap `addi r26,r31,15216` |
| `kDeviceOffsetSwapCount` / Swaps mostrados (`16976` na cola) | 16968 / 16976 | 16560 / — | 16560 / 16568 | **16776 / 16784** | +216 | Swap `lwz/stw 16776`, `lwz 16784`; VBlank `16784`++ |
| `16712` (zerado no setup; spinlock do VBlank) | 16712 (?) | 16712 | 16712 | **16928** | +216 | `824AB9E0 stw r11,16928`; VBlank `addi r30,r3,16928` + spinlock |
| `kDeviceOffsetSideBuffer` / `off::SideBuffer` (0x12C0) | 17136 | 16728 | 16728 | **16944** | +216 | `824ABACC stw r3,16944`; `824A8C38` |
| campos de vídeo (0x5428/0x542C/0x5430/0x5434) | 0x5428… | 21544… | 21540… | **21760/21764/21768/21772** | +216 | InitPP `stw 21760/21768`, `21764`, `stfiwx → 21772` |
| `kDeviceOffsetsMinusOne[0..1]` | 21968/21972 | 21560/21564 | 21556/21560 | **21776/21780** | +216 | setup e `824ABD50 stw r11,21776/21780` |
| `kDeviceOffsetPerfFrequency` (+4 = 1/f) | 21996 | 21588 | 21584 | **21804** | +216 | setup `stfs f13,21804`, `stfs f0,21808` |
| `d.W(0x541C)` (`SetFence`) | — | 21532 | ? | 21748 (?) | pos. | `21748` é lido em `824A89C8`/`824A9B70`/Swap; o papel não foi verificado |
| `kDeviceOffsetTitleMinusOne` (41 palavras) | 23380 | — | 21700 | **23600** | novo | `824B1E60 li r11,41; addi r10,r3,23600` |
| `kDeviceOffsetBehaviorFlags` (`0x5DAC` em d3d_native_state) | 24392 | 23980 | 22300 | **24200** | novo (N&B +220) | CreateDevice `stw r30,24200`; InitializeApiState `lwz r24,24200` |
| `kVdGlobalDeviceImportSlot` | 0x820005F8 | 0x820007FC | 0x820005C4 | **0x8200074C** | novo | setup `lwz r11,1868(0x82000000)`; a imagem tem `0x000101BE` |
| `kDeltaPrivateUnreadBegin/End` (queued.cpp) | 11008–12224 | — | 10880–11776 | **10880–11968** | deduzido | do qword global até antes da declaração (11992), em pedaços de 64 a partir de 10112 |
| `static_assert(kDeltaReadChunks.count == …)` | 40 | — | 39 | **39** | deduzido | 12 da cabeça + 44 da cauda (até 12888) − 17 privados |
| `ChainChanged` (render/draw.cpp): alvos / texturas | 12816–12835 / 12920–13023 | — | 12440–12459 / 12544–12647 | **12616–12635 / 12720–12823** | +176 | dos campos acima |

### Objetos

| O que o Renut lê | MGR/N&B/BL | RB2 | Prova no RB2 |
|---|---|---|---|
| `D3DResource` (Common, RefCount +4, Fence +8) | igual | igual | Release `lwarx` em `+4`; Destroy `lwz r4,8(r31)`; tipo `clrlwi …,28` |
| tipos VertexDeclaration 5, VertexShader 6, PixelShader 7, Surface 4 | igual | igual | `0x00100005` (`8249E900`), `li r11,6` (`8249EE50`), `li r10,7` (`8249E520`), Release testa `== 4` |
| VertexShader: container em +0x368 (872), físico em +0x20 | igual | igual | XGRegisterVertexShader |
| PixelShader: físico em +0x18 | igual | igual | `stw r4,24(r3)` |
| VertexDeclaration: elementos a partir de +52, contagem em +24 | igual ao BL | igual | `8249E900` |
| IndexBuffer: bit 31 de Common, endereço +0x18 | igual | igual | DrawIndexedVertices `lwz r6,0(r24)`, `lwz r10,24(r24)` |
| VertexBuffer: endereço +0x18, tamanho +0x1C | igual | igual | SetStreamSource `lwz r10,24(r5)`, `lwz r5,28(r5)` |
| Surface: Size +0x24, base de EDRAM +0x1C, EdramSize +0x2C | igual | igual | ClearF `lwz r9,36(r11)`; Destroy `lwz r11,44` / 5120, `lwz r9,28` |
| `D3DCommandBuffer`: fence +0x08, tags +0x18 e +0x40, flags +0x6C, +0x70…+0x90, memória fixa +0x98, tamanho +0xA8, +0xAC | N&B | igual ao N&B | BeginCB `addi r25,r30,24`, cópia a partir de `+64`, `lwz 108`, `stw 112…144`, `lwz 152`, `lwz 168`; NewSegment `lwz 172(r3)`; EndCB `stw 136/140/144` |

## O layout do Renut é paramétrico?

**Não.** O patch do BL (`Dragon-Burst/patches/renut/0100-burst-limit-layout-and-hooks.patch`) trocou os valores no
lugar: `constexpr` em `d3d.h`, `d3d_native.h` (`off::`), `hooks/device.cpp`, `backend/queued.cpp` (faixa e
`static_assert` da contagem), `render/draw.cpp` (faixas do `ChainChanged`), `sa2_renut_pacing.cpp` e literais da cola
(`16568`), e trocou o `mgr_hook_map.h` pelo `bl_hook_map.h`. O `Sa2CreateDevice` saiu e entrou o
`BlHardwareDeviceSetup`. Um build serve só um jogo. Ficam ainda literais do N&B espalhados (`0x3168`, `0x31B0`,
`0x31B4`, `0x3384`, `0x3390`, `0x33A0`, `0x33A4`, `0x3548`, `0x3484`, `0x351C`, `0x5DAC`, `0x2E2C…0x2F9B`,
`0x3A40…0x3A50`, `0x34D0…0x34E4`, `0x3A9C`, `0x541C`, `0x5428…0x5434`) que só não fazem mal porque o caminho do MGR
não os executa.

Para o RB2 ser o terceiro layout: ou um patch próprio que troque os mesmos ~70 valores pelos da tabela acima (o
caminho do BL), ou um cabeçalho de layout por jogo escolhido em tempo de compilação (`renut_layout_rb2.h` com
`constexpr`), já que `kDeviceAllocSize` dimensiona o `struct Device` e a contagem de pedaços entra em `static_assert`.
Os literais N&B acima têm de virar nomes nesse cabeçalho, porque o RB2 **executa** parte deles (command buffers).

## Como os hooks do MGR se transportam

- **Iguais em assinatura** (o hook do Renut serve com o endereço trocado; os corpos usam os offsets do RB2 da tabela):
  KickOff, KickOffSegment, BeginRingBig, RingBufferDeviceAllocate, AllocateInlineData, NewSegment, BlockOnFence (o
  NoKick está em r7, ignorado), CBlocker_Check, IsFencePending, **InsertCallback** (agora sim igual),
  SynchronizeToPresentationInterval, Swap, DrawVertices, DrawIndexedVertices, ClearF, Resolve, WindowScissor,
  XGRegisterVertexShader, **XGRegisterPixelShader** (agora sim função própria), CreateVertexDeclaration,
  D3DResource_Destroy (com `GameXMemFree` = `8218D028`).
- **Volta o caminho do MGR na criação:** hook em `8249AD90` com o `Sa2CreateDevice` (original com tipo 2; para o
  título: secções críticas `15156/15184`, VdGlobalDevice `0x8200074C`, BehaviorFlags `24200` com a mesma regra
  `0x100`/`0x3F000000` → `|= 0x0C000000`, −1 em `21776/21780`, frequência `21804`, `824AB5E0` e `824AB378`). A
  acrescentar ao do MGR: os 41 × −1 em `23600`, o zero em `16928` (spinlock do VBlank) e `10942 |= 4` como o setup
  real. O BL não serve de modelo aqui (lá a criação estava embutida no jogo).
- **A reescrever ou ligar:**
  - Declarações: hook cru em `8249E900` (`pElements` = r3, `pDecl` = r4), que é o que o jogo chama. Sem ele o Renut
    não conhece nenhuma declaração do jogo.
  - **Command buffers** (não estão no mapa do MGR): o jogo grava em dois devices de tipo 2 (`823A4928`: Acquire,
    BeginCommandBuffer `824A44D0` com Flags = 0, draws, EndCommandBuffer `824A4A28`, Release) e executa no device do
    título (`823A87D8`: RunCommandBuffer `824A4E48`, `PredicationSelect` = 0). O Renut tem os hooks do N&B
    (`D3DDevice_Begin/End/RunCommandBuffer_hook`, `command_buffers.cpp`), não ligados no MGR, com assinaturas que
    batem (Begin: `r6` tags, `r8` rects, `r9` TileCount). Os literais N&B desses hooks e do `UnsetAll`/
    `UpdateTilePredicated` (`d3d_native_state.cpp`) têm de ir para o layout do RB2 (tabela acima).
  - VBlank: a cola bate `8249AED8(device)` a 60 Hz (ver acima).
  - SetRingBufferParameters: `824AA578` sem efeito (só a desmontagem chega lá).
- **Dispensáveis:** BeginVertices (não existe; o hook e o `kDeviceOffsetVerticesRingEnd` saem), EngineSwapProgress.
  O tiling predicado (BeginTiling/EndTiling do N&B) também não entra: o RB2 não tem essas funções e só o
  BeginCommandBuffer escreve `TileCount`, sempre com Flags = 0.

## Questões em aberto

1. Draws gravados em command buffer nos devices de tipo 2, em threads de trabalho: o caminho do N&B no Renut nunca
   correu no Android. Falta ver se o `RecordRun`/`QueueRunCommandBuffer` aguenta o padrão do RB2 (dois buffers por
   frame, alternados por `[r31+312]+7`).
2. `0x541C` (o teste do SetFence com fences imediatas): candidato `21748` pela posição, não verificado.
3. `kDeviceSnapshotSize = 12888` corta antes de `TilingSurfaces` (`12904`), como no N&B; o RB2 não usa tiling,
   mas o `ClearF`/`WindowScissor` originais testam `10940 & 0x10/0x20`.
4. Os devices de tipo 2 recebem o bloco de fences do `Sa2CreateDevice`, mas o `824ABD50` original corre antes e chama
   KickOff (já com hook) com `10896` = 0: ver se o `KickFence` tolera completion nulo (no MGR o caso é o mesmo).
5. A thread `824B91A8` e o leitor XMV (`824CCA90`, `8249BD10`) também chamam Swap e InsertCallback: só aparecem nos
   vídeos e na interface do sistema; ver em execução.
6. `16756` (callback de VBlank) é escrito também por código do jogo noutro objeto (`82312D78`, `82313350`): é outra
   estrutura com o mesmo offset, não o device (confirmar em execução).
