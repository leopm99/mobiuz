# Serviço de cores do Community Board

O handler continua sendo `handlers/communityboard/ColorChangeBoard.java`, com o bypass `_bbcolorchange`. A RouletteService não foi alterada.

## Apresentação

O HTML segue o cabeçalho de 720 pixels da SubClassMasterBoard/HomeBoard, com `L2UI_CH3.refinewnd_back_Pattern`, painel central `L2UI_CT1.Windows_DF_Drawer_Bg`, fontes e botões nativos. O botão de retorno no canto superior direito aponta para `_bbstop;merchant/servicesMain.html`.

A lista padrão tem dez cores: branco, vermelho, verde, azul, dourado, rosa, ciano, laranja, roxo e prata. Laranja (`FFA500`), roxo (`9932CC`) e prata (`C0C0C0`) são as três opções novas. As colunas permitem alterar nome ou título separadamente; a cor atual é identificada e não é cobrada novamente. A página acompanha o idioma br/en usado pelas demais boards.

## Correções

- Validação exata do bypass e dos tipos nick/title; parâmetros extras e cores fora da paleta são rejeitados.
- Validação da paleta e do item/preço antes da cobrança; quantidade zero é gratuita, valores negativos são inválidos.
- Configuração lida a cada requisição, sem cópias estáticas que fixem a paleta antiga na classe.
- Verificações de estado do personagem, restrições do Community Board, permissão de transação e flood protection.
- Persistência da coluna name_color/title_color e da variável correspondente de login em character_variables. A aparência e a variável em memória são alteradas após confirmação da gravação.
- Falhas SQL passam pelo logger padrão da Journey. Uma falha antes de iniciar o commit, com rollback confirmado ou sem transação iniciada, permite compensar a cobrança. Um commit de resultado incerto não gera reembolso automático nem mensagem de sucesso: exige conferência administrativa, evitando devolver o custo de uma alteração que pode ter sido gravada.
- Removidos CSS, tag scrollbar e o prefixo # dos códigos de cor no HTML.

Inventário e persistência de cor usam APIs/conexões diferentes; isto não torna a cobrança e a cor uma operação atômica única. O tratamento acima cobre falhas normais de SQL, mas não promete reconciliação automática de uma queda do processo entre todas as etapas. São usadas as tabelas já existentes; nenhum SQL novo foi criado ou executado.

## Atualização

Atualize o script `dist/game/data/scripts/handlers/communityboard/ColorChangeBoard.java` e as duas listas `ColorChangeColors` / `ColorChangeColorNames` em `dist/game/config/communityboard.ini`, preservando preço e habilitação desejados. Reinicie o servidor. Para essa implantação, o núcleo existente já possui as configurações e APIs necessárias.

Os valores de fallback de Config.java também foram ampliados para dez cores e entrarão na próxima compilação do núcleo. Com as listas explícitas no INI atualizado, essa recompilação não é necessária para testar o serviço.

Os testes usam personagem, inventário e SQL simulados, incluindo validação de estrutura e tamanho do HTML com dez cores. Falta validar a apresentação no cliente ingame, a alteração paga/gratuita e a manutenção das cores após relogar.
