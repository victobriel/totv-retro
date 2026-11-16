package app.totv.retro.ui

/** User-visible strings (pt-BR). Kept in one place so they are easy to translate later. */
object Tx {
    const val appName = "Totv Retro"
    const val infoTitle = "Abra um jogo pelo app Totv"
    const val infoBody = "O Totv Retro é o emulador usado pelo Totv. Escolha um jogo na biblioteca de jogos do app Totv e o Totv Retro abre sozinho."
    const val infoHint = "Recomendamos usar um controle (gamepad) conectado à TV."

    const val loading = "Carregando…"
    const val loadingPlatforms = "Consultando o servidor…"
    const val loadingBios = "Baixando BIOS…"
    const val loadingRom = "Baixando o jogo…"
    const val loadingSave = "Carregando o seu progresso…"
    const val starting = "Iniciando o emulador…"
    const val back = "Voltar"
    const val retry = "Tentar de novo"

    const val errTitle = "Não foi possível abrir o jogo"
    const val errBadIntent = "Pedido de jogo inválido. Abra o jogo pelo app Totv."
    const val errNoCore = "Este aparelho não tem um emulador para a plataforma"
    const val errNetwork = "Falha de rede ao falar com o servidor."
    const val errAuth = "Sessão expirada. Abra o jogo de novo pelo app Totv."
    const val errNotFound = "Jogo não encontrado (ou bloqueado pelo controle parental)."
    const val errSchedule = "Fora do horário permitido para este perfil."
    const val errBios = "BIOS obrigatória ausente no servidor"
    const val errCore = "O emulador falhou ao iniciar o jogo."
    const val stoppedBySchedule = "Fora do horário permitido. O jogo foi encerrado."

    const val menuTitle = "Menu"
    const val resume = "Continuar"
    const val saveState = "Salvar estado"
    const val loadState = "Carregar estado"
    const val restart = "Reiniciar"
    const val fastForward = "Avanço rápido"
    const val aspect = "Proporção"
    const val filter = "Filtro"
    const val controls = "Controles"
    const val exit = "Sair"
    const val on = "Ligado"
    const val off = "Desligado"
    const val aspectOriginal = "Original"
    const val aspectStretch = "Esticar"
    const val filterSharp = "Nítido"
    const val filterSoft = "Suave"
    const val slot = "Slot"
    const val emptySlot = "Vazio"
    const val autoState = "Automático"
    const val savedToast = "Estado salvo"
    const val loadedToast = "Estado carregado"
    const val saveFailed = "Não foi possível salvar o estado"
    const val loadFailed = "Não foi possível carregar o estado"
    const val wrongCore = "Este estado foi salvo por outro emulador e não pode ser carregado"
    const val noStates = "Nenhum estado salvo ainda."
    const val confirmRestartTitle = "Reiniciar o jogo?"
    const val confirmRestartBody = "O progresso que não foi salvo será perdido."
    const val cancel = "Cancelar"
    const val confirmOverwriteTitle = "Sobrescrever este slot?"
    const val gamepadHint = "Dica: um controle (gamepad) deixa a experiência bem melhor. Com o controle remoto: OK = A, Voltar = menu, Play/Pause = Start."
    const val controlsTitle = "Controles"
    val controlsGamepad = listOf(
        "D-pad / analógico esquerdo" to "Direcional",
        "A / B / X / Y" to "Botões do jogo (A leste, B sul, como no SNES)",
        "L1 / R1 (ou L2 / R2)" to "Gatilhos L / R",
        "Start / Select" to "Start / Select",
        "Select + Start" to "Abre o menu",
    )
    val controlsRemote = listOf(
        "Direcionais" to "Direcional",
        "OK" to "Botão A",
        "Play/Pause" to "Start",
        "Voltar" to "Abre o menu",
        "Menu" to "Select",
    )
    const val sectionGamepad = "Controle (gamepad)"
    const val sectionRemote = "Controle remoto da TV"
}
