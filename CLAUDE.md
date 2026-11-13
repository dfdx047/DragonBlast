# Dragon Blast — instruções para o Claude Code

Port de **Dragon Ball: Raging Blast 2** (Xbox 360, título 4E4D0826, disco USA Rev 1) para Android por recompilação
estática (ReXGlue SDK). Fork de [moddedrounds/RB2-Android](https://github.com/moddedrounds/RB2-Android) (remoto
`upstream`), que por sua vez vem de [WistfulHopes/RB2](https://github.com/WistfulHopes/RB2). Branch de trabalho:
`dragonblast`. Método e documentação de GPU: `~/Optxbox` (roteiro em `docs/pt-BR/ROTEIRO.md`). Port irmão mais
próximo (mesmo SDK, Renut, launcher): `~/AndroidStudioProjects/Dragon-Burst`.

## Como falar com o desenvolvedor
- Responder em português do Brasil. Mensagens de commit em inglês, curtas.
- Fazer, medir, mostrar. Só parar para decisões que mudam o jogo ou a release.

## Aparelhos (adb)
| Aparelho | Serial | GPU | Uso |
|---|---|---|---|
| Poco X6 Pro | `4LPF6HMFEYPZCEJB` | Mali-G615 MC6 (Dimensity 8300) | aparelho de teste deste projeto (2026-11-13) |

- Sempre `adb -s <serial>`. Outros aparelhos só com pedido do desenvolvedor. O Mi A3 nunca.
- O Poco não tem cooler e esquenta: medições longas com cuidado.
- Pasta do app no aparelho: `/sdcard/Android/data/com.dfdx047.dragonblast/files` (jogo em `game/`, `logs/`, `cache/`).
- Arquivos de teste que pusermos no aparelho são apagados no fim do teste.

## Estrutura
- `android/` — projeto Gradle (AGP 8.13, Kotlin 2.2, Compose). Launcher em
  `app/src/main/java/com/dfdx047/dragonblast/` (Início, Gráficos, Controles, Drivers, Depuração, Sobre). Shell do jogo
  (SDL GameActivity, menu in-game) em `org/rb2recomp/android/` (pacote antigo mantido por causa dos nomes JNI).
- As configurações ficam em SharedPreferences `options`; as chaves do RB2-Android (`render_scale`, `fsr`, `fps_mode`,
  `show_fps`, `gpu_driver`, `custom_driver_*`) são lidas pelo `LaunchConfig.java` no arranque do jogo.
- Textos: `python3 tools/launcher/gen_strings.py android/app/src/main/res` gera `values/`, `values-pt/`, `values-es/` (EN, PT-BR, ES; segue o celular,
  outro idioma → inglês). Ícone: `python3 tools/launcher/icon_gen.py .` (fontes SVG em `docs/art/`).
- Tudo o que um teste gera fica em `build/` (ignorado): `build/shots`, `build/apk`, `build/tmp`.

## Build
- Só a launcher (sem o SDK ao lado do repositório o build entra sozinho nesse modo):
  `cd android && ./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
- Jogo completo: `tools/android/prepare_sdk.sh`, `build_host_cli.sh`, `codegen.sh <jogo extraído>`,
  `build_native.sh`, depois o Gradle (ver `ANDROID.md`). Precisa do `default.xex` do disco do desenvolvedor.
- Instalar no Poco: `adb -s 4LPF6HMFEYPZCEJB install -r <apk>` ("Instalar via USB" ligado no HyperOS).

## Licença
- BSD-3-Clause do RB2 (`LICENSE`). Arquivos do jogo e código recompilado nunca no git nem num APK distribuído.

## Estado
Ver `docs/ROADMAP.md`.
