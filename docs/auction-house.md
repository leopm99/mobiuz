# Auction House da Journey

Implementado em 03/11/2026 a partir da análise das duas referências Mobius/Sunrise. O módulo usa as APIs da Journey e não precisa de bibliotecas adicionais nem de alteração do cliente.

## Instalação para teste

1. Compile novamente a source Java 25 e atualize o `GameServer.jar`/artefato equivalente do seu build. Somente copiar scripts não atualiza o núcleo.
2. Com o servidor desligado, execute `dist/db_installer/sql/game/community_auction.sql` no banco GAME. O arquivo cria duas tabelas novas; não altera inventários nem importa anúncios de `auction_house` das referências.
3. Atualize os scripts `handlers/communityboard/AuctionHouseBoard.java`, `handlers/communityboard/HomeBoard.java` e `handlers/MasterHandler.java` no datapack do servidor de teste.
4. Atualize `data/html/CommunityBoard/auction/main.html` e `data/html/CommunityBoard/home.html`.
5. Copie o bloco novo de `config/communityboard.ini`, preservando suas demais configurações. Habilite `CommunityAuctionEnabled = True`. O módulo foi entregue desabilitado por padrão.
6. Verifique `AllowMail = True` na configuração do servidor. Reinicie. O log deve mostrar `AuctionHouseManager: Restored ... active listings.`. Tabelas ausentes/incompatíveis ou correio desativado deixam o serviço indisponível, sem impedir a inicialização do resto do servidor.
7. O bypass de acesso é `_bbsauction`. O atalho da Home foi removido a pedido; pode ser colocado na página Services.

O caminho de origem dos arquivos acima é `C:/Users/llean/git/mobiuz/dist/`. O código do núcleo fica em `java/com/l2journey/gameserver/auctionhouse/`, além das inserções em Config, GameServer e Shutdown.

## Regras

- Mercado de preço fixo em Adena, sem lances. Quantidade de 1 a 1.000.000, preço unitário de 1 a 10.000.000.000, prazo de 1 a 9 dias.
- Limite padrão de 10 anúncios por personagem, configurável de 1 a 100.
- Taxa padrão: maior valor entre 10.000 Adena e `quantidade × preço unitário × dias × 0,5 / 100`, arredondado para cima. O usuário confirma o valor antes da cobrança. Percentual e mínimo são configuráveis.
- A compra pode ser parcial, sempre com revalidação do estoque disponível. O total de cada compra não pode ultrapassar o limite de Adena configurado no servidor.
- O pagamento integral da venda vai ao vendedor por correio, inclusive se estiver offline. Cancelamento e expiração devolvem os itens não vendidos por correio. A taxa de publicação não é devolvida nesses dois casos.
- Itens equipados, Adena, itens de quest, temporários/shadow, não negociáveis, com augmentation ou visual customizado não são aceitos. As restrições nativas de manipulação/controle de pets também são respeitadas. Pilhas com enchant ou custom types são excluídas, pois a transferência genérica recria a pilha e não garante preservar esses campos. Armas/armaduras comuns não empilháveis preservam seu objeto, enchant e atributos pelas APIs nativas.
- Loja privada, trade, enchant, prisão, morte, teleporte, cast, ataque, combate, duelo, Olympiad, evento, zonas de PvP/siege, karma/PvP flag e bloqueio de inventário e falta de permissão de transação impedem o acesso. A exigência de zona de paz considera a configuração do AuctionHouse e a configuração geral do Community Board.
- Cada confirmação é vinculada ao personagem, expira em dois minutos e pode ser usada uma única vez. Voltar e abrir outra confirmação substitui a anterior.

## HTML

O módulo usa tabelas convencionais, fontes, imagens e botões `L2UI_ct1.button_df`, seguindo as cores e o banner da home atual. Não usa CSS, JavaScript, flex/grid, scrollbar customizado ou elementos de navegador moderno. São sete anúncios por página e navegação horizontal. O atalho da Home foi removido a pedido. Os geradores existentes de banner, raid bosses, serviços e dicas não foram reescritos.

Os testes verificam a estrutura aninhada de table/tr/td, escape de nomes, confirmação única e limite de tamanho do Community Board. A aparência e a interpretação pelo cliente High Five exigem o teste ingame; a validação estática não substitui esse teste.

## Persistência e interrupções

Cada anúncio tem sua própria custódia, em `items.loc='AUCTION'`, com `loc_data` igual ao ID do anúncio. Pilhas de anúncios distintos nunca são agregadas na restauração. Itens e metadados são conferidos antes de disponibilizar o anúncio após restart.

Antes de cobrar ou transferir, o módulo registra uma reserva em `community_auction_operations` e deixa o anúncio BUSY. O registro inclui ator, objeto original, quantidade original, quantidade da operação, valor e saldo anterior. Etapas registram o objeto de destino e o ID do correio. O fechamento do anúncio e da operação usa uma transação InnoDB única.

As APIs nativas de inventário e correio têm conexões próprias e algumas capturam erros SQL internamente. Por isso, o AuctionHouse força as gravações e confere os dados persistidos antes de avançar. Isso não constitui uma transação SQL única entre inventário, dinheiro e correio.

Quando uma transferência retorna falha e o item original é comprovadamente intacto, a cobrança é compensada e a devolução da Adena é verificada. Uma falha ambígua coloca o anúncio em REVIEW, preserva o diário e impede repetição automática. Reservas BUSY de uma execução interrompida também ficam bloqueadas no próximo restart. Se a própria reserva tiver um resultado de commit incerto, o serviço inteiro para de aceitar operações até reiniciar e conferir os registros.

Esse é um bloqueio conservador contra duplicação; operações ambíguas precisam de reconciliação administrativa. Não há promessa de recuperação automática de toda queda entre gravações nativas. Não apague o diário nem altere um estado para ACTIVE sem conferir inventário, dinheiro e correio.

Consulta inicial de diagnóstico, somente leitura:

```sql
SELECT l.*, o.id AS operation_id, o.kind, o.actor_id, o.source_object_id,
       o.source_count, o.quantity AS operation_quantity, o.amount,
       o.adena_before, o.target_object_id, o.message_id, o.stage
FROM community_auction_listings l
LEFT JOIN community_auction_operations o ON o.listing_id = l.id
WHERE l.state IN ('BUSY', 'REVIEW')
ORDER BY l.id, o.created_at;
```

Use os IDs para conferir `items` (owner_id, object_id, count, loc, loc_data) e `messages`. Os correios incluem o número do anúncio e o identificador da operação. Não reenvie pagamento ou item com base apenas no nome de uma etapa: uma gravação pode ter sido concluída antes da interrupção de seu registro seguinte. Pare o servidor antes de qualquer correção manual de inventário.

A expiração é verificada a cada minuto, independentemente de visitas ao Community Board. Itens expirados são retirados da busca imediatamente; a devolução ocorre na próxima varredura. No desligamento, a tarefa para e novas operações são recusadas. O prazo e as regras de expiração do correio são os nativos da Journey: o jogador precisa retirar seus anexos dentro desse prazo.

## Validação realizada e testes ingame

Compilação completa do núcleo e dos handlers com Java 25; 30 testes específicos aprovados, executados isoladamente com banco/inventário/correio simulados. Cobertura funcional: taxa, limites, entradas malformadas, estoque desatualizado, restrições, permissão de cancelamento, confirmação única, reservas e rollback, compra parcial, compensação de falha, criação e expiração sem navegação, concorrência e bloqueio após interrupção.

Não foram executados SQL no banco real, inicialização do GameServer ou compras reais nesta implementação.

No servidor de teste, validar nesta ordem:

1. Abrir home/market, buscar por nome e ID, filtrar, ordenar e trocar páginas; conferir ícones e botões.
2. Publicar um material empilhável e uma arma/armadura com enchant/atributos; conferir taxa e quantidade.
3. Comprar parte da pilha com outro personagem; conferir entrega, estoque restante e pagamento por correio com vendedor online/offline.
4. Reiniciar com anúncios ativos; conferir identidade, quantidade e atributos, inclusive dois anúncios do mesmo material pelo mesmo vendedor.
5. Cancelar e retirar os anexos de retorno; tentar cancelar com outro personagem e repetir confirmações antigas.
6. Usar dois compradores para disputar a última quantidade do mesmo anúncio; somente a quantidade disponível deve ser cobrada e entregue.
7. Testar inventário cheio, pouco peso disponível, pouca Adena e acesso fora de paz/loja/trade.
8. Validar expiração com anúncio vencido no banco de teste e restart; não é necessário esperar um dia. Alterar expires_at somente com servidor desligado.

Não migrar diretamente anúncios legados das duas referências: sua custódia e seus estados não seguem este diário.

### Limitação dos testes existentes do projeto

A compilação completa do núcleo Java 25 passou. Ao ampliar a checagem para os testes antigos do Community Board, foram encontrados testes desatualizados que ainda referenciam `Config.SUBCLASS_MASTER_BOARD`, construtores e métodos antigos do DropSearch/DropInfoHandler. Esses testes não compilam contra as APIs atuais do workspace e não foram alterados nesta adaptação. Isso impede declarar a suíte global aprovada. Os testes novos do AuctionHouse são executados separadamente.

A regressão isolada `DropCalculatorCompatibilityTest` também apresentou uma falha independente no caso de alocação de imagens (`imageAllocationPreservesExistingMappingsAndNumericNpcIds`, esperado 1000000000, obtido 0). A mesma falha ocorre executando somente essa classe; ImagesCache e DropCalculator não foram alterados nesta tarefa.

## Remetente, avisos e logs

Novos correios de pagamento e devolução usam o remetente `Auction Manager`. O tipo próprio é persistido no campo já existente `messages.sendBySystem`, sem migração SQL, e preserva sua natureza de correio do sistema no servidor. Somente nos pacotes enviados ao cliente ele é apresentado com o tipo de nome comum, evitando um tipo de remetente que o High Five não conhece. Os tipos anteriores mantêm seus valores. Correios antigos não são convertidos retroativamente.

Após concluir uma compra, o comprador recebe a mensagem nativa de obtenção do item (com a quantidade comprada). Falhas no envio desse aviso não repetem a compra nem reabrem uma venda concluída. Logs de criação, venda e retorno mostram os nomes dos personagens; IDs dos anúncios e identificadores de operação continuam disponíveis para auditoria.

Estas mudanças ficam no núcleo: é necessário recompilar e atualizar o GameServer.jar e reiniciar. Não exigem atualização de HTML nem novo SQL.
