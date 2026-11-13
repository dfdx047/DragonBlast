#!/usr/bin/env python3
"""One-off editor that turns the Renut tree (mgr-nx-deko + Dragon Burst's patches/renut 0001-0120, Burst Limit's
layout) into Raging Blast 2's: device layout (XDK 2.0.12625, docs/renut/HOOKMAP_RB2.md) and hook addresses.
Its result is stored as patches/renut/0200-raging-blast-2-layout-and-hooks.patch; this script documents how it was
made. Every replacement must match exactly once (or the given count)."""
import pathlib
import sys

ROOT = pathlib.Path(sys.argv[1]) / 'app/src/renut_engine/gpu'


def rep(rel, old, new, count=1):
    p = ROOT / rel
    s = p.read_text()
    n = s.count(old)
    if n != count:
        sys.exit(f'{rel}: expected {count} x {old!r}, found {n}')
    p.write_text(s.replace(old, new))


# d3d.h ---------------------------------------------------------------------------------------------------------------
H = 'd3d/d3d.h'
rep(H, 'constexpr u32 kDeviceAllocSize = 0x5780;  // Dragon Burst: BL 6683 (820F92F8 memset 22400)',
    'constexpr u32 kDeviceAllocSize = 0x5F00;  // Dragon Blast: RB2 12625 (8249AD90 li r3,24320)')
rep(H, 'constexpr u32 kDeviceOffsetBehaviorFlags = 22300;', 'constexpr u32 kDeviceOffsetBehaviorFlags = 24200;')
for name, bl, rb2 in [('VertexDeclaration', 11820, 11992), ('IndexBuffer', 12436, 12612),
                      ('RenderTargets', 12440, 12616), ('DepthStencil', 12456, 12632),
                      ('StreamSources', 12460, 12636), ('StreamStrides', 12528, 12704), ('Textures', 12544, 12720),
                      ('PixelShader', 12692, 12868), ('VertexShader', 12696, 12872), ('Predication', 12708, 12884),
                      ('CommandBuffer', 13516, 13728), ('VerticesRingEnd', 13444, 13620),
                      ('SwapCount', 16560, 16776), ('PresentFenceTag', 14940, 15152)]:
    rep(H, f'constexpr u32 kDeviceOffset{name} = {bl};', f'constexpr u32 kDeviceOffset{name} = {rb2};')
rep(H, 'constexpr u32 kDeviceSnapshotSize = 12712;', 'constexpr u32 kDeviceSnapshotSize = 12888;  // Dragon Blast: RB2')

# d3d_native.h off:: --------------------------------------------------------------------------------------------------
N = 'd3d/d3d_native.h'
for name, bl, rb2 in [('ScissorRect', 12676, 12852), ('IndexBuffer', 12436, 12612), ('RenderTargets', 12440, 12616),
                      ('DepthStencil', 12456, 12632), ('StreamSources', 12460, 12636),
                      ('StreamStrides', 12528, 12704), ('Textures', 12544, 12720), ('Predication', 12708, 12884),
                      ('RunPredicationState', 12712, 12888), ('PredicationSet', 12716, 12892),
                      ('TilingSurfaces', 12728, 12904), ('TileCount', 12748, 12924), ('TileRects', 12752, 12928),
                      ('TileOrigins', 12992, 13168), ('TilingExtent', 13180, 13356),
                      ('CommandBuffer', 13516, 13728), ('SideBuffer', 16728, 16944),
                      ('AutoDepthStencil', 14824, 15036), ('AutoFrontBuffer', 14828, 15040),
                      ('AutoBackBuffer', 14832, 15044)]:
    pad = ' ' * max(1, 20 - len(name) - 3)
    old = f'constexpr u32 {name} = {bl};'
    rep(N, old, f'constexpr u32 {name} = {rb2};')
rep(N, '// Dragon Burst (2026-10-27): Burst Limit\'s XDK 6683 is N&B\'s layout, the values in brackets (the numbers below).',
    '// Dragon Blast (2026-11-13): Raging Blast 2\'s XDK 12625 (docs/renut/HOOKMAP_RB2.md): N&B\'s up to 10943, then\n'
    '// +176 (bound objects to InKickOff), +212 (command buffer to the gamma ramp), +216 (Swap count on). N&B in brackets.')

# d3d_native_state.cpp: N&B literals on paths RB2 runs (draws, UnsetAll, tile predication) ----------------------------
S = 'd3d/d3d_native_state.cpp'
for old, new in [('constexpr u32 kPresentInterval = 0x351C;', 'constexpr u32 kPresentInterval = 13808;  // RB2 [0x351C]'),
                 ('constexpr u32 kBehaviorFlags2 = 0x5DAC;', 'constexpr u32 kBehaviorFlags2 = 24200;  // RB2 [0x5DAC]'),
                 ('constexpr u32 kViewport = 0x3168;', 'constexpr u32 kViewport = 12824;  // RB2 [0x3168]'),
                 ('constexpr u32 kVertexDeclaration = 0x2E2C;', 'constexpr u32 kVertexDeclaration = 11992;  // RB2 [0x2E2C]'),
                 ('constexpr u32 kVertexShader = 0x3198;', 'constexpr u32 kVertexShader = 12872;  // RB2 [0x3198]'),
                 ('constexpr u32 kPixelShader = 0x3194;', 'constexpr u32 kPixelShader = 12868;  // RB2 [0x3194]'),
                 ('!device.B(0x2F9B)', '!device.B(12363 /* RB2 [0x2F9B] */)'),
                 ('device.W(0x3374)', 'device.W(13348 /* RB2 [0x3374] */)'),
                 ('constexpr u32 kExportCount = 0x34C0;', 'constexpr u32 kExportCount = 13716;  // RB2 [0x34C0] +212 (?)'),
                 ('constexpr u32 kExportNanVertexDone = 0x34C4;', 'constexpr u32 kExportNanVertexDone = 13720;'),
                 ('constexpr u32 kExportFlags = 0x34C8;', 'constexpr u32 kExportFlags = 13724;')]:
    rep(S, old, new)

# queued.cpp: private words the render thread does not read ------------------------------------------------------------
Q = 'backend/queued.cpp'
rep(Q, 'constexpr u32 kDeltaPrivateUnreadBegin = 10880, kDeltaPrivateUnreadEnd = 11776;',
    'constexpr u32 kDeltaPrivateUnreadBegin = 10880, kDeltaPrivateUnreadEnd = 11968;  // Dragon Blast: RB2 (decl 11992)')
rep(Q, 'static_assert(kDeltaReadChunks.count == 39);  // Dragon Burst: BL (MGR 40)',
    'static_assert(kDeltaReadChunks.count == 39);  // Dragon Blast: RB2 (BL 39, MGR 40)')

# render/draw.cpp: the bound targets and textures -----------------------------------------------------------------------
D = 'render/draw.cpp'
rep(D, 'ChainChanged(chain, 12440, 12459)', 'ChainChanged(chain, 12616, 12635)')
rep(D, 'ChainChanged(chain, 12544, 12647)', 'ChainChanged(chain, 12720, 12823)')

# sa2_renut_pacing.cpp ------------------------------------------------------------------------------------------------
rep('sa2_renut_pacing.cpp', 'constexpr uint32_t kDeviceOffsetPresentationInterval = 13596;',
    'constexpr uint32_t kDeviceOffsetPresentationInterval = 13808;  // Dragon Blast: RB2 (InitializeApiState lwz 13808)')

# banjo_shared_hooks.h --------------------------------------------------------------------------------------------------
B = 'banjo_shared_hooks.h'
rep(B, 'renut_native_sub_820A8A20', 'renut_native_sub_824A7FF0')
rep(B, 'renut_native_sub_820A8E10', 'renut_native_sub_824A8408')
rep(B, 'renut_native_sub_820B64C0', 'renut_native_sub_8249B688')
print('layout edits done')

# hooks/device.cpp ------------------------------------------------------------------------------------------------------
V = 'd3d/hooks/device.cpp'
rep(V, '#include "renut_engine/gpu/d3d/bl_hook_map.h"  // Dragon Burst: the Burst Limit\'s guards',
    '#include "renut_engine/gpu/d3d/rb2_hook_map.h"  // Dragon Blast: Raging Blast 2\'s guards')
for old, new in [
        ('constexpr u32 kDeviceOffsetInKickOff = 13232;', 'constexpr u32 kDeviceOffsetInKickOff = 13408;  // RB2 (KickOff lwz 13408)'),
        ('constexpr u32 kDeviceOffsetSideBuffer = 16728;', 'constexpr u32 kDeviceOffsetSideBuffer = 16944;  // RB2'),
        ('constexpr u32 kDeviceOffsetsMinusOne[] = {21556, 21560, 14936};  // Dragon Burst: BL (N&B - 4: 820B7230)',
         'constexpr u32 kDeviceOffsetsMinusOne[] = {21776, 21780, 15148};  // Dragon Blast: RB2 (824ABD50)'),
        ('constexpr u32 kGlobalQwordVa = 0x827252E8;  // Dragon Burst: BL (820F94C8)',
         'constexpr u32 kGlobalQwordVa = 0x8260EBB8;  // Dragon Blast: RB2 (824ABA88)'),
        ('constexpr u32 kDeviceOffsetCriticalSection0 = 14944;', 'constexpr u32 kDeviceOffsetCriticalSection0 = 15156;  // RB2'),
        ('constexpr u32 kDeviceOffsetCriticalSection1 = 14972;', 'constexpr u32 kDeviceOffsetCriticalSection1 = 15184;  // RB2'),
        ('constexpr u32 kDeviceOffsetPerfFrequency = 21584;  // Dragon Burst: BL (N&B - 4)',
         'constexpr u32 kDeviceOffsetPerfFrequency = 21804;  // Dragon Blast: RB2 (824ABAE8 stfs 21804)'),
        ('constexpr u32 kDeviceOffsetTitleMinusOne = 21700;  // Dragon Burst: BL (820B7428)',
         'constexpr u32 kDeviceOffsetTitleMinusOne = 23600;  // Dragon Blast: RB2 (824B1E60)'),
        ('constexpr u32 kVdGlobalDeviceImportSlot = 0x820005C4;  // Dragon Burst: BL (820B72C8)',
         'constexpr u32 kVdGlobalDeviceImportSlot = 0x8200074C;  // Dragon Blast: RB2 (824ABAE8 lwz 1868)'),
        ('constexpr u32 kSa2DeviceOffsetGammaRamp = 15004;', 'constexpr u32 kSa2DeviceOffsetGammaRamp = 15216;  /* RB2 */'),
        ('GuestPtr<be_u32>(device_va + 16568)  /* Dragon Burst: BL (VBlank 820B5CF8) */',
         'GuestPtr<be_u32>(device_va + 16784)  /* Dragon Blast: RB2 (VBlank 8249AED8) */'),
        ('constexpr u32 kDeviceOffsetGammaRamp = 0x3A9C;', 'constexpr u32 kDeviceOffsetGammaRamp = 15216;  // RB2 [0x3A9C]'),
        ('if (!d.W(0x541C)) {', 'if (!d.W(21748 /* RB2 [0x541C] (?) */)) {'),
        ('completion[1] = (d.W(0x3A48) & 3) | d.W(off::Ring);', 'completion[1] = (d.W(15132 /* RB2 [0x3A48] */) & 3) | d.W(off::Ring);'),
        # BeginCommandBuffer / ConfigureForOnePassZPass (N&B literals; RB2 values from the hook map table)
        ('d.SetW(0x3338 + i * 4,', 'd.SetW(13288 /* RB2 [0x3338] */ + i * 4,'),
        ('d.SetW(0x3374, pitch);', 'd.SetW(13348 /* RB2 [0x3374] */, pitch);'),
        ('d.SetW(0x3378, ((unit', 'd.SetW(13352 /* RB2 [0x3378] */, ((unit'),
        ('(d.W(0x2E4C) & 0xE0000)', '(d.W(12028 /* RB2 [0x2E4C] */) & 0xE0000)'),
        ('d.SetW(0x3A50, 8);', 'd.SetW(15140 /* RB2 [0x3A50] */, 8);'),
        ('d.SetW(0x3A44, 0);', 'd.SetW(15128 /* RB2 [0x3A44] */, 0);'),
        ('d.SetW(0x3A40, bw(kBufferAllocatorSize));', 'd.SetW(15124 /* RB2 [0x3A40] */, bw(kBufferAllocatorSize));'),
        ('for (u32 offset = 0x34D0; offset <= 0x34E4; offset += 4)',
         'for (u32 offset = 13732 /* RB2 [0x34D0] */; offset <= 13752; offset += 4)'),
        ('    d.SetW(0x3384, 0);\n    d.SetW(0x31B0, 2);',
         '    d.SetW(13364 /* RB2 [0x3384] */, 0);\n    d.SetW(12896 /* RB2 [0x31B0] */, 2);'),
        ('d.SetW(0x31A0, native::Extract', 'd.SetW(12880 /* RB2 [0x31A0] */, native::Extract'),
        ('if (d.W(0x3378) != (d.W(native::off::SurfaceInfo)', 'if (d.W(13352) != (d.W(native::off::SurfaceInfo)'),
]:
    rep(V, old, new)
rep(V, 'd.SetW(16712, 0);', 'd.SetW(16928, 0);  // RB2: the VBlank handler\'s spinlock (824AB9E0)', 2)

# The device setup: RB2's own (8249AD90) creates the device, runs XenonGpu_Initialize (824ABA78: the fields every
# device gets) and then, for the title device, the hardware setup 824ABAE8 (CDevice*, D3DPRESENT_PARAMETERS*) -> 1,
# the hook (Burst Limit's shape); for a device of type 2 the no-hardware path 824ABD50 (CDevice*) -> 1.
for old, new in [('REX_EXTERN(__imp__sub_820B7670);  // XenonGpu_Initialize (CDevice*)',
                  'REX_EXTERN(__imp__sub_824B9598);  // the startup register image (BL\'s "XenonGpu_Initialize") (CDevice*)'),
                 ('REX_EXTERN(__imp__sub_820B6E38);', 'REX_EXTERN(__imp__sub_824AB5E0);'),
                 ('REX_EXTERN(__imp__sub_820B6B58);', 'REX_EXTERN(__imp__sub_824AB378);'),
                 ('__imp__sub_820B7670(ctx, base);', '__imp__sub_824B9598(ctx, base);'),
                 ('__imp__sub_820B6E38(ctx, base);', '__imp__sub_824AB5E0(ctx, base);'),
                 ('__imp__sub_820B6B58(ctx, base);', '__imp__sub_824AB378(ctx, base);'),
                 ('for (const auto &guard : bl::kGuestCodeGuards) {', 'for (const auto &guard : rb2::kGuestCodeGuards) {'),
                 ('RGPU_ERROR("Burst Limit XDK hook validation failed:', 'RGPU_ERROR("Raging Blast 2 XDK hook validation failed:'),
                 ('RGPU_INFO("Burst Limit XDK 6683: all {} guest interface guards match", std::size(bl::kGuestCodeGuards));',
                  'RGPU_INFO("Raging Blast 2 XDK 12625: all {} guest interface guards match", std::size(rb2::kGuestCodeGuards));'),
                 ('RGPU_INFO("Guest D3D device (Burst Limit) set up at {:#010x}", device_va);',
                  'RGPU_INFO("Guest D3D device (Raging Blast 2) set up at {:#010x}", device_va);')]:
    rep(V, old, new)
a = (ROOT / V).read_text()
i = a.index('  // The hooks of the Burst Limit map (docs/renut/HOOKMAP_BL.md) installed')
j = a.index('  ctx.r3.u64 = 1;\n}\n', i)
a = a[:i] + ('  RGPU_INFO("Renut hooks installed (Raging Blast 2 XDK 12625): device 824ABAE8 824ABD50 824AA520 8249B258 "\n'
             '            "824AA1F8 824AA070 824AA430 824A8CB0 824A8EE0 824A9370 824AA578 8249B688 824A8FC8 8249A6C8 "\n'
             '            "824AA338; command buffers 824A44D0 824A4A28 824A4E48; draw 824A7FF0 824A8408 824A01C8 "\n'
             '            "8249DA40 824A03B0; shader 8249EE50 8249E520 8249E900; resource 824A7970; glue VBlank 8249AED8");\n') + a[j:]
# Burst Limit's InsertCallback stand-in goes; RB2 has MGR's D3DDevice_InsertCallback (824AA520).
i = a.index('// The interrupt packet writer (0x820AE868')
j = a.index('// SA2\'s ring segment allocator (sub_826B45A8', i)
a = a[:i] + '''// Dragon Blast: a device of type 2 (RB2 records its command buffers on two of them): D3D's no-hardware path
// (0x824ABD50: the side buffer as ring, the -1 words, the register image, InitializeApiState), with the fence block,
// the sink and immediate fences of MGR's no-hardware device (Sa2CreateDevice) around it.
void Rb2NoHardwareSetup(PPCContext &ctx, uint8_t *base) {
  const u32 device_va = ctx.r3.u32;
  native::Device d = native::DeviceAt(device_va);
  if (!Rb2GuestCodeMatches(base) || !d || !records::Init()) {
    ctx.r3.u64 = 0;
    return;
  }
  if (!g_ring_sink)
    g_ring_sink = records::AllocPhysicalBlock(kRingSinkSize);
  if (!g_ring_data)
    g_ring_data = records::AllocPhysicalBlock(kRingDataSize);
  const u32 fence_va = records::AllocPhysicalBlock(0x80 + 0x20);
  if (!g_ring_sink || !g_ring_data || !fence_va) {
    RGPU_ERROR("Raging Blast 2 no-hardware device: guest allocation failed");
    ctx.r3.u64 = 0;
    return;
  }
  std::memset(GuestPtr<u8>(fence_va), 0, 0x80 + 0x20);
  u8 *bytes = d.bytes;
  WriteU32(bytes, kDeviceOffsetFenceCompletion, fence_va);
  WriteU32(bytes, kRb2DeviceOffsetSwapScratch, fence_va + 0x80);
  bytes[kDeviceOffsetFlags2] |= kDeviceFlagImmediateFences;
  __imp__sub_824ABD50(ctx, base);
  // Its ring was the side buffer, marked out of memory (0x824A8C38): from here the sink.
  bytes[kDeviceOffsetFlags2] &= u8(~0x20);
  bytes[kDeviceOffsetFlags2] |= kDeviceFlagImmediateFences;
  native::RewindRing(d);
  RGPU_INFO("Guest D3D device of type 2 (Raging Blast 2) set up at {:#010x}", device_va);
  ctx.r3.u64 = 1;
}

''' + a[j:]
a = a.replace('REX_EXTERN(__imp__sub_824AB378);  // InitializeApiState (CDevice*)',
              'REX_EXTERN(__imp__sub_824AB378);  // InitializeApiState (CDevice*)\n'
              'REX_EXTERN(__imp__sub_824ABD50);  // the no-hardware path of a device of type 2 (CDevice*) -> 1', 1)
a = a.replace('kBlDeviceOffsetSwapScratch', 'kRb2DeviceOffsetSwapScratch').replace('kBlDeviceOffsetFlags3', 'kRb2DeviceOffsetFlags3')
a = a.replace('BlGuestCodeMatches', 'Rb2GuestCodeMatches').replace('BlHardwareDeviceSetup', 'Rb2HardwareDeviceSetup')
a = a.replace('"Burst Limit device setup:', '"Raging Blast 2 device setup:')
# The hook list.
i = a.index("// Dragon Burst (2026-10-27): the Burst Limit's addresses (docs/renut/HOOKMAP_BL.md; MGR's in brackets).")
j = a.index('[[maybe_unused]] static auto *const kBlUnusedIsFencePending = &IsFencePending_hook;')
j = a.index('\n', j) + 1
a = a[:i] + '''// Dragon Blast (2026-11-13): Raging Blast 2's addresses (docs/renut/HOOKMAP_RB2.md; MGR's in brackets).
BANJO_RENUT_HOOK_RAW(sub_824ABAE8) {  // hardware device setup [MGR Direct3D_CreateDevice sub_826B2418]
  Rb2HardwareDeviceSetup(ctx, base);
}
BANJO_RENUT_HOOK_RAW(sub_824ABD50) {  // no-hardware path of a device of type 2
  Rb2NoHardwareSetup(ctx, base);
}
BANJO_RENUT_HOOK(sub_824AA520, D3DDevice_InsertCallback_hook);  // [sub_826B59B0]
BANJO_RENUT_HOOK(sub_8249B258, Sa2SynchronizeToPresentationInterval_hook);  // [sub_826B3050]
BANJO_RENUT_HOOK(sub_824AA1F8, KickOff_hook);  // CDevice::KickOff [sub_826B5688]
BANJO_RENUT_HOOK(sub_824AA070, KickOffSegment_hook);  // [sub_826B5348]
BANJO_RENUT_HOOK(sub_824AA430, BeginRingBig_hook);  // [sub_826B58C0]
BANJO_RENUT_HOOK(sub_824A8CB0, RingBufferDeviceAllocate_hook);  // [sub_826B4078]
BANJO_RENUT_HOOK(sub_824A8EE0, AllocateInlineData_hook);  // [sub_826B44C0]
BANJO_RENUT_HOOK(sub_824A9370, Sa2NewSegment_hook);  // NewSegment [sub_826B45A8]
// The ring setup (0x824AA578), SetRingBufferParameters' internal equivalent: only the device teardown (0x824ABE20)
// reaches it once the setup above is the hook.
BANJO_RENUT_HOOK(sub_824AA578, D3DDevice_SetRingBufferParameters_hook);  // [sub_826B5A08]
BANJO_RENUT_HOOK_SHARED(sub_8249B688, D3DDevice_Swap_hook);  // D3DDevice_Swap [sub_826B3510]: the glue's hook calls it
BANJO_RENUT_HOOK(sub_824A8FC8, BlockOnFence_hook);  // (CDevice*, Fence, Type, Object, NoKick) [sub_826B42A8]
BANJO_RENUT_HOOK(sub_8249A6C8, CBlocker_Check_hook);  // [sub_826B1C88]
BANJO_RENUT_HOOK(sub_824AA338, IsFencePending_hook);  // [sub_826B57C8]
// Command buffers: the game records on its two devices of type 2 and runs them on the title device (N&B's hooks).
BANJO_RENUT_HOOK(sub_824A44D0, D3DDevice_BeginCommandBuffer_hook);
BANJO_RENUT_HOOK(sub_824A4A28, D3DDevice_EndCommandBuffer_hook);
BANJO_RENUT_HOOK(sub_824A4E48, D3DDevice_RunCommandBuffer_hook);
// Not in this game: MGR's EngineSwapProgress [sub_826B2FC0].
''' + a[j:]
(ROOT / V).write_text(a)

# hooks/draw.cpp --------------------------------------------------------------------------------------------------------
W = 'd3d/hooks/draw.cpp'
rep(W, 'BANJO_RENUT_HOOK_SHARED(sub_820A8A20, D3DDevice_DrawVertices_hook);  // Dragon Burst: BL [MGR sub_826BC928]',
    'BANJO_RENUT_HOOK_SHARED(sub_824A7FF0, D3DDevice_DrawVertices_hook);  // Dragon Blast: RB2 [MGR sub_826BC928]')
rep(W, 'BANJO_RENUT_HOOK_SHARED(sub_820A8E10, D3DDevice_DrawIndexedVertices_hook);',
    'BANJO_RENUT_HOOK_SHARED(sub_824A8408, D3DDevice_DrawIndexedVertices_hook);')
rep(W, "BANJO_RENUT_HOOK(sub_820A8538, D3DDevice_BeginVertices_hook);  // [MGR sub_826BC468]; BL's DrawVerticesUP 820A89D8 calls it  // D3DDevice_BeginVertices [0x8222CC50]",
    "// Raging Blast 2 has no BeginVertices / DrawVerticesUP (HOOKMAP_RB2.md line 21).\n"
    "[[maybe_unused]] static auto *const kRb2UnusedBeginVertices = &D3DDevice_BeginVertices_hook;")
for bl, rb2 in [('820A9C38', '824A03B0'), ('820BF910', '824A01C8'), ('820BB348', '8249DA40')]:
    rep(W, f'BANJO_RENUT_HOOK_RAW(sub_{bl})', f'BANJO_RENUT_HOOK_RAW(sub_{rb2})')
    rep(W, f'__imp__sub_{bl}(ctx, base);', f'__imp__sub_{rb2}(ctx, base);')

# hooks/shader.cpp ------------------------------------------------------------------------------------------------------
X = 'd3d/hooks/shader.cpp'
rep(X, 'BANJO_RENUT_HOOK_RAW(sub_820A27B8) {  // Dragon Burst: BL [MGR sub_826ACD00]',
    'BANJO_RENUT_HOOK_RAW(sub_8249EE50) {  // Dragon Blast: RB2 [MGR sub_826ACD00]')
rep(X, '__imp__sub_820A27B8(ctx, base);', '__imp__sub_8249EE50(ctx, base);')
s = (ROOT / X).read_text()
i = s.index('// D3DPixelShader *D3DDevice_CreatePixelShader(const DWORD *pFunction) [MGR sub_8255E568]')
j = s.index('// D3DVertexDeclaration *D3DDevice_CreateVertexDeclaration(const D3DVERTEXELEMENT9*)', i)
s = s[:i] + '''// void XGRegisterPixelShader(D3DPixelShader*, void *pPhysicalPart) [MGR sub_826AC3D8]: Raging Blast 2 has it as its
// own function again (56 bytes); the game (823D8BD0) and CreatePixelShader (8249ED20) call it.
BANJO_RENUT_HOOK_RAW(sub_8249E520) {
  const u32 shader = ctx.r3.u32, physical = ctx.r4.u32;
  __imp__sub_8249E520(ctx, base);
  RegisterGuestShader(shader, physical, true);
}
[[maybe_unused]] static auto *const kRb2UnusedRegisterGuestPixelShader = &RegisterGuestPixelShader;

''' + s[j:]
k = s.index('BANJO_RENUT_HOOK_RAW(sub_820A2C78) {  // Dragon Burst: BL [MGR sub_826AC938]')
e = s.index('}\n', s.index('RegisterGuestDeclaration(declaration, elements);', k)) + 2
s = s[:k] + '''// The declaration initializer (0x8249E900: pElements r3, the declaration r4; Common 0x00100005, count at +24,
// elements from +52). Raging Blast 2's game allocates its declarations itself and calls this directly (823D2AF0);
// CreateVertexDeclaration (0x8249E9F0, only the XMV player) calls it too, so this one hook sees every declaration.
BANJO_RENUT_HOOK_RAW(sub_8249E900) {
  const u32 elements_va = ctx.r3.u32, declaration = ctx.r4.u32;
  __imp__sub_8249E900(ctx, base);
  if (const auto *elements = declaration ? GuestPtr<const GuestVertexElement>(elements_va) : nullptr)
    RegisterGuestDeclaration(declaration, elements);
}
''' + s[e:]
(ROOT / X).write_text(s)

# hooks/resource.cpp ----------------------------------------------------------------------------------------------------
R = 'd3d/hooks/resource.cpp'
rep(R, "REX_IMPORT(__imp__sub_82506F18, GameXMemFree, void(u32, u32));  // Dragon Burst: BL's XMemFree (820A9268 calls it)",
    "REX_IMPORT(__imp__sub_8218D028, GameXMemFree, void(u32, u32));  // Dragon Blast: RB2's XMemFree (824A7970 calls it)")
rep(R, 'BANJO_RENUT_HOOK_RAW(sub_820A9268) {  // Dragon Burst: BL [MGR sub_826AAF80]',
    'BANJO_RENUT_HOOK_RAW(sub_824A7970) {  // Dragon Blast: RB2 [MGR sub_826AAF80]')
rep(R, '__imp__sub_820A9268(ctx, base);', '__imp__sub_824A7970(ctx, base);')
print('hook edits done')
