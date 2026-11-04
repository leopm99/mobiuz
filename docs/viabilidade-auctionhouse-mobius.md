# Viabilidade de adaptação do AuctionHouse para a Journey

Data: 03/11/2026. Análise estática do material em `C:/Users/llean/Pictures/Mobius Source Files MmoDev Forum`, comparado a `C:/Users/llean/git/mobiuz`.

## Conclusão

A adaptação é viável e não exige migrar a arquitetura do servidor. A compatibilidade de base é favorável: ambos são High Five/Java 25 e a Journey já fornece inventário, correio com anexos, flood protection, Community Board e ItemLocation.AUCTION. O esforço é médio a alto porque a lógica de compra, custódia, pagamento e recuperação de falhas precisa ser reforçada antes de expor itens de jogadores.

Não se recomenda uma conversão mecânica de imports ou colocar o código do material diretamente em produção. A interface, filtros e parte das regras podem servir de base; a liquidação das transações exige trabalho específico.

Apenas este documento foi criado. Nenhum arquivo do AuctionHouse foi incorporado, compilado ou executado, e nenhum SQL foi aplicado.

## O que a funcionalidade faz

Apesar do nome, a implementação é um mercado de venda direta a preço fixo, não um sistema de lances entre compradores.

- Anúncios pelo Community Board, com categorias de armas, armaduras e outros itens, filtros, pesquisa, ordenação e paginação.
- Venda de equipamento individual ou quantidade de item empilhável; compra parcial de pilhas.
- Tela de anúncios próprios, criação, confirmação de compra e cancelamento pelo vendedor.
- Até 10 anúncios por personagem; quantidade aceita na criação de 1 a 1.000.000; preço unitário de 1 a 10.000.000.000 Adena; duração de 1 a 9 dias.
- Cobrança ao anunciar: quantidade × preço unitário × dias × percentual configurado, com mínimo de 10.000 Adena. O padrão da configuração é 0,5% por dia. Não é simplesmente uma comissão descontada do vendedor depois da venda.
- O item anunciado sai do inventário e fica em contêiner de custódia com localização AUCTION.
- Comprador recebe no inventário; vendedor recebe Adena pelo correio ingame, inclusive quando desconectado.
- Itens expirados voltam pelo correio. O cancelamento feito pelo dono tenta devolver ao inventário.

A moeda é Adena, fixada no código. `CommunityCurrencyId` não torna automaticamente as vendas deste módulo utilizáveis com qualquer moeda. Uma moeda configurável exigiria mudanças explícitas, caso desejada.

## Dependências encontradas

No material externo:

| Componente | Papel |
| --- | --- |
| `java/org/l2jmobius/gameserver/managers/AuctionHouseManager.java` | Gerenciamento, regras, SQL e construção dinâmica do HTML; 2.452 linhas |
| `entity/item/AuctionHouseItem.java` | Contêiner de custódia e restauração dos itens |
| `entity/itemcontainer/AuctionHouse.java` | Enumeração das categorias de anúncios |
| `mechanics/conditions/AuctionConditions.java` | Classificação por tipo, slot, skill, nome, ícone etc. |
| `dist/game/data/html/CommunityBoard/Custom/auction/auction_house_server.htm` | Listagem |
| `auction_house_my.htm` | Anúncios próprios |
| `auction_house_purchase.htm` | Compra/confirmacão |
| `dist/db_installer/sql/game/auction_house.sql` | Metadados dos anúncios |
| `CommunityBoardConfig` / `CommunityBoard.ini` | Habilitação e percentual de taxa |
| `handlers/bypass/communityboard/HomeBoard.java` | Registro e encaminhamento do prefixo `_bbs_auction` |

Não identifiquei bibliotecas externas específicas nem alteração de cliente exigida por esse módulo. Ele usa os HTMLs e pacotes já esperados pelo Community Board High Five. Isso ainda requer confirmação visual no cliente usado pela Journey.

## Equivalentes na Journey

- `model/item/enums/ItemLocation.java` já contém AUCTION. Não é necessário inventar uma nova localização ou alterar essa enumeração só para comportar a funcionalidade.
- `model/itemcontainer/ItemContainer.java` oferece transferência de itens entre contêineres e persistência; o comportamento de redução automática da quantidade solicitada exige atenção em compras concorrentes.
- `Player.checkItemManipulation`, inventário, validação de peso/capacidade, flags de transação e flood protection têm equivalentes.
- `model/Message.java` e `MailManager` substituem o MailMessage do material externo e oferecem correio com anexos.
- `CharInfoTable.getNameById` equivale à consulta de nomes via PlayerInfoTable usada pelo material.
- `Config.java`, a configuração atual do Community Board e o MasterHandler permitem registrar o recurso sem trazer o ConfigLoader externo.
- A Journey já tem ItemAuctionManager e ClanHallAuctionManager, mas são outros sistemas. Não substituem um mercado P2P e não precisam ser trocados por este módulo.

A integração preferida é um handler próprio para `_bbs_auction`, com habilitação explícita e telas específicas. Preservar a estrutura já validada da HomeBoard; não importar a HomeBoard externa inteira nem refazer seu cabeçalho/menu como efeito colateral.

## Falhas e pontos que precisam ser corrigidos

### 1. Cobrança antes da transferência, sem compensação clara

`AuctionHouseManager.purchaseItem`, a partir da linha 1842, reduz a Adena e só depois transfere o item. Se a transferência retornar null, o método mostra um erro e retorna sem devolver explicitamente a quantia. A criação do anúncio também cobra a taxa antes de transferir o item para a custódia.

A adaptação precisa tratar falhas em cada estágio e registrar o resultado de forma recuperável. Envolver apenas o INSERT do anúncio em uma transação SQL não resolve tudo: inventário e correio possuem suas próprias operações/conexões de persistência.

### 2. Anúncio publicado em memória antes de sua gravação no banco

`addNewAuctionToDB`, linha 2040 em diante, adiciona o anúncio à lista antes do INSERT; em caso de erro SQL, apenas emite aviso. O item já pode ter saído do inventário, mas o anúncio não será restaurável no reinício.

Exige gravação consistente de anúncio/custódia e tratamento de itens órfãos. A tabela original contém apenas itemId, ownerId, count, sale_price e expiration_time; não tem estados de operação nem histórico de liquidação para recuperação.

### 3. Entrega, correio de pagamento e atualização do anúncio são etapas independentes

Após entregar ao comprador, o método cria/envia o correio ao vendedor e só depois atualiza/remove o anúncio. Não há uma liquidação durável comum aos três efeitos. Queda ou erro nesse intervalo pode deixar divergência entre estoque, entrega e pagamento.

A adaptação precisa de identificação de operação, estados persistidos e proteção contra repetição, além de reconciliação no restart. Não prometer atomicidade completa reutilizando as APIs de inventário e correio sem verificar como elas gravam seus dados.

### 4. Concorrência e validação final da quantidade

`makePurchaseHtm`, próximo da linha 961, calcula purchaseCount por Math.min antes de chamar o método synchronized purchaseItem. Este último não revalida o estoque disponível e a validade temporal dentro do bloqueio de forma suficiente.

Na Journey, `ItemContainer.transferItem`, próximo da linha 355, reduz a quantidade pedida ao estoque disponível. Uma compra concorrente que tenha calculado a quantidade antes de outra venda pode cobrar pela quantidade antiga e transferir menos itens. É um risco identificado pelo encadeamento estático; não foi executado um teste de concorrência neste levantamento.

Há ainda limite superior via Math.min, mas não uma validação explícita purchaseCount >= 1 nesse ponto. Devem ser conferidos quantidade, dono, localização, identidade do objeto e anúncio ativo dentro da mesma exclusão usada para comprar/cancelar/expirar. Valores monetários e peso devem usar operações de long verificadas, evitando multiplicações intermediárias em int.

### 5. Expiração dependente da navegação e retorno com flag antecipada

Os anúncios expirados são identificados ao construir páginas de listagem. A única tarefa localizada é o envio agendado de devolução depois dessa detecção; não há varredura periódica independente para expiração.

`returnItemToOwner`, próximo da linha 1985, marca isRemoved antes de a transferência e o envio de correio terem sido bem-sucedidos. Se falharem, o anúncio pode deixar de ser processado naquela execução.

Exige uma tarefa própria, estado persistente de devolução e repetição segura. O status de expirado não pode depender de alguém visitar o Community Board.

### 6. Renderização concorrente e entradas de busca

DecimalFormat e DateFormat são estáticos e usados por fluxos de renderização que podem ser concorrentes. Listas ArrayList são percorridas em caminhos sem a mesma proteção aplicada às mutações. Precisam de snapshots ou sincronização coerente e formatação segura para múltiplas threads.

Busca, categoria e ordenação recebidas do cliente são concatenadas em ações de HTML. Limites, escape e enumeração dos valores aceitos precisam ser revistos para evitar HTML inválido e comandos malformados, especialmente considerando a sensibilidade do cliente High Five a HTML.

### 7. Banco, restauração e elegibilidade dos itens

A tabela fornecida não declara engine. Na adaptação, usar InnoDB explicitamente, colunas nomeadas nos comandos SQL, restrições e estados adequados ao novo fluxo. A quantidade MEDIUMINT suporta o limite original de um milhão, mas deve ser definida conforme o limite de produto efetivamente adotado.

A restauração em `AuctionHouseItem.restore` precisa ser conferida junto a `Item.restoreFromDb` da Journey para manter enchant, elementos, augmentation e demais dados auxiliares. Também revisar itens temporários, quest items, itens de pet/montaria, itens protegidos/bloqueados e todas as restrições de negociação já existentes na Journey. A implementação possui verificações úteis de trade, enchant, jail, acesso e peso/capacidade, mas elas não equivalem a cobertura completa de todos os casos.

## Proposta de adaptação por fases

1. Definir regras: Adena inicialmente, limite de anúncios, preço unitário versus total, taxa de publicação, prazo e itens permitidos. Manter desabilitado por configuração até completar os testes.
2. Implementar custódia, persistência dos anúncios e recuperação de falhas antes de ligar a interface de compra.
3. Adaptar listagem/confirmacão/cancelamento pelo handler próprio e HTMLs limitados ao AuctionHouse.
4. Implementar pagamento pelo correio, compra parcial e expiração periódica com operações recuperáveis e sem repetição.
5. Testar em cópia do servidor/banco: dois compradores no mesmo anúncio, compra simultânea com cancelamento/expiração, falha no banco, queda em cada etapa, restauração após restart, vendedor offline, inventário cheio, peso, atributos do item e tentativas de manipular objeto/quantidade/preço.

## Parecer final

Viável com boa aderência funcional à Journey, sem necessidade de bibliotecas de pagamento, site externo ou modificação do cliente identificada. O esforço principal está em confiabilidade de inventário, Adena e correio. Recomenda-se reutilizar a apresentação e as regras úteis e reforçar as operações críticas; não apenas traduzir os pacotes e colocar a implementação original para rodar.
