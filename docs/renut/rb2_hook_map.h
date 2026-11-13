// Dragon Ball: Raging Blast 2 (default.xex USA Rev 1, XDK 2.0.12625): code guards for Renut's hooks.
// Same format as mgr-nx-deko's d3d/mgr_hook_map.h and Dragon-Burst's bl_hook_map.h: N bytes of the original image at
// each address (64, or the whole function when it is shorter) and their FNV-32 (offset basis 2166136261, prime
// 16777619). Image: build/renut/map/rb2.img (PE image, base 0x82000000; never commit it).
// Evidence for each address: docs/renut/HOOKMAP_RB2.md.
#pragma once
#include <cstdint>
namespace renut::gpu::rb2 {
struct GuestCodeGuard { uint32_t address, bytes, fnv32; const char* name; };
inline constexpr GuestCodeGuard kGuestCodeGuards[] = {
  // Device creation. Unlike Burst Limit, RB2 has its own Direct3D_CreateDevice (MGR's shape): the hook goes on it, as
  // MGR's Sa2CreateDevice (the original with type 2, then the title device's hardware-path fields).
  {0x8249AD90u, 64u, 0x69DC8254u, "Direct3D_CreateDevice"},
  {0x824AB5E0u, 64u, 0x6A754C03u, "InitializePresentationParameters"},
  {0x824AB378u, 64u, 0x33DC4B18u, "InitializeApiState"},
  {0x824ABA78u, 64u, 0x253C4EFFu, "XenonGpu_Initialize"},  // MGR's role: the fields every device gets (10880..16944)
  {0x824ABAE8u, 64u, 0x338BEFB0u, "HardwareDeviceSetup_reference"},
  {0x824B9598u, 64u, 0x521A124Au, "XenonGpu_RegisterImage"},  // not one of the 29; the type-2 path runs it
  // Ring, segments and fences.
  {0x824AA1F8u, 64u, 0xAC6F9753u, "KickOff"},
  {0x824AA070u, 64u, 0xECA6F51Eu, "KickOffSegment"},
  {0x824AA430u, 64u, 0xD69E8C65u, "BeginRingBig"},
  {0x824A8CB0u, 64u, 0xBD7A0F71u, "RingBufferDeviceAllocate"},
  {0x824A8EE0u, 64u, 0x6BEB05D2u, "AllocateInlineData"},
  {0x824A9370u, 64u, 0x35B324D0u, "NewSegment"},
  {0x824A8FC8u, 64u, 0x082272F0u, "BlockOnFence"},  // (CDevice*, Fence, Type, Object, NoKick)
  {0x8249A6C8u, 64u, 0x54BA0E18u, "CBlocker_Check"},
  {0x824AA338u, 64u, 0xA8F6A9D8u, "IsFencePending"},
  {0x824AA520u, 64u, 0x13F51EFAu, "InsertCallback"},
  // Presentation.
  {0x8249B258u, 64u, 0xB5B26183u, "SynchronizeToPresentationInterval"},
  {0x8249B688u, 64u, 0x98CB2118u, "Swap"},
  // Draws, clear, resolve. No BeginVertices / DrawVerticesUP in this image.
  {0x824A7FF0u, 64u, 0xF6A99427u, "DrawVertices"},
  {0x824A8408u, 64u, 0xB44D7E37u, "DrawIndexedVertices"},
  {0x824A01C8u, 64u, 0x5209804Cu, "ClearF"},
  {0x8249DA40u, 64u, 0x318E4263u, "Resolve"},
  {0x824A03B0u, 64u, 0xD3749F88u, "WindowScissor"},
  // Shaders, declarations, resources.
  {0x8249EE50u, 64u, 0xDD902FF9u, "XGRegisterVertexShader"},
  {0x8249E520u, 56u, 0x0FA989DAu, "XGRegisterPixelShader"},  // the whole function (56 bytes)
  {0x8249E9F0u, 64u, 0xCEF02F60u, "CreateVertexDeclaration"},  // only the XMV player calls it
  {0x8249E900u, 64u, 0x052ACC01u, "VertexDeclarationInit"},  // (elements, decl): the game's 823D2AF0 calls it
  {0x824A7970u, 64u, 0x98295182u, "D3DResource_Destroy"},
  // Command buffers (the game records on two type-2 devices and runs them on the title device). Not in MGR's map.
  {0x824A44D0u, 64u, 0x172F7422u, "BeginCommandBuffer"},
  {0x824A4A28u, 64u, 0x133B3080u, "EndCommandBuffer"},
  {0x824A4E48u, 64u, 0xA8BB2E96u, "RunCommandBuffer"},
  // VBlank handler of the GPU interrupt callback (0x824A8BA0); the glue ticks it, as Burst Limit's 820B5CF8.
  {0x8249AED8u, 64u, 0xD968AE4Cu, "VBlankHandler"},
};

// Probable equivalent (no public D3DDevice_SetRingBufferParameters; see HOOKMAP_RB2.md line 12). Guarded the same way,
// kept apart until a run confirms it.
inline constexpr GuestCodeGuard kGuestCodeGuardsProbable[] = {
  {0x824AA578u, 64u, 0x243B98DFu, "InitializeRingBuffer"},  // (CDevice*, D3DRING_BUFFER_PARAMETERS*): setup, teardown
};

// Not in this game: BeginVertices (no DrawVerticesUP either), EngineSwapProgress_reference (MGR's engine).
}  // namespace renut::gpu::rb2
