# Generates values/, values-pt/, values-es/ strings.xml for the Dragon Blast launcher.
import pathlib, sys
S = {}
def s(k, en, pt, es): S[k] = (en, pt, es)

s("app_name", "Dragon Blast", "Dragon Blast", "Dragon Blast")
s("header_subtitle", "Raging Blast 2 · native Android port", "Raging Blast 2 · port nativo para Android", "Raging Blast 2 · port nativo para Android")
s("game_title", "Dragon Ball: Raging Blast 2", "Dragon Ball: Raging Blast 2", "Dragon Ball: Raging Blast 2")
s("tab_home", "Home", "Início", "Inicio")
s("tab_graphics", "Graphics", "Gráficos", "Gráficos")
s("tab_controls", "Controls", "Controles", "Controles")
s("tab_drivers", "Drivers", "Drivers", "Drivers")
s("tab_debug", "Debug", "Depuração", "Depuración")
s("tab_about", "About", "Sobre", "Acerca de")
s("tab_more", "More", "Mais", "Más")
s("more_drivers_sub", "GPU drivers: download, import and choose", "Drivers da GPU: baixar, importar e escolher", "Drivers de la GPU: descargar, importar y elegir")
s("more_debug_sub", "Logs, test options and maintenance", "Logs, opções de teste e manutenção", "Registros, opciones de prueba y mantenimiento")
s("more_about_sub", "Version, credits and links", "Versão, créditos e links", "Versión, créditos y enlaces")
s("back", "Back", "Voltar", "Volver")
s("play", "PLAY", "JOGAR", "JUGAR")
s("import_iso", "IMPORT ISO", "IMPORTAR ISO", "IMPORTAR ISO")
s("got_it", "Got it", "Entendi", "Entendido")
s("cancel", "Cancel", "Cancelar", "Cancelar")
s("save", "Save", "Salvar", "Guardar")
s("retry", "Try again", "Tentar de novo", "Reintentar")
s("loading", "Loading…", "Carregando…", "Cargando…")
s("off", "Off", "Desligado", "Apagado")
s("recommended", "recommended", "recomendado", "recomendado")
s("experimental", "experimental", "experimental", "experimental")
s("in_development", "in development", "em desenvolvimento", "en desarrollo")

# status
s("status_checking", "Checking…", "Verificando…", "Comprobando…")
s("status_importing", "Importing the ISO…", "Importando a ISO…", "Importando la ISO…")
s("status_ready", "Ready to play", "Pronto para jogar", "Listo para jugar")
s("status_incomplete", "Game files incomplete", "Arquivos do jogo incompletos", "Archivos del juego incompletos")
s("status_no_iso", "Waiting for the ISO", "Aguardando a ISO", "Esperando la ISO")

# game files
s("files_title", "Game files", "Arquivos do jogo", "Archivos del juego")
s("files_subtitle", "Xbox 360 · USA · Rev 1", "Xbox 360 · USA · Rev 1", "Xbox 360 · USA · Rev 1")
s("files_help", "Pick the .iso of your own Raging Blast 2 disc (Xbox 360, USA, Rev 1). The app extracts and checks it; you can delete the .iso afterwards. Nothing is uploaded.",
  "Escolha a .iso do seu próprio disco do Raging Blast 2 (Xbox 360, USA, Rev 1). O app extrai e confere tudo; depois você pode apagar a .iso. Nada é enviado para a internet.",
  "Elige la .iso de tu propio disco de Raging Blast 2 (Xbox 360, USA, Rev 1). La app la extrae y la comprueba; después puedes borrar la .iso. No se sube nada.")
s("files_installed", "%1$d files · %2$s", "%1$d arquivos · %2$s", "%1$d archivos · %2$s")
s("files_xex_bad", "default.xex has the wrong size: import the ISO again.", "O default.xex está com o tamanho errado: importe a ISO de novo.", "default.xex tiene un tamaño incorrecto: importa la ISO de nuevo.")
s("files_packs_bad", "Some data packs are missing: import the ISO again.", "Faltam pacotes de dados: importe a ISO de novo.", "Faltan paquetes de datos: importa la ISO de nuevo.")
s("files_import", "Import ISO", "Importar ISO", "Importar ISO")
s("files_reimport", "Import again", "Importar de novo", "Importar de nuevo")
s("files_delete", "Delete", "Apagar", "Borrar")
s("files_delete_title", "Delete the game files?", "Apagar os arquivos do jogo?", "¿Borrar los archivos del juego?")
s("files_delete_text", "The extracted game is removed (about 6 GB). Your saves and settings stay.", "O jogo extraído é removido (cerca de 6 GB). Seus saves e configurações continuam.", "Se elimina el juego extraído (unos 6 GB). Tus partidas guardadas y ajustes se quedan.")

# screen
s("screen_title", "Screen", "Tela", "Pantalla")
s("orientation", "Orientation", "Orientação", "Orientación")
s("orientation_short", "How the game screen is held.", "Como a tela do jogo fica.", "Cómo se sostiene la pantalla del juego.")
s("orientation_long", "Landscape: the phone lying down, the usual way to play (recommended).\n\nPortrait: the phone standing up; the picture sits at the top and the on-screen controller below it, handy for menus and playing with one hand.\n\nSensor: follows how you hold the phone and turns by itself.",
  "Paisagem: celular deitado, o jeito normal de jogar (recomendado).\n\nRetrato: celular em pé; a imagem fica em cima e o controle na tela embaixo, bom para menus e para jogar com uma mão.\n\nSensor: segue o jeito que você segura o celular e gira sozinho.",
  "Horizontal: el móvil acostado, la forma normal de jugar (recomendado).\n\nVertical: el móvil de pie; la imagen arriba y el mando en pantalla abajo, útil para menús y para jugar con una mano.\n\nSensor: sigue cómo sostienes el móvil y gira solo.")
s("orientation_landscape", "Landscape", "Paisagem", "Horizontal")
s("orientation_portrait", "Portrait", "Retrato", "Vertical")
s("orientation_sensor", "Sensor", "Sensor", "Sensor")

# community
s("community_title", "Community", "Comunidade", "Comunidad")
s("community_sub", "Videos, help and news", "Vídeos, ajuda e novidades", "Vídeos, ayuda y novedades")
s("community_kofi", "Ko-fi keeps these ports going. Thank you!", "O Ko-fi ajuda a manter esses ports. Obrigado!", "Ko-fi ayuda a mantener estos ports. ¡Gracias!")

# graphics
s("gfx_presets", "Quick presets", "Predefinições", "Ajustes rápidos")
s("gfx_presets_sub", "Resolution and upscaling in one tap", "Resolução e escala num toque", "Resolución y escalado en un toque")
s("preset_light", "Light", "Leve", "Ligero")
s("preset_balanced", "Balanced", "Equilibrado", "Equilibrado")
s("preset_quality", "Quality", "Qualidade", "Calidad")
s("gfx_render", "Rendering", "Renderização", "Renderizado")
s("gfx_renderer", "Renderer", "Renderizador", "Renderizador")
s("gfx_renderer_short", "Who draws the game.", "Quem desenha o jogo.", "Quién dibuja el juego.")
s("gfx_renderer_long", "Xenia: emulates the Xbox 360 GPU (EDRAM, resolves) over Vulkan. Works on every GPU, but costs more.\n\nRenut: a native renderer that draws the game's Direct3D straight in Vulkan, with no GPU emulation. Much lighter. It is being written for Raging Blast 2 on the same base as Dragon Burst and will be enabled when it is ready.",
  "Xenia: emula a GPU do Xbox 360 (EDRAM, resolves) sobre Vulkan. Funciona em todas as GPUs, mas pesa mais.\n\nRenut: renderizador nativo que desenha o Direct3D do jogo direto em Vulkan, sem emular a GPU. Bem mais leve. Está sendo escrito para o Raging Blast 2, na mesma base do Dragon Burst, e será liberado quando estiver pronto.",
  "Xenia: emula la GPU de Xbox 360 (EDRAM, resolves) sobre Vulkan. Funciona en todas las GPU, pero cuesta más.\n\nRenut: renderizador nativo que dibuja el Direct3D del juego directamente en Vulkan, sin emular la GPU. Mucho más ligero. Se está escribiendo para Raging Blast 2 sobre la misma base que Dragon Burst y se activará cuando esté listo.")
s("gfx_renderer_xenia_note", "GPU emulation", "GPU emulada", "GPU emulada")
s("gfx_resolution", "Resolution", "Resolução", "Resolución")
s("gfx_resolution_short", "Internal size the game draws at.", "Tamanho interno em que o jogo desenha.", "Tamaño interno al que dibuja el juego.")
s("gfx_resolution_long", "Raging Blast 2 draws at 1280x720. Lower values draw fewer pixels: less heat and steadier frames on weaker phones; the upscaler then stretches the picture to your screen.\n\nIf frames drop after a few minutes of play the phone is throttling: lower this one step.",
  "O Raging Blast 2 desenha em 1280x720. Valores menores desenham menos pixels: menos calor e quadros mais estáveis em celulares mais fracos; depois o escalonador estica a imagem para a sua tela.\n\nSe os quadros caírem depois de alguns minutos, o celular está esquentando e reduzindo a velocidade: baixe um degrau.",
  "Raging Blast 2 dibuja a 1280x720. Valores más bajos dibujan menos píxeles: menos calor y fotogramas más estables en móviles más débiles; luego el escalador estira la imagen a tu pantalla.\n\nSi los fotogramas bajan tras unos minutos, el móvil se está calentando y bajando la velocidad: baja un paso.")
s("gfx_aspect", "Aspect ratio", "Proporção", "Proporción")
s("gfx_aspect_short", "How the 16:9 picture fills a wider screen.", "Como a imagem 16:9 ocupa uma tela mais larga.", "Cómo la imagen 16:9 llena una pantalla más ancha.")
s("gfx_aspect_long", "Raging Blast 2 draws a 16:9 picture. Phones are wider (20:9 and up).\n\n16:9: the game's own shape, with black bars at the sides.\n\nStretch: the same picture stretched over the whole screen, a little wider than drawn.\n\nChanges at once, also from the in-game menu.", "O Raging Blast 2 desenha uma imagem 16:9. Os celulares são mais largos (20:9 ou mais).\n\n16:9: o formato do próprio jogo, com barras pretas nas laterais.\n\nEsticar: a mesma imagem esticada na tela inteira, um pouco mais larga que o desenho.\n\nMuda na hora, também pelo menu do jogo.", "Raging Blast 2 dibuja una imagen 16:9. Los móviles son más anchos (20:9 o más).\n\n16:9: la forma propia del juego, con barras negras a los lados.\n\nEstirar: la misma imagen estirada a toda la pantalla, algo más ancha que el dibujo.\n\nCambia al momento, también desde el menú del juego.")
s("aspect_original", "16:9", "16:9", "16:9")
s("aspect_original_note", "side bars", "barras laterais", "barras laterales")
s("aspect_stretch", "Stretch", "Esticar", "Estirar")
s("aspect_stretch_note", "full screen", "tela cheia", "pantalla completa")
s("menu_paused", "PAUSED", "PAUSADO", "EN PAUSA")
s("menu_tab_game", "Game", "Jogo", "Juego")
s("menu_tab_picture", "Picture", "Imagem", "Imagen")
s("menu_tab_controls", "Controls", "Controles", "Controles")
s("menu_tab_system", "System", "Sistema", "Sistema")
s("menu_tab_debug", "Debug", "Depuração", "Depuración")
s("menu_resume", "Resume", "Continuar", "Continuar")
s("menu_resume_help", "Back to the game.", "Volta para o jogo.", "Vuelve al juego.")
s("menu_restart", "Restart the game", "Reiniciar o jogo", "Reiniciar el juego")
s("menu_restart_help", "Closes and opens the game again (applies the options marked to reopen). Progress since the last save is lost.", "Fecha e abre o jogo de novo (aplica as opções marcadas para reabrir). O progresso desde o último save se perde.", "Cierra y abre el juego de nuevo (aplica las opciones marcadas para reabrir). Se pierde el progreso desde el último guardado.")
s("menu_exit", "Exit to the launcher", "Sair para a launcher", "Salir al launcher")
s("menu_exit_help", "Closes the game. Progress since the last save is lost.", "Fecha o jogo. O progresso desde o último save se perde.", "Cierra el juego. Se pierde el progreso desde el último guardado.")
s("menu_go", "Go", "Ir", "Ir")
s("menu_open", "Open", "Abrir", "Abrir")
s("menu_reopen", "applies when the game reopens", "funciona ao reabrir o jogo", "se aplica al reabrir el juego")
s("menu_reopen_hint", "Takes effect the next time the game opens (Game › Restart).", "Vale na próxima vez que o jogo abrir (Jogo › Reiniciar).", "Vale la próxima vez que se abra el juego (Juego › Reiniciar).")
s("menu_hints_pad", "◀ ▶ change · Ⓐ choose · Ⓑ close · LB / RB tabs", "◀ ▶ muda · Ⓐ escolhe · Ⓑ fecha · LB / RB abas", "◀ ▶ cambia · Ⓐ elige · Ⓑ cierra · LB / RB pestañas")
s("menu_layout_now", "Layout on screen", "Layout na tela", "Diseño en pantalla")
s("menu_layout_now_help", "Fight or menus. Also the ⇄ button at the top of the screen.", "Luta ou menus. Também o botão ⇄ no topo da tela.", "Combate o menús. También el botón ⇄ arriba en la pantalla.")
s("menu_layout_fight", "Fight", "Luta", "Combate")
s("menu_layout_menus", "Menus", "Menus", "Menús")
s("menu_edit_help", "Move and resize the buttons of the current scheme; the game stays paused.", "Mover e redimensionar os botões do esquema atual; o jogo fica pausado.", "Mover y cambiar el tamaño de los botones del esquema actual; el juego sigue en pausa.")
s("menu_export_help", "Saves the logs in a .zip to share (Discord, e-mail).", "Junta os logs num .zip para compartilhar (Discord, e-mail).", "Junta los registros en un .zip para compartir (Discord, correo).")
s("menu_device_info", "This session", "Esta sessão", "Esta sesión")
s("menu_info_renderer", "Renderer: %1$s", "Renderizador: %1$s", "Renderizador: %1$s")
s("menu_info_gpu", "GPU: %1$s", "GPU: %1$s", "GPU: %1$s")
s("menu_info_driver", "Driver: %1$s", "Driver: %1$s", "Driver: %1$s")
s("menu_info_render", "Drawn at %1$s, shown on %2$s", "Desenhado em %1$s, mostrado em %2$s", "Dibujado a %1$s, mostrado en %2$s")
s("gfx_upscaler", "Upscaler", "Escalonador", "Escalador")
s("gfx_upscaler_short", "How the picture is stretched to the screen.", "Como a imagem é esticada até a tela.", "Cómo se estira la imagen a la pantalla.")
s("gfx_upscaler_long", "Bilinear: plain and soft, the lightest.\n\nFSR 1 (AMD FidelityFX Super Resolution): sharper edges when the resolution is below 100 %, for a tiny GPU cost. Recommended.",
  "Bilinear: simples e suave, o mais leve.\n\nFSR 1 (AMD FidelityFX Super Resolution): bordas mais nítidas quando a resolução está abaixo de 100 %, com um custo mínimo de GPU. Recomendado.",
  "Bilineal: simple y suave, el más ligero.\n\nFSR 1 (AMD FidelityFX Super Resolution): bordes más nítidos cuando la resolución está por debajo del 100 %, con un coste mínimo de GPU. Recomendado.")
s("gfx_upscaler_bilinear", "Bilinear", "Bilinear", "Bilineal")
s("gfx_frames", "Frame rate", "Taxa de quadros", "Fotogramas")
s("gfx_fps", "Frames per second", "Quadros por segundo", "Fotogramas por segundo")
s("gfx_fps_short", "60 is the game's own speed.", "60 é a velocidade do próprio jogo.", "60 es la velocidad del propio juego.")
s("gfx_fps_long", "Raging Blast 2 runs its game logic once per frame, so it always plays at 60: capping it at 30 would make the whole game run at half speed.\n\n120 FPS: frame generation adds one interpolated picture between two real ones, for 120 Hz screens. Smoother motion, about one frame more of input delay and more battery.",
  "O Raging Blast 2 roda a lógica do jogo uma vez por quadro, então ele sempre joga a 60: limitar em 30 deixaria o jogo inteiro em câmera lenta.\n\n120 FPS: a geração de quadros põe uma imagem interpolada entre duas reais, para telas de 120 Hz. Movimento mais fluido, cerca de um quadro a mais de atraso nos comandos e mais bateria.",
  "Raging Blast 2 ejecuta la lógica del juego una vez por fotograma, así que siempre va a 60: limitarlo a 30 haría que todo el juego fuera a media velocidad.\n\n120 FPS: la generación de fotogramas añade una imagen interpolada entre dos reales, para pantallas de 120 Hz. Movimiento más fluido, un fotograma más de retraso y más batería.")
s("gfx_fps_fg_note", "frame generation", "geração de quadros", "generación de fotogramas")
s("gfx_show_fps", "Show FPS", "Mostrar FPS", "Mostrar FPS")
s("gfx_show_fps_short", "Frame counter in the corner.", "Contador de quadros no canto.", "Contador de fotogramas en la esquina.")
s("gfx_show_fps_long", "Shows the frames per second and the frame time in the top corner while you play. Useful to compare settings and drivers.",
  "Mostra os quadros por segundo e o tempo de cada quadro no canto de cima enquanto você joga. Útil para comparar configurações e drivers.",
  "Muestra los fotogramas por segundo y el tiempo de cada fotograma en la esquina superior mientras juegas. Útil para comparar ajustes y drivers.")
s("gfx_system", "System", "Sistema", "Sistema")
s("gfx_pause_bg", "Pause in the background", "Pausar em segundo plano", "Pausar en segundo plano")
s("gfx_pause_bg_short", "Stops the game when you leave it.", "Para o jogo quando você sai dele.", "Detiene el juego cuando sales.")
s("gfx_pause_bg_long", "When you switch to another app or turn the screen off, the game stops drawing and waits: no battery or heat spent on a picture nobody sees. It continues where it was when you come back.",
  "Quando você troca de app ou desliga a tela, o jogo para de desenhar e espera: nada de bateria ou calor gastos com uma imagem que ninguém vê. Ele continua de onde parou quando você volta.",
  "Cuando cambias de app o apagas la pantalla, el juego deja de dibujar y espera: sin gastar batería ni calor en una imagen que nadie ve. Sigue donde estaba al volver.")
s("gfx_stereo", "Stereo sound", "Som estéreo", "Sonido estéreo")
s("gfx_stereo_short", "Keeps music and voices on phone speakers.", "Mantém músicas e vozes nos alto-falantes do celular.", "Mantiene música y voces en los altavoces del móvil.")
s("gfx_stereo_long", "The game mixes its music and voices on the 5.1 centre channel. Many phones drop that channel and play only effects. With this on the app folds 5.1 to stereo itself, so nothing is lost. Turn it off only on a real surround setup.",
  "O jogo mixa as músicas e as vozes no canal central do 5.1. Muitos celulares descartam esse canal e tocam só os efeitos. Ligado, o app converte o 5.1 em estéreo por conta própria e nada se perde. Desligue só num sistema surround de verdade.",
  "El juego mezcla la música y las voces en el canal central 5.1. Muchos móviles descartan ese canal y solo suenan los efectos. Activado, la app convierte el 5.1 a estéreo y no se pierde nada. Desactívalo solo con un equipo surround real.")

# controls
s("ctl_touch", "On-screen controller", "Controle na tela", "Mando en pantalla")
s("ctl_touch_mode", "Show", "Mostrar", "Mostrar")
s("ctl_touch_mode_short", "When the touch controls appear.", "Quando os controles de toque aparecem.", "Cuándo aparecen los controles táctiles.")
s("ctl_touch_mode_long", "Automatic: shown while you play by touch, hidden as soon as a controller is used, and back when you touch the screen again.\n\nAlways: always on screen.\n\nOff: controller only.",
  "Automático: aparece enquanto você joga pelo toque, some assim que um controle é usado e volta quando você toca na tela de novo.\n\nSempre: sempre na tela.\n\nDesligado: só controle físico.",
  "Automático: aparece mientras juegas con la pantalla, se oculta en cuanto usas un mando y vuelve al tocar la pantalla.\n\nSiempre: siempre en pantalla.\n\nApagado: solo mando físico.")
s("ctl_touch_auto", "Automatic", "Automático", "Automático")
s("ctl_touch_always", "Always", "Sempre", "Siempre")
s("ctl_preset", "Layout", "Layout", "Diseño")
s("ctl_preset_short", "Ready-made controllers for each way of playing.", "Controles prontos para cada jeito de jogar.", "Mandos listos para cada forma de jugar.")
s("ctl_preset_long", "Pro: made for competitive play. Dynamic stick that appears under your thumb anywhere on the left side, bigger touch areas on the buttons and sliding between buttons to chain attacks.\n\nNormal: the balanced layout: dynamic stick, buttons in the usual place.\n\nCasual: large fixed stick and buttons; charging and guarding stay held with one tap.\n\nAfter choosing, fine-tune size, opacity and the rest below.",
  "Pro: feito para o competitivo. Analógico dinâmico que aparece embaixo do polegar em qualquer ponto do lado esquerdo, botões com área de toque maior e deslizar entre botões para emendar ataques.\n\nNormal: o layout equilibrado: analógico dinâmico, botões no lugar de sempre.\n\nCasual: analógico fixo e botões grandes; carregar ki e defender ficam segurados com um toque.\n\nDepois de escolher, ajuste tamanho, opacidade e o resto logo abaixo.",
  "Pro: pensado para el competitivo. Stick dinámico que aparece bajo el pulgar en cualquier punto del lado izquierdo, botones con más área táctil y deslizar entre botones para encadenar ataques.\n\nNormal: el diseño equilibrado: stick dinámico, botones en su sitio de siempre.\n\nCasual: stick fijo y botones grandes; cargar ki y defender se mantienen con un toque.\n\nTras elegir, ajusta tamaño, opacidad y lo demás abajo.")
s("ctl_preset_pro_note", "competitive", "competitivo", "competitivo")
s("ctl_preset_casual_note", "relaxed", "tranquilo", "relajado")
s("ctl_preset_pro_desc", "Dynamic stick that appears under your thumb on the left half, 28% larger touch areas, and sliding between buttons with a guard against accidental slides.", "Analógico dinâmico que nasce onde o polegar toca na metade esquerda, áreas de toque 28% maiores e deslizar entre botões com trava contra deslizes acidentais.", "Stick dinámico que aparece bajo el pulgar en la mitad izquierda, áreas táctiles un 28% mayores y deslizar entre botones con freno contra deslices accidentales.")
s("ctl_preset_normal_desc", "Dynamic stick, slightly enlarged touch areas, one button per touch.", "Analógico dinâmico, áreas de toque um pouco maiores, um botão por toque.", "Stick dinámico, áreas táctiles algo mayores, un botón por toque.")
s("ctl_preset_casual_desc", "Fixed stick that stays in place, larger touch areas, no sliding between buttons.", "Analógico fixo que não sai do lugar, áreas de toque maiores, sem deslizar entre botões.", "Stick fijo que no se mueve, áreas táctiles mayores, sin deslizar entre botones.")
s("ctl_style", "Button style", "Estilo dos botões", "Estilo de botones")
s("ctl_style_short", "Symbols on the touch buttons.", "Símbolos nos botões de toque.", "Símbolos en los botones táctiles.")
s("ctl_style_long", "Dragon: action icons (punch, ki, guard, dash, Super Attacks...) in the port's own look, with a small badge naming the pad button.\n\nXbox: A B X Y in their colours, as the game shows them.\n\nPlayStation: cross, circle, square and triangle.\n\nNintendo: B A Y X in Nintendo's positions.\n\nOnly the look changes: each position always does the same action.",
  "Dragon: ícones de ação (soco, ki, guarda, dash, Super Attacks...) no visual do próprio port, com um selo pequeno dizendo o botão do controle.\n\nXbox: A B X Y nas cores, como o jogo mostra.\n\nPlayStation: xis, círculo, quadrado e triângulo.\n\nNintendo: B A Y X nas posições da Nintendo.\n\nSó muda o visual: cada posição faz sempre a mesma ação.",
  "Dragon: iconos de acción (golpe, ki, guardia, dash, Super Attacks...) con el aspecto propio del port y una pequeña insignia con el botón del mando.\n\nXbox: A B X Y en sus colores, como los muestra el juego.\n\nPlayStation: equis, círculo, cuadrado y triángulo.\n\nNintendo: B A Y X en las posiciones de Nintendo.\n\nSolo cambia el aspecto: cada posición hace siempre la misma acción.")
s("ctl_scheme_title", "Fight controls", "Comandos da luta", "Controles de combate")
s("ctl_scheme_sub", "Menus always keep the game's controls", "Os menus continuam com os comandos do jogo", "Los menús siempre usan los controles del juego")
s("ctl_scheme", "Scheme", "Esquema", "Esquema")
s("ctl_scheme_short", "How the fight buttons are laid out on the screen.", "Como os botões da luta ficam na tela.", "Cómo quedan los botones de combate en la pantalla.")
s("ctl_scheme_long", "Raging Blast 2: the game's own controls, button by button. The four Super Attacks are on one disc: drag towards a direction to pick one, or just tap it for the first.\n\nModern: one tap per action, like newer Dragon Ball games. Sway, Vanishing, Counter and Throw (normally Guard + a button) get their own buttons, and so do the Ultimate, ki charge, ascend and descend.\n\nTenkaichi 3: fights play like Budokai Tenkaichi 3, on the screen and on a physical controller: □ melee, △ ki blast, ○ guard, ✕ dash, R1 / R2 up / down, L2 held charges ki, L2+○ Blast 1, L2+△ Blast 2 (with ↑ → ← the other Super Attacks), L2+↓△ Ultimate, R3 transform. The game reads them as its own moves; menus and the pause menu keep the usual buttons.\n\nEach scheme keeps its own layout in the editor. Menus always use the menu layout.", "Raging Blast 2: os comandos do próprio jogo, botão por botão. Os quatro Super Attacks ficam num disco só: arraste para uma direção para escolher, ou só toque para o primeiro.\n\nModerno: um toque por ação, como nos Dragon Ball mais novos. Sway, Vanishing, Counter e Agarrar (que no controle são Guarda + um botão) ganham botões próprios, assim como o Ultimate, a carga de ki, subir e descer.\n\nTenkaichi 3: a luta se joga como no Budokai Tenkaichi 3, na tela e no controle físico: □ soco, △ rajada de ki, ○ guarda, ✕ dash, R1 / R2 sobe / desce, segurar L2 carrega o ki, L2+○ Blast 1, L2+△ Blast 2 (com ↑ → ← os outros Super Attacks), L2+↓△ Ultimate, R3 transforma. O jogo entende como os golpes dele; os menus e a pausa continuam com os botões normais.\n\nCada esquema guarda o seu próprio layout no editor. Os menus usam sempre o layout de menu.", "Raging Blast 2: los controles del propio juego, botón por botón. Los cuatro Super Attacks están en un solo disco: arrastra hacia una dirección para elegir, o solo tócalo para el primero.\n\nModerno: un toque por acción, como en los Dragon Ball más nuevos. Sway, Vanishing, Counter y Agarre (en el mando, Guardia + un botón) tienen botones propios, igual que el Ultimate, la carga de ki, subir y bajar.\n\nTenkaichi 3: el combate se juega como en Budokai Tenkaichi 3, en la pantalla y en un mando físico: □ golpe, △ ráfaga de ki, ○ guardia, ✕ dash, R1 / R2 sube / baja, mantener L2 carga el ki, L2+○ Blast 1, L2+△ Blast 2 (con ↑ → ← los otros Super Attacks), L2+↓△ Ultimate, R3 transforma. El juego lo entiende como sus propios movimientos; los menús y la pausa mantienen los botones normales.\n\nCada esquema guarda su propio diseño en el editor. Los menús siempre usan el diseño de menú.")
s("ctl_scheme_original", "original", "original", "original")
s("ctl_scheme_modern", "Modern", "Moderno", "Moderno")
s("ctl_scheme_modern_note", "one tap", "um toque", "un toque")
s("ctl_scheme_bt3_note", "BT3 controls", "comandos do BT3", "controles de BT3")
s("ctl_edit", "Edit layout", "Editar layout", "Editar diseño")
s("ctl_edit_menu", "Menu layout", "Layout de menu", "Diseño de menú")
s("editor_hint", "Drag the buttons · tap one to edit it", "Arraste os botões · toque num para editar", "Arrastra los botones · toca uno para editarlo")
s("editor_title", "Touch layout", "Layout dos controles", "Diseño táctil")
s("editor_layout_fight", "Fight", "Luta", "Combate")
s("editor_layout_menu", "Menus", "Menus", "Menús")
s("editor_add", "Add", "Adicionar", "Añadir")
s("reset", "Reset", "Restaurar", "Restablecer")
s("delete", "Delete", "Apagar", "Eliminar")
s("editor_helpers", "Move buttons", "Botões de golpe", "Botones de movimiento")
s("editor_macro", "Macro", "Macro", "Macro")
s("editor_show", "Show %1$s", "Mostrar %1$s", "Mostrar %1$s")
s("editor_pad_buttons", "Controller buttons", "Botões do controle", "Botones del mando")
s("editor_new_macro", "New macro", "Nova macro", "Nueva macro")
s("editor_hide", "Hide", "Esconder", "Ocultar")
s("editor_size", "Size", "Tamanho", "Tamaño")
s("editor_opacity", "Opacity", "Opacidade", "Opacidad")
s("editor_label", "Label", "Rótulo", "Etiqueta")
s("editor_combo", "Combination", "Combinação", "Combinación")
s("editor_sequence", "Sequence", "Sequência", "Secuencia")
s("editor_combo_help", "All selected buttons are pressed together while you hold.", "Todos os botões marcados são apertados juntos enquanto você segura.", "Todos los botones marcados se pulsan a la vez mientras mantienes.")
s("editor_sequence_help", "Steps run in order, 60 frames = 1 second. Forward / Back = towards / away from the opponent.", "Os passos correm em ordem, 60 frames = 1 segundo. Frente / Trás = na direção do adversário / para longe dele.", "Los pasos van en orden, 60 frames = 1 segundo. Adelante / Atrás = hacia el rival / lejos de él.")
s("editor_step", "Step %1$d", "Passo %1$d", "Paso %1$d")
s("editor_frames", "Frames", "Frames", "Frames")
s("editor_add_step", "Step", "Passo", "Paso")
s("editor_hold_last", "Hold last step", "Segurar último passo", "Mantener último paso")
s("editor_macro_note", "Macros only send controller buttons: the game sees them as a normal controller.", "As macros só mandam botões do controle: o jogo vê como um controle normal.", "Las macros solo envían botones del mando: el juego lo ve como un mando normal.")
s("input_melee", "Melee", "Soco", "Golpe")
s("input_ki", "Ki blast", "Rajada de ki", "Ráfaga de ki")
s("input_dash", "Dash", "Dash", "Dash")
s("input_signature", "Signature", "Técnica", "Técnica")
s("input_guard", "Guard", "Guarda", "Guardia")
s("input_forward", "Forward", "Frente", "Adelante")
s("input_back", "Back", "Trás", "Atrás")
s("helper_sway", "Sway (Guard + Melee)", "Sway (Guarda + Soco)", "Sway (Guardia + Golpe)")
s("helper_vanishing", "Vanishing (Guard + Dash)", "Vanishing (Guarda + Dash)", "Vanishing (Guardia + Dash)")
s("helper_counter", "Counter (Guard + Ki)", "Counter (Guarda + Ki)", "Counter (Guardia + Ki)")
s("helper_throw", "Throw (Guard + Signature)", "Agarrar (Guarda + Técnica)", "Agarre (Guardia + Técnica)")
s("helper_ultimate", "Ultimate Attack (R3)", "Ultimate Attack (R3)", "Ultimate Attack (R3)")
s("helper_charge", "Ki charge (hold)", "Carregar ki (segurar)", "Cargar ki (mantener)")
s("helper_ascend", "Ascend", "Subir", "Subir")
s("helper_descend", "Descend", "Descer", "Bajar")
s("helper_rising", "Super Rising", "Super Rising", "Super Rising")
s("helper_diamond", "Switch / Transform (RT)", "Trocar / Transformar (RT)", "Cambiar / Transformar (RT)")
s("helper_super1", "Super Attack 1", "Super Attack 1", "Super Attack 1")
s("helper_super2", "Super Attack 2", "Super Attack 2", "Super Attack 2")
s("helper_super3", "Super Attack 3", "Super Attack 3", "Super Attack 3")
s("helper_super4", "Super Attack 4", "Super Attack 4", "Super Attack 4")
s("ctl_fine", "Fine tuning", "Ajustes finos", "Ajustes finos")
s("ctl_opacity", "Opacity", "Opacidade", "Opacidad")
s("ctl_opacity_short", "How visible the controls are.", "Quanto os controles aparecem.", "Cuánto se ven los controles.")
s("ctl_size", "Size", "Tamanho", "Tamaño")
s("ctl_size_short", "Scales every control together.", "Muda o tamanho de todos os controles juntos.", "Escala todos los controles a la vez.")
s("ctl_stick", "Stick", "Analógico", "Stick")
s("ctl_stick_short", "Where the stick appears.", "Onde o analógico aparece.", "Dónde aparece el stick.")
s("ctl_stick_long", "Fixed: always in the same place; you aim for it.\n\nDynamic: appears wherever your thumb lands on the left side and follows it if you slide past the edge. Fewer missed touches in fast fights.",
  "Fixo: sempre no mesmo lugar; você mira nele.\n\nDinâmico: aparece onde o polegar encostar no lado esquerdo e acompanha se você passar da borda. Menos toques perdidos em lutas rápidas.",
  "Fijo: siempre en el mismo sitio; apuntas a él.\n\nDinámico: aparece donde apoyes el pulgar en el lado izquierdo y lo sigue si te pasas del borde. Menos toques fallidos en combates rápidos.")
s("ctl_stick_fixed", "Fixed", "Fixo", "Fijo")
s("ctl_stick_dynamic", "Dynamic", "Dinâmico", "Dinámico")
s("ctl_deadzone", "Dead zone", "Zona morta", "Zona muerta")
s("ctl_deadzone_short", "Movement ignored around the centre.", "Movimento ignorado perto do centro.", "Movimiento ignorado cerca del centro.")
s("ctl_deadzone_long", "Small thumb movements near the centre are ignored, so resting the thumb does not move the fighter. Raise it if your character drifts; lower it for finer control.",
  "Pequenos movimentos do polegar perto do centro são ignorados, para o lutador não andar quando você só apoia o dedo. Aumente se o personagem andar sozinho; diminua para um controle mais fino.",
  "Los pequeños movimientos cerca del centro se ignoran, para que el luchador no se mueva al apoyar el pulgar. Súbela si el personaje se mueve solo; bájala para un control más fino.")
s("ctl_swipe", "Slide between buttons", "Deslizar entre botões", "Deslizar entre botones")
s("ctl_swipe_short", "Chain attacks without lifting the thumb.", "Emende ataques sem tirar o dedo.", "Encadena ataques sin levantar el dedo.")
s("ctl_swipe_long", "Slide your thumb from one face button to a neighbour and the second one is pressed without lifting. To avoid accidents the thumb has to go well inside the next button (past a third of its radius) and stay there for a moment; between buttons there is a neutral band where nothing is pressed, and only neighbouring buttons chain, never across the diamond.",
  "Deslize o polegar de um botão de ação até o vizinho e o segundo é apertado sem tirar o dedo. Para evitar acidentes o dedo precisa entrar bem no próximo botão (passar de um terço do raio) e ficar ali um instante; entre os botões há uma faixa neutra em que nada é apertado, e só botões vizinhos se encadeiam, nunca atravessando o losango.",
  "Desliza el pulgar de un botón de acción al vecino y el segundo se pulsa sin levantar el dedo. Para evitar accidentes el dedo tiene que entrar bien en el siguiente botón (pasar un tercio de su radio) y quedarse un instante; entre botones hay una franja neutra donde no se pulsa nada, y solo se encadenan botones vecinos, nunca cruzando el rombo.")
s("ctl_left", "Left-handed", "Canhoto", "Zurdo")
s("ctl_left_short", "Mirrors the layout: stick on the right.", "Espelha o layout: analógico à direita.", "Refleja el diseño: stick a la derecha.")
s("ctl_haptics", "Vibration", "Vibração", "Vibración")
s("ctl_haptics_short", "A short buzz on each press.", "Uma vibração curta a cada toque.", "Una vibración corta en cada pulsación.")
s("ctl_haptics_light", "Light", "Leve", "Suave")
s("ctl_haptics_strong", "Strong", "Forte", "Fuerte")
s("ctl_pad", "Physical controller", "Controle físico", "Mando físico")
s("ctl_pad_sub", "Bluetooth and USB controllers", "Controles Bluetooth e USB", "Mandos Bluetooth y USB")
s("ctl_nintendo", "Nintendo layout", "Layout Nintendo", "Diseño Nintendo")
s("ctl_nintendo_short", "Swaps A/B and X/Y.", "Troca A/B e X/Y.", "Intercambia A/B y X/Y.")
s("ctl_nintendo_long", "On Nintendo-style controllers (Switch Pro, 8BitDo in Switch mode, many handhelds) A and B, X and Y are swapped compared to Xbox. Turn this on so the button in each position does what the game expects there.",
  "Em controles no estilo Nintendo (Switch Pro, 8BitDo no modo Switch, vários portáteis) o A e o B, o X e o Y ficam trocados em relação ao Xbox. Ligue para que o botão em cada posição faça o que o jogo espera ali.",
  "En mandos estilo Nintendo (Switch Pro, 8BitDo en modo Switch, muchas portátiles) A y B, X e Y están cambiados respecto a Xbox. Actívalo para que el botón de cada posición haga lo que el juego espera ahí.")

# drivers
s("drv_gpu", "Your GPU", "Sua GPU", "Tu GPU")
s("drv_gpu_name", "GPU", "GPU", "GPU")
s("drv_soc", "Chip", "Chip", "Chip")
s("drv_system_version", "System driver", "Driver do sistema", "Driver del sistema")
s("drv_last_used", "Last game used", "Último jogo usou", "Última partida usó")
s("drv_never", "not played yet", "ainda não jogou", "aún no se ha jugado")
s("drv_active", "Driver in use", "Driver em uso", "Driver en uso")
s("drv_active_sub", "What the game loads next time", "O que o jogo carrega na próxima vez", "Lo que el juego carga la próxima vez")
s("drv_active_long", "Automatic: on Adreno the bundled Turnip when this build has it, otherwise the phone's own driver; on Mali and other GPUs the phone's own driver.\n\nA downloaded driver only replaces Vulkan for this game. If it cannot find your GPU, the game falls back to the phone's own driver by itself and this tab shows what really ran.\n\nNew driver, odd picture or crash? Go back to Automatic.",
  "Automático: na Adreno usa o Turnip incluído quando esta build tem um, senão o driver do próprio celular; na Mali e outras GPUs, o driver do celular.\n\nUm driver baixado só substitui o Vulkan deste jogo. Se ele não encontrar a sua GPU, o jogo volta sozinho para o driver do celular e esta aba mostra o que realmente rodou.\n\nDriver novo com imagem estranha ou travando? Volte para Automático.",
  "Automático: en Adreno, el Turnip incluido si esta build lo tiene; si no, el driver del propio móvil; en Mali y otras GPU, el driver del móvil.\n\nUn driver descargado solo sustituye el Vulkan de este juego. Si no encuentra tu GPU, el juego vuelve solo al driver del móvil y esta pestaña muestra lo que se usó realmente.\n\n¿Driver nuevo con imagen rara o cierres? Vuelve a Automático.")
s("drv_auto", "Automatic", "Automático", "Automático")
s("drv_auto_turnip", "Bundled Turnip on this GPU", "Turnip incluído nesta GPU", "Turnip incluido en esta GPU")
s("drv_auto_system", "The phone's own driver on this GPU", "O driver do próprio celular nesta GPU", "El driver del propio móvil en esta GPU")
s("drv_system", "Phone's own driver", "Driver do celular", "Driver del móvil")
s("drv_system_sub", "The Vulkan driver that came with the phone", "O driver Vulkan que veio no celular", "El driver Vulkan que trae el móvil")
s("drv_import", "Import driver (.zip)", "Importar driver (.zip)", "Importar driver (.zip)")
s("drv_wrong_gpu", "Not for this GPU", "Não serve para esta GPU", "No es para esta GPU")
s("drv_download", "Download drivers", "Baixar drivers", "Descargar drivers")
s("drv_download_sub", "Builds from the people who make them, on GitHub", "Builds de quem faz os drivers, no GitHub", "Builds de quienes hacen los drivers, en GitHub")
s("drv_your_gpu", "your GPU", "sua GPU", "tu GPU")
s("drv_other_vendor", "These drivers are not for your GPU (%1$s): you can browse them, not install them.", "Estes drivers não são para a sua GPU (%1$s): dá para ver a lista, não para instalar.", "Estos drivers no son para tu GPU (%1$s): puedes ver la lista, no instalarlos.")
s("drv_mali_note", "Mali drivers (PanVK, MaliSX2) are experimental. If the picture breaks or the game does not open, go back to Automatic.", "Os drivers para Mali (PanVK, MaliSX2) são experimentais. Se a imagem quebrar ou o jogo não abrir, volte para Automático.", "Los drivers para Mali (PanVK, MaliSX2) son experimentales. Si la imagen falla o el juego no abre, vuelve a Automático.")
s("drv_versions", "%1$d versions", "%1$d versões", "%1$d versiones")
s("drv_list_failed", "Could not load the list (%1$s).", "Não deu para carregar a lista (%1$s).", "No se pudo cargar la lista (%1$s).")
s("drv_none_for_gpu", "No package of this source fits your GPU.", "Nenhum pacote desta fonte serve para a sua GPU.", "Ningún paquete de esta fuente sirve para tu GPU.")
s("drv_source_not_for_gpu", "This source does not build for your GPU model.", "Esta fonte não faz builds para o seu modelo de GPU.", "Esta fuente no compila para tu modelo de GPU.")
s("drv_err_network", "Download failed: check the connection.", "O download falhou: confira a conexão.", "La descarga falló: revisa la conexión.")
s("drv_err_zip", "That file is not a driver package (.zip).", "Esse arquivo não é um pacote de driver (.zip).", "Ese archivo no es un paquete de driver (.zip).")
s("drv_err_lib", "No Vulkan library (.so) inside the package.", "Não há biblioteca Vulkan (.so) dentro do pacote.", "No hay biblioteca Vulkan (.so) en el paquete.")
s("drv_err_api", "This driver needs Android API %1$s or newer.", "Este driver precisa do Android API %1$s ou mais novo.", "Este driver necesita Android API %1$s o superior.")
s("drv_err_io", "Could not install the driver.", "Não deu para instalar o driver.", "No se pudo instalar el driver.")
s("src_purple", "Turnip tuned for emulation (Adreno 6xx/7xx)", "Turnip ajustado para emulação (Adreno 6xx/7xx)", "Turnip ajustado para emulación (Adreno 6xx/7xx)")
s("src_steven", "Turnip from Mesa main, frequent builds", "Turnip do Mesa main, builds frequentes", "Turnip de Mesa main, builds frecuentes")
s("src_k11mch1", "Turnip and Qualcomm driver packages", "Turnip e pacotes do driver Qualcomm", "Turnip y paquetes del driver Qualcomm")
s("src_whitebelyash", "Mainline Turnip (made for Adreno 8xx, may work on 6xx/7xx)", "Turnip mainline (feito para Adreno 8xx, pode funcionar nas 6xx/7xx)", "Turnip mainline (hecho para Adreno 8xx, puede funcionar en 6xx/7xx)")
s("src_faux123", "Turnip with speed and stability fixes (6xx/7xx/8xx)", "Turnip com correções de velocidade e estabilidade (6xx/7xx/8xx)", "Turnip con mejoras de velocidad y estabilidad (6xx/7xx/8xx)")
s("src_armsx2_turnip", "ARMSX2's Turnip (Adreno 6xx/7xx)", "Turnip do ARMSX2 (Adreno 6xx/7xx)", "Turnip de ARMSX2 (Adreno 6xx/7xx)")
s("src_zenithblue", "PanVK (Mesa) for Bifrost, Valhall and 5th gen Mali", "PanVK (Mesa) para Mali Bifrost, Valhall e 5ª geração", "PanVK (Mesa) para Mali Bifrost, Valhall y 5ª generación")
s("src_jimvulkan", "PanVK (Mesa) port for Mali GPUs", "Port do PanVK (Mesa) para GPUs Mali", "Port de PanVK (Mesa) para GPU Mali")
s("src_malisx2", "Vulkan driver for Mali G52/G57/G68/G77/G78/G615/G715", "Driver Vulkan para Mali G52/G57/G68/G77/G78/G615/G715", "Driver Vulkan para Mali G52/G57/G68/G77/G78/G615/G715")
s("src_fristonerr", "PanVK tuned for Bifrost and Valhall (G57, G68, G77, G78)", "PanVK ajustado para Bifrost e Valhall (G57, G68, G77, G78)", "PanVK ajustado para Bifrost y Valhall (G57, G68, G77, G78)")
s("src_minegraph", "PanVK for command-stream Mali (G610 and newer)", "PanVK para Mali com command stream (G610 em diante)", "PanVK para Mali con command stream (G610 en adelante)")
s("src_faux123_panvk", "PanVK built for the Mali-G610 (CSF)", "PanVK feito para a Mali-G610 (CSF)", "PanVK hecho para la Mali-G610 (CSF)")

# debug
s("dbg_logs", "Logs", "Registros", "Registros")
s("dbg_logs_sub", "What to send when something goes wrong", "O que mandar quando algo der errado", "Lo que enviar cuando algo falla")
s("dbg_export", "Export logs", "Exportar logs", "Exportar registros")
s("dbg_verbose", "Detailed log", "Log detalhado", "Registro detallado")
s("dbg_verbose_short", "More lines in the game's log.", "Mais linhas no log do jogo.", "Más líneas en el registro del juego.")
s("dbg_verbose_long", "Writes debug-level lines to the game's log (logs/rb2.log). Use it to chase a crash or a graphics problem, then turn it off: it costs a little performance and space.",
  "Escreve linhas de depuração no log do jogo (logs/rb2.log). Use para investigar um travamento ou problema gráfico e depois desligue: custa um pouco de desempenho e espaço.",
  "Escribe líneas de depuración en el registro del juego (logs/rb2.log). Úsalo para investigar un cierre o un fallo gráfico y luego desactívalo: cuesta algo de rendimiento y espacio.")
s("dbg_last_log", "End of the last game log", "Fim do último log do jogo", "Final del último registro del juego")
s("dbg_no_log", "No game log yet.", "Ainda não há log do jogo.", "Aún no hay registro del juego.")
s("dbg_overlay", "In-game info", "Informações no jogo", "Información en el juego")
s("dbg_gpu_stats", "GPU statistics", "Estatísticas da GPU", "Estadísticas de GPU")
s("dbg_gpu_stats_short", "GPU time, draws and clock on screen.", "Tempo de GPU, draws e clock na tela.", "Tiempo de GPU, draws y reloj en pantalla.")
s("dbg_gpu_stats_long", "Shows GPU time per frame, draw calls, CPU and GPU load and temperature over the game. For testing drivers and settings; turn it off to play.",
  "Mostra o tempo de GPU por quadro, draw calls, carga e temperatura da CPU e da GPU por cima do jogo. Para testar drivers e configurações; desligue para jogar.",
  "Muestra el tiempo de GPU por fotograma, draw calls, carga y temperatura de CPU y GPU sobre el juego. Para probar drivers y ajustes; desactívalo para jugar.")
s("dbg_args", "Extra launch options", "Opções extras de inicialização", "Opciones extra de inicio")
s("dbg_args_sub", "For testing: one option per line", "Para testes: uma opção por linha", "Para pruebas: una opción por línea")
s("dbg_args_long", "Options handed to the game's runtime when it starts, one per line (for example --vsync=false). They replace the launcher's value for the same option. Only for tests asked for in a bug report: a wrong option can stop the game from starting. Clear the box to go back to normal.",
  "Opções passadas para o runtime do jogo quando ele abre, uma por linha (por exemplo --vsync=false). Elas substituem o valor da launcher para a mesma opção. Só para testes pedidos num relato de bug: uma opção errada pode impedir o jogo de abrir. Apague o texto para voltar ao normal.",
  "Opciones que se pasan al runtime del juego al iniciar, una por línea (por ejemplo --vsync=false). Sustituyen el valor de la launcher para la misma opción. Solo para pruebas pedidas en un informe de error: una opción equivocada puede impedir que el juego abra. Borra el texto para volver a lo normal.")
s("dbg_maint", "Maintenance", "Manutenção", "Mantenimiento")
s("dbg_clear_cache", "Clear shader cache (%1$s)", "Limpar cache de shaders (%1$s)", "Borrar caché de shaders (%1$s)")
s("dbg_clear_cache_help", "After a driver change or odd colours, clearing the cache makes the game rebuild its shaders (the first fights stutter a little).",
  "Depois de trocar de driver ou se aparecerem cores estranhas, limpar o cache faz o jogo refazer os shaders (as primeiras lutas engasgam um pouco).",
  "Tras cambiar de driver o si aparecen colores raros, borrar la caché hace que el juego rehaga los shaders (los primeros combates tironean un poco).")
s("dbg_reset", "Reset settings", "Restaurar configurações", "Restablecer ajustes")
s("dbg_reset_text", "Every launcher setting goes back to its default. Game files, saves and drivers stay.", "Todas as configurações da launcher voltam ao padrão. Arquivos do jogo, saves e drivers continuam.", "Todos los ajustes de la launcher vuelven a su valor por defecto. Archivos del juego, partidas y drivers se quedan.")
s("dbg_device", "Device", "Aparelho", "Dispositivo")
s("dbg_copy", "Copy", "Copiar", "Copiar")

# about
s("about_title", "Dragon Blast", "Dragon Blast", "Dragon Blast")
s("about_version", "Version %1$s", "Versão %1$s", "Versión %1$s")
s("about_ui_only", "Test build: launcher only, without the game engine", "Build de teste: só a launcher, sem o motor do jogo", "Build de prueba: solo la launcher, sin el motor del juego")
s("about_text", "Native Android port of Dragon Ball: Raging Blast 2 (Xbox 360). The game's PowerPC code is recompiled ahead of time to ARM64 and runs natively, with no emulator, on the ReXGlue runtime over Vulkan.",
  "Port nativo para Android do Dragon Ball: Raging Blast 2 (Xbox 360). O código PowerPC do jogo é recompilado antes para ARM64 e roda nativo, sem emulador, sobre o runtime do ReXGlue em Vulkan.",
  "Port nativo para Android de Dragon Ball: Raging Blast 2 (Xbox 360). El código PowerPC del juego se recompila de antemano a ARM64 y se ejecuta de forma nativa, sin emulador, sobre el runtime de ReXGlue en Vulkan.")
s("about_by", "Android port and launcher by DFD (dfdx047).", "Port para Android e launcher por DFD (dfdx047).", "Port para Android y launcher por DFD (dfdx047).")
s("about_credits", "Credits", "Créditos", "Créditos")
s("credit_recomp", "The static recompilation of the game", "A recompilação estática do jogo", "La recompilación estática del juego")
s("credit_android", "The first Android port this one builds on", "O primeiro port para Android, base deste", "El primer port para Android, base de este")
s("credit_p8", "Android runtime and SDK patches shared with RB2-Android", "Runtime Android e patches do SDK usados pelo RB2-Android", "Runtime Android y parches del SDK que usa RB2-Android")
s("credit_rexglue", "Xbox 360 recompilation SDK and runtime", "SDK e runtime de recompilação do Xbox 360", "SDK y runtime de recompilación de Xbox 360")
s("credit_xenia", "Xbox 360 GPU emulation the runtime comes from", "Emulação da GPU do Xbox 360 que deu origem ao runtime", "Emulación de la GPU de Xbox 360 en la que se basa el runtime")
s("credit_sdl", "Window, input and audio", "Janela, controles e áudio", "Ventana, controles y audio")
s("credit_mesa", "Open Vulkan drivers for Adreno and Mali", "Drivers Vulkan abertos para Adreno e Mali", "Drivers Vulkan abiertos para Adreno y Mali")
s("credit_adrenotools", "Loading custom Vulkan drivers", "Carregamento de drivers Vulkan alternativos", "Carga de drivers Vulkan alternativos")
s("credit_turnip", "Turnip and Adreno driver builds", "Builds de Turnip e drivers Adreno", "Builds de Turnip y drivers Adreno")
s("credit_panvk", "PanVK and Mali driver builds", "Builds de PanVK e drivers Mali", "Builds de PanVK y drivers Mali")
s("credit_font", "Title font (SIL Open Font License)", "Fonte dos títulos (SIL Open Font License)", "Fuente de los títulos (SIL Open Font License)")
s("about_app", "App", "Aplicativo", "Aplicación")
s("about_language", "Language", "Idioma", "Idioma")
s("about_language_short", "Follows the phone unless you pick one.", "Segue o celular, a não ser que você escolha um.", "Sigue al móvil salvo que elijas uno.")
s("about_language_system", "Phone", "Celular", "Móvil")
s("about_legal", "Legal", "Aviso legal", "Aviso legal")
s("about_legal_text", "No game data comes with this app: you need your own legally obtained copy of Dragon Ball: Raging Blast 2 for Xbox 360. Dragon Ball and Raging Blast are trademarks of their owners; this project is not affiliated with or endorsed by them. Icons, artwork and animations here are original.",
  "Este app não traz nenhum arquivo do jogo: você precisa da sua própria cópia legal do Dragon Ball: Raging Blast 2 de Xbox 360. Dragon Ball e Raging Blast são marcas de seus donos; este projeto não tem ligação nem aprovação deles. Ícones, artes e animações daqui são originais.",
  "Esta app no incluye datos del juego: necesitas tu propia copia legal de Dragon Ball: Raging Blast 2 para Xbox 360. Dragon Ball y Raging Blast son marcas de sus dueños; este proyecto no está afiliado ni respaldado por ellos. Los iconos, ilustraciones y animaciones son originales.")
s("about_notices", "Open-source licences", "Licenças de código aberto", "Licencias de código abierto")

# busy + messages
s("busy_import", "Importing the ISO", "Importando a ISO", "Importando la ISO")
s("busy_delete", "Deleting the game files", "Apagando os arquivos do jogo", "Borrando los archivos del juego")
s("busy_driver", "Installing driver", "Instalando driver", "Instalando driver")
s("busy_logs", "Packing the logs", "Juntando os logs", "Empaquetando los registros")
s("msg_import_ok", "Game installed: %1$d files, %2$s GB. Ready to play!", "Jogo instalado: %1$d arquivos, %2$s GB. Pronto para jogar!", "Juego instalado: %1$d archivos, %2$s GB. ¡Listo para jugar!")
s("msg_import_failed", "The ISO was not imported. See the Game files card.", "A ISO não foi importada. Veja o cartão Arquivos do jogo.", "No se importó la ISO. Mira la tarjeta Archivos del juego.")
s("msg_game_deleted", "Game files deleted.", "Arquivos do jogo apagados.", "Archivos del juego borrados.")
s("msg_driver_ready", "%1$s installed and selected.", "%1$s instalado e selecionado.", "%1$s instalado y seleccionado.")
s("msg_logs_failed", "Could not pack the logs.", "Não deu para juntar os logs.", "No se pudieron empaquetar los registros.")
s("msg_cache_cleared", "Shader cache cleared (%1$s).", "Cache de shaders limpo (%1$s).", "Caché de shaders borrada (%1$s).")
s("msg_settings_reset", "Settings restored.", "Configurações restauradas.", "Ajustes restablecidos.")
s("no_engine_title", "Launcher test build", "Build de teste da launcher", "Build de prueba de la launcher")
s("no_engine_text", "This build has only the launcher: the game engine (Raging Blast 2's recompiled code) is not included yet, so the game cannot start. Everything else here (import, settings, drivers, logs) works and is kept for the full build.",
  "Esta build tem só a launcher: o motor do jogo (o código recompilado do Raging Blast 2) ainda não está incluído, então o jogo não abre. Todo o resto aqui (importação, configurações, drivers, logs) funciona e fica guardado para a build completa.",
  "Esta build solo tiene la launcher: el motor del juego (el código recompilado de Raging Blast 2) aún no está incluido, así que el juego no puede abrirse. Todo lo demás (importación, ajustes, drivers, registros) funciona y se conserva para la build completa.")

# iso errors
s("iso_err_open", "Could not open the selected file.", "Não deu para abrir o arquivo escolhido.", "No se pudo abrir el archivo elegido.")
s("iso_err_not_xbox", "This is not an Xbox 360 disc image (no XDVDFS file system found).", "Isto não é uma imagem de disco de Xbox 360 (sem sistema de arquivos XDVDFS).", "Esto no es una imagen de disco de Xbox 360 (no hay sistema de archivos XDVDFS).")
s("iso_err_incomplete", "The disc image is incomplete (%1$s ends past the end of the file). Copy or dump the disc again.", "A imagem do disco está incompleta (%1$s passa do fim do arquivo). Copie ou extraia o disco de novo.", "La imagen del disco está incompleta (%1$s termina después del final del archivo). Copia o vuelca el disco de nuevo.")
s("iso_err_no_xex", "No default.xex in this image: is it the right game?", "Não há default.xex nesta imagem: é o jogo certo?", "No hay default.xex en esta imagen: ¿es el juego correcto?")
s("iso_err_space", "Not enough free space: the game needs %1$s GB, %2$s GB are free.", "Espaço livre insuficiente: o jogo precisa de %1$s GB e há %2$s GB livres.", "No hay espacio suficiente: el juego necesita %1$s GB y hay %2$s GB libres.")
s("iso_err_zeroed", "The disc image is incomplete: %1$s is empty (all zeros). Copy or dump the disc again.", "A imagem do disco está incompleta: %1$s está vazio (só zeros). Copie ou extraia o disco de novo.", "La imagen del disco está incompleta: %1$s está vacío (solo ceros). Copia o vuelca el disco de nuevo.")
s("iso_err_version", "This disc is a version of the game the port does not support: it needs the USA \"Rev 1\" disc. Your current install was not changed.", "Este disco é uma versão do jogo que o port não suporta: ele precisa do disco USA \"Rev 1\". A sua instalação atual não foi alterada.", "Este disco es una versión del juego que el port no admite: necesita el disco USA \"Rev 1\". Tu instalación actual no se ha cambiado.")
s("iso_err_original", "This is the original USA release (v1.0). The port supports only the later USA \"Rev 1\" disc (sometimes listed as v1.1). Your current install was not changed.", "Esta é a versão original USA (v1.0). O port suporta só o disco USA \"Rev 1\" (às vezes listado como v1.1). A sua instalação atual não foi alterada.", "Esta es la versión original USA (v1.0). El port solo admite el disco USA \"Rev 1\" (a veces listado como v1.1). Tu instalación actual no se ha cambiado.")
s("iso_err_table", "The disc's directory table is damaged.", "A tabela de diretórios do disco está danificada.", "La tabla de directorios del disco está dañada.")
s("iso_err_move", "Could not move the extracted game into place.", "Não deu para colocar o jogo extraído no lugar.", "No se pudo mover el juego extraído a su sitio.")
s("iso_err_io", "Reading the disc image failed: %1$s", "A leitura da imagem do disco falhou: %1$s", "Falló la lectura de la imagen del disco: %1$s")

def esc(t):
    t = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    t = t.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if t.startswith("@") or t.startswith("?"): t = "\\" + t
    return t

res = pathlib.Path(sys.argv[1])
for idx, folder in enumerate(["values", "values-pt", "values-es"]):
    out = ['<?xml version="1.0" encoding="utf-8"?>', '<!-- Generated by tools/launcher/gen_strings.py; edit there or here, keep the three languages in step. -->', "<resources>"]
    for k, v in S.items():
        if idx > 0 and k == "app_name": continue
        extra = ' formatted="false"' if ("%" in v[idx] and "%1$" not in v[idx]) else ""
        out.append(f'    <string name="{k}"{extra}>{esc(v[idx])}</string>')
    out.append("</resources>\n")
    (res / folder).mkdir(exist_ok=True)
    (res / folder / "strings.xml").write_text("\n".join(out), encoding="utf-8")
print(len(S), "strings")
