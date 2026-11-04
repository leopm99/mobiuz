# Análise funcional do material Mobius do fórum

Data: 03/11/2026.

Material: `C:/Users/llean/Pictures/Mobius Source Files MmoDev Forum`.
Projeto comparado: `C:/Users/llean/git/mobiuz`.

## Escopo e conclusão

Há conteúdo aproveitável. As melhores oportunidades identificadas são ferramentas de edição de datapack, alterações pontuais de comportamento de NPCs/summons e ferramentas de diagnóstico de geodata. A recomendação é extrair funcionalidades isoladas, mantendo os sistemas personalizados da Journey.

Conforme solicitado, diferenças de pacote, nomes de classes, divisão de Config e organização dos handlers não foram consideradas benefícios. A presença de uma classe com nome diferente tampouco foi tratada como funcionalidade ausente: por exemplo, a Journey já trata Knoriks em `LairOfAntharas.java`.

Foi feito inventário dos fontes e leitura direcionada dos candidatos e de seus equivalentes. Não é uma revisão linha a linha de todos os arquivos. Foram encontrados 2.191 arquivos Java no core e 1.514 scripts Java no datapack externo. O build declara source/target Java 25; as versões de HikariCP, MySQL Connector e SLF4J da pasta são iguais às presentes na Journey. Não foi verificada a origem comercial ou a condição de versão paga.

Nenhum programa desse material, build, launcher, servidor ou SQL foi executado. Não foi incorporado código externo à Journey. Este documento registra a análise; o inventário intermediário ficou em `build/mobius-material-comparison.json`.

## 1. Editor de drops: melhor primeiro candidato

Origem: `java/org/l2jmobius/tools/editor/DropEditor.java` e `tools/editor/drop/`.

A implementação oferece edição de drops e spoil, grupos e itens, resolução de nomes a partir dos XMLs de itens, validação de referências, quantidades e probabilidades. `NpcXmlEditor.saveDrops`, linha 133, compara o SHA-256 do arquivo carregado com a versão atual antes de salvar. Isso ajuda a evitar sobrescrever uma alteração feita no Eclipse enquanto o editor permanece aberto.

O método modifica o bloco de drops do NPC selecionado, preserva o restante do documento, cria backup e grava por arquivo temporário com tentativa de `ATOMIC_MOVE`. Há fallback para substituição normal quando o sistema de arquivos não oferece movimento atômico. O texto resultante é novamente analisado como XML.

Compatibilidade favorável: o detector reconhece `dist/game/data`, `game/data` e `data`; as pastas `stats/npcs` e `stats/items` correspondem às da Journey. O perfil incluído contempla drop/spoil e probabilidades percentuais. A lógica usa JDK/Swing e classes internas da própria ferramenta; não identifiquei imports de bibliotecas externas nesse conjunto.

Antes de adotar: validar round-trip em cópias representativas dos XMLs da Journey, atributos personalizados, comentários, grupos, quantidades long e IDs desconhecidos. A validação do editor de drops combina regras próprias e parsing XML; não deve ser confundida com validação completa do datapack contra os XSDs. O próprio código valida item chance <=100, mas o group chance recebe apenas a checagem numérica não negativa; esse limite merece revisão antes de uso.

Esforço estimado: baixo a médio para adaptação isolada, sem necessidade de alterar o funcionamento do servidor ou os HTMLs do community.

## 2. Editor de NPCs

Origem: `java/org/l2jmobius/tools/editor/NpcEditor.java` e `tools/editor/npc/`.

`NpcDefinitionXmlEditor`, linhas 101 e 129 em diante, detecta alteração externa pelo hash, valida os campos e a estrutura, produz um candidato e o valida contra o schema de NPCs antes de salvar. Também cria backup, preserva BOM e usa gravação por arquivo temporário.

Utilidade: manutenção de atributos, skills e dados estruturados de NPCs com menos edição manual repetitiva. A implementação permite partir do schema do datapack selecionado, o que é melhor do que presumir compatibilidade de todos os atributos.

Antes de adotar: verificar a cobertura dos campos personalizados da Journey e o comportamento com schemas/imports locais e definições de NPC que não seguem o perfil esperado pelo editor. Fazer os primeiros salvamentos em cópia do datapack. Não foi executado um teste de abertura/gravação real nesta análise.

Esforço estimado: médio; prioridade logo após o editor de drops.

## 3. Summons: diferença útil, mas material também mantém a falha de skills

Na Journey, `ai/SummonAI.java:52` rejeita a intenção de ataque quando findPath retorna null. No material externo, o fluxo de ataque de SummonAI não tem essa mesma pré-condição; usa a sequência de intenções e guarda de alvo de sua própria IA. O ataque básico em `entity/actor/Summon.java:1495` verifica `GeoEngine.canSeeTarget`, enquanto a Journey verifica `GeoData.canMove` no ponto correspondente.

Isso é uma referência útil para separar visibilidade e cálculo de rota, em linha com o problema observado nos testes locais sem geodata. Não se deve copiar a classe completa: APIs de intenção, instância, casting e agendamento de ataque diferem.

Limitação concreta: o material externo ainda possui, em `entity/actor/Summon.java:954`, a checagem de findPath null para skill física, acompanhada de CANNOT_SEE_TARGET. Portanto ele não constitui uma correção integral do problema de pet/summon que investigamos.

A Journey local passou a funcionar com PathFinding=0 sem geodata, conforme teste do usuário. Qualquer alteração permanente deve ser motivada por um cenário reproduzido e verificada também com geodata ativa.

Esforço estimado: médio para uma correção pontual com testes, alto para substituir a IA inteira. Não alterado nesta análise.

## 4. Comportamentos de NPCs de Antharas Lair e Dragon Valley

O material inclui scripts específicos para Knoriks, DragonGuards, DragonKnight, BloodyKarik, DragonTrackerOfTheValley e outros. Knoriks, por exemplo, inclui gerenciamento por cursos/rotas, reaparecimento periódico, fase invisível, retorno por distância, habilidades e remoção de efeitos de controle.

A Journey já tem lógica para o mesmo NPC e outros monstros em `dist/game/data/scripts/ai/areas/DragonValley/LairOfAntharas.java`, além de spawns estáticos em `data/spawns/Others/24_21.xml`. Por isso o benefício seria enriquecer/corrigir comportamentos específicos, não adicionar scripts duplicados. Copiar o Knoriks externo sem reconciliar spawns, rotas e registros de eventos pode duplicar monstros ou disputar a IA existente.

A descrição nos comentários de ser comportamento retail ainda exige confirmação independente; não foi tomada como prova histórica. O mesmo vale para o LastImperialTomb/Frintezza e novas definições de instância.

Esforço estimado: médio a alto por conjunto, com teste ingame de spawn, combate, morte, respawn, timers e reload. Prioridade conforme a área que se deseja trabalhar.

## 5. Ferramentas de extração e comparação de geodata

Origem: `tools/MapExtractor.java` e `tools/map/`.

Há leitura de pacotes Unreal, terreno, meshes, geração de grades e gravação de geodata, além de `GeoVerifier.verify` e `geoCompare` para comparação com região de referência. A ferramenta é separável do servidor e não apresenta imports de bibliotecas externas ao JDK/projeto no conjunto examinado.

Potencial: investigar diferenças de terreno, alturas e regiões que gerem problemas de movimentação ou visibilidade. É mais interessante inicialmente como ferramenta de diagnóstico do que como substituição da geodata atual.

Limitações: exigência de dados de cliente compatíveis, custo de processamento e diferenças no motor geográfico. Parte da comparação do verificador considera apenas a camada 0, conforme comentário em GeoVerifier:147; isso não certifica sozinha mapas com múltiplos andares. A Journey usa geoengine.jar e APIs próprias, enquanto o material fornece outro motor em fonte. Não foi demonstrada intercambialidade dos resultados com nosso jar.

Esforço estimado: médio para avaliar a ferramenta em região isolada; alto para trocar o motor ou adotar um conjunto completo de geodata.

## 6. Segurança por PIN: funcionalidade opcional real

Origem: `gameserver/managers/PinSecurityManager.java`, configurações, HTMLs, tabela pin_security e hooks no login/recebimento de pacotes/bypass.

A implementação contempla hash PBKDF2, salt, registro de tentativas e bloqueio temporário. Vai além de adicionar uma janela: impede ações enquanto a sessão permanece bloqueada.

Pode ser útil se houver interesse em uma camada adicional de proteção de contas/personagens. A adoção exigiria conferir todos os caminhos de pacotes, recuperação administrativa, concorrência e persistência dos bloqueios. A existência dos mecanismos não equivale a uma auditoria de segurança concluída. Não foi encontrado um equivalente de mesmo nome na Journey; a comparação de todas as proteções existentes ainda seria necessária antes de decidir.

Esforço estimado: médio a alto; recurso opcional, sem prioridade sobre estabilidade das compras e do gameplay.

## 7. Logs assíncronos: ideia útil com ressalvas concretas

Origem: `log/handler/AsyncFileHandler.java`.

O handler envia escrita em disco para uma thread dedicada e utiliza fila limitada a 8.192 registros. Isso pode reduzir espera por I/O nas threads do jogo, mas não há benchmark comparativo nesta análise.

Dois cuidados impedem adoção direta: `offer` descarta registros quando a fila está cheia; o formatter é chamado nas threads produtoras, então os formatadores da Journey que usam SimpleDateFormat compartilhado precisariam ser protegidos contra concorrência. O encerramento também usa espera limitada para drenagem.

Não aplicar diretamente a logs de itens, GM/auditoria ou compras, onde perda de registros prejudica reconciliação. Caso seja necessário otimizar I/O, adaptar a ideia com política explícita de perda, flush/encerramento e testes de carga, sem trocar globalmente os handlers atuais.

## O que não priorizar

- Renomear pacotes, classes ou dividir Config: fora do objetivo do usuário.
- PaperdollCache e troca completa do agendamento de ataques: acoplamento amplo e ganho de desempenho ainda não medido; comparar a implementação funcional existente antes de assumir novidade.
- Importação em massa de scripts, XMLs, HTMLs, configurações ou banco: não há demonstração de compatibilidade integral com os sistemas personalizados da Journey.
- Substituir doações, votos, ImagesCache ou DropCalculator: não foram encontradas implementações com esses identificadores no conjunto Java examinado que justificassem substituir as adaptações já validadas.
- Usar o README/checklist de patch notes ou a atribuição de versão paga como prova de comportamento correto.

## Ordem proposta

1. Adaptar o editor de drops como ferramenta independente e validar em uma cópia dos dados.
2. Fazer o mesmo com o editor de NPCs.
3. Selecionar correções pontuais de gameplay com cenário reproduzível; começar por summons/geodata somente se o teste com geodata ativa justificar.
4. Revisar uma área de NPCs por vez, conciliando seus dados e spawns.
5. Avaliar ferramentas de mapas e PIN conforme necessidade. Tratar logging/cache como otimização posterior baseada em medição.
