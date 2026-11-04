# Comparação das duas versões do AuctionHouse

Análise estática em 03/11/2026. Nenhum código de execução, HTML ou banco foi alterado; os projetos externos não foram compilados nem executados.

Referências:

- Adaptação do usuário: `C:/Users/llean/Documents/Mobius h5 Java 25/L2J_Mobius_CT_2.6_HighFive`.
- Material do fórum: `C:/Users/llean/Pictures/Mobius Source Files MmoDev Forum`.
- Destino: `C:/Users/llean/git/mobiuz`.
- Complementa `docs/viabilidade-auctionhouse-mobius.md`, que detalha os riscos da versão do fórum.

## Conclusão

É possível aproveitar trechos da adaptação Sunrise. Ela oferece exemplos de integração com APIs mais próximas da Journey, bloqueios adicionais durante loja privada e uma alternativa de apresentação dos atributos dos itens. Não encontrei um mecanismo de liquidação ou recuperação mais seguro que resolva os problemas identificados na outra versão.

As duas mantêm o mesmo modelo funcional: anúncios com preço fixo em Adena, categorias e busca, compra parcial, cancelamento e correio para pagamento/devolução. A adaptação não acrescenta um sistema de lances. A semelhança dos fluxos é maior que a diferença de organização dos arquivos.

## Diferenças relevantes

| Aspecto | Adaptação Sunrise | Material do fórum | Aproveitamento para Journey |
| --- | --- | --- | --- |
| Loja privada | Bloqueia criação, retirada e compra quando `PrivateStoreType != NONE` | Não localizei os mesmos bloqueios explícitos nesses fluxos | Incorporar as verificações aos caminhos de mutação |
| Inventário e correio | Usa `World.getInstance()`, `Message`, `Mail`, `CharInfoTable` e escolhe InventoryUpdate/ItemList conforme configuração | Usa APIs reorganizadas | Consultar a adaptação como exemplo concreto para as APIs existentes, sem importar toda a estrutura |
| Inicialização | GameServer inicializa o manager explicitamente | Inicialização pelo singleton ao primeiro uso | Restaurar custódia e reconciliar anúncios na inicialização da futura implementação |
| Coleções | Mapas externos ConcurrentHashMap | Mapas externos HashMap | Proteger todo o estado composto; trocar somente o mapa não resolve listas e operações concorrentes |
| SQL | Declara InnoDB explicitamente | Tabela não declara engine | Manter InnoDB; acrescentar estados e recuperação, pois a engine não torna inventário/correio atomicamente consistentes |
| Taxa | `long` inicializado com `(long) 0.5`, sem leitura de configuração localizada | Percentual configurável, convertido por divisão por 100 | Preferir o modelo configurável do fórum, com aritmética verificada |
| Templates | `Files.read` a cada renderização | HtmCache | Preferir o cache nativo da Journey |
| Atributos mostrados | Cálculo próprio de atributos básicos e bônus de enchant | Consulta funções de stats do template | Reaproveitar a intenção visual; calcular com as regras do destino |

## Falhas específicas da adaptação

1. **Taxa truncada para zero.** Em `java/org/l2jmobius/Config.java:1022`, `(long) 0.5` resulta em zero. As fórmulas nas linhas 1376 e 1496 do manager aplicam `Math.max(10000, ...)`, portanto a cobrança fica sempre no piso de 10.000 Adena. Não localizei carregamento dessas duas configurações do AuctionHouse a partir dos INIs. Além disso, usar simplesmente 0,5 como multiplicador representaria 50%, caso a intenção fosse 0,5%.

2. **Restrição de zona de paz ineficaz.** Em `communitybbs/Manager/AuctionBBSManager.java:55`, o ramo de bloqueio apenas envia uma mensagem e continua chamando `processBypass`. O ramo de prisão na linha 45 está vazio. O manager verifica prisão nas mutações; isso não equivale a uma validação efetiva de acesso no handler, nem corrige a restrição de zona de paz.

3. **Envio duplicado na busca.** No mesmo handler, a busca envia os pacotes especiais 1001/1002 e depois também chama `CommunityBoardHandler.separateAndSend(content, activeChar)`. É uma sobreposição de mecanismos que deve ser substituída pelo fluxo compatível com o handler nativo da Journey. O retorno é sempre false, inclusive quando o comando foi processado. O encaminhamento existe na HomeBoard externa; não é um caso de handler simplesmente não registrado.

4. **Regras de atributos duplicadas.** `AuctionHouseManager.java:928` e `:946` implementam `getTemplateStatValue` e `getFinalStatValue`. O primeiro retorna a primeira função de stat que atenda aos filtros, sem avaliar todas as condições/composições possíveis. O segundo repete fórmulas de enchant que a Journey já mantém em `model/stats/functions/FuncEnchant.java`. Isso pode divergir em itens customizados ou após mudanças de regras. Não apresentar essa estimativa como atributo final completo do personagem equipado.

5. **Classe Generator não valida os templates ao inicializar.** A mensagem de inicialização verifica as strings de caminhos, e os arquivos são lidos posteriormente. A existência/disponibilidade dos HTMLs deve ser verificada de verdade. Separar SQL nessa classe não acrescentou propagação confiável das falhas: métodos de gravação capturam a exceção e não comunicam ao chamador que a operação falhou.

## Problemas compartilhados que continuam exigindo correção

- Cobrança antes de transferir o item, sem compensação explícita caso a transferência falhe. Na adaptação: `purchaseItem`, linhas 1862–1873.
- Inclusão do anúncio na memória antes da persistência; na adaptação: `addNewAuctionToDB`, linhas 1976–1982.
- Transferência para o comprador, pagamento por correio ao vendedor e atualização do anúncio em operações independentes; na adaptação: linhas 1869–1912. Não há diário durável comum de liquidação.
- Quantidade calculada antes da seção que efetua a compra, sem revalidação suficiente do estoque/validade dentro da exclusão usada pela transação; continuam necessários limites inferiores, aritmética segura e conferência de identidade/dono/localização do objeto.
- Expiração detectada pela navegação e devolução marcada antecipadamente, sem recuperação robusta de falhas.
- Formatadores estáticos e listas mutáveis não ficam seguros apenas porque dois mapas passaram a ConcurrentHashMap.
- Restauração de custódia agrega pilhas do mesmo item nas duas versões. Como anúncios são referenciados por objectId, essa agregação merece teste específico com vários anúncios do mesmo item por vendedor após reiniciar: não assumir que preservar o total de itens preserva a identidade de cada anúncio.

Não localizei uso de `auction_watch` nas fontes Java desta adaptação; a presença desse SQL não demonstra uma funcionalidade de acompanhamento integrada ao AuctionHouse.

## HTML e integração sugerida

Os três templates correspondentes das versões têm os mesmos conjuntos de placeholders. Existem diferenças de marcação e apresentação, mas elas não demonstram uma funcionalidade de negociação adicional. Isso permite escolher uma referência visual, mantendo alinhados template e gerador daquela referência. Não misturar geradores e HTMLs sem verificar o resultado High Five.

Para a futura implementação: usar a adaptação como referência de inventário/correio e dos bloqueios de loja; aproveitar o modelo de configuração e cache da versão do fórum; construir a liquidação, revalidação e recuperação segundo as APIs reais da Journey. Registrar handler próprio, preservando a HomeBoard atual e seu HTML já validado.

Antes de uso com jogadores, validar concorrência entre compradores, falha de transferência/gravação/correio, reinício durante cada etapa, múltiplos anúncios do mesmo item, cancelamento versus compra e expiração sem navegação. Esta análise não constitui validação ingame.
