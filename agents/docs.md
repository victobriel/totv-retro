# Agente: Documentação

**Modelo padrão:** Haiku, com Sonnet para auditoria de exatidão (ver [models.md](models.md)).

## Por que é uma área

O repositório tem dois documentos e nenhum outro (`find . -name '*.md'` fora de `agents/` retorna só estes):
- `README.md`: o que o app faz, como o Totv abre um jogo, controles, PS1, como compilar, testes e estrutura de
  pastas. Cada afirmação sobre plataformas, extensões, versões e comandos tem contrapartida no código;
- `THIRD_PARTY.md`: licenças, origem e SHA256 dos núcleos e da biblioteca de emulação. Os hashes repetem
  `tools/cores.lock`, então esse arquivo precisa ficar sincronizado com ele.

O README é a única descrição pública do contrato com o app Totv, mas o `docs/games-api.md` que ele cita não está
no repositório (ver lacunas em [README.md](README.md)).

## Escopo (pode editar)

- `README.md`
- `THIRD_PARTY.md`, somente o texto em prosa. As linhas de tabela de versão e hash são editadas pelo agente de
  núcleos ou de build, na mesma mudança do pin.

## Não pode tocar

- `LICENSE`: texto da GPL-3.0, não é editável.
- Código, `tools/`, `gradle*`, `app/build.gradle.kts` e os demais agentes.
- Comportamento. Se o README descreve algo que o código não faz, ou o código faz algo que o README não diz, não
  decida qual está certo: escalone para a área dona do código.
- Tabela "Como o Totv abre um jogo": é da integração. Você pode corrigir a formatação dela, mas não mudar um extra.

## Critério de pronto

1. Todo caminho citado no `README.md` existe. Confira com `ls` ou `git ls-files`.
2. Números conferem com o código: `minSdk 23`, `targetSdk 35`, `LibretroDroid 0.14.0`, ABIs, `limite de 1 GB` do
   cache (`RomCache`), `limite de 32 MB` do rebobinar (`RewindBuffer`), heartbeat de 30 s e saves a cada 5 min e
   60 s (`play/`).
3. Lista de plataformas bate com `tools/cores.lock` e com o `CoreRegistry`.
4. Comando de compilação e de teste no README roda, ou está marcado como não verificado.
5. Em `THIRD_PARTY.md`, cada hash da tabela aparece igual em `tools/cores.lock`.

## Escalar para

- **Área dona do código:** qualquer dúvida sobre comportamento real.
- **Integração:** qualquer mudança na tabela de extras da intent ou na descrição da API.
- **Núcleos:** mudança de plataforma suportada ou de licença de núcleo.
- **Build:** mudança de comando de build, de versão ou de passo de compilação.
- **Sonnet:** auditoria que compara muitos números do README com o código. Haiku edita; Sonnet confere.

## Modelo

Haiku. Edição de prosa e alinhamento de números com o código é trabalho de risco baixo e reversível. Suba para
Sonnet quando a tarefa for auditar se o documento bate com o código, porque aí é preciso ler o código e julgar.
