# Especificação de módulos — Fidelidade e Boraê Club

Os dois programas são produtos diferentes e precisam de saldos, regras, ledgers e relatórios independentes.

## 1. Fidelidade individual do restaurante

### Objetivo
Permitir que cada restaurante crie um programa próprio de relacionamento com consumidores.

### Capacidades previstas
- Ativar/desativar programa.
- Definir regra de acúmulo.
- Definir recompensas e custo em pontos.
- Definir validade/expiração.
- Consultar saldo e histórico.
- Emitir e validar resgate.
- Registrar estorno e ajuste administrativo auditado.

### Regras invariantes
- Saldo pertence a uma conta no programa de um restaurante específico.
- Pontos não são transferíveis entre restaurantes.
- Cada evento de crédito/débito deve possuir origem e chave de idempotência.
- Consumidor não pode atribuir pontos a si mesmo.
- Resgate deve reservar/debitar saldo atomicamente.
- Estorno gera lançamento compensatório; não apaga o histórico.
- Alteração de regra não deve reescrever retroativamente lançamentos existentes.

### Regra aprovada parcialmente — D-12
Cada restaurante escolhe entre dois modelos: (1) pontos proporcionais ao valor gasto; ou (2) quantidade fixa de pontos por compra. O programa local mantém saldo, recompensas e ledger próprios, sem transferência para outros restaurantes ou para o Boraê Club.

### Decisões pendentes
- Parâmetros exatos de cada modelo (taxa por valor gasto ou quantidade fixa por compra).
- Valor monetário equivalente, se houver.
- Momento de concessão (compra confirmada, validação manual, POS).
- Prazo de expiração e ordem de consumo dos pontos.
- Política de cancelamento/estorno.
- Quem pode registrar compra e como comprovar a origem.
- Limites de ajuste manual e dupla aprovação.
- Regras fiscais/contratuais aplicáveis.

## 2. Boraê Club

### Objetivo
Programa opcional de benefícios compartilhados entre restaurantes parceiros, com uma experiência de consumidor única.

### Diferença essencial
- Fidelidade individual: pontos emitidos e resgatados no programa de um restaurante.
- Boraê Club: carteira de pontos compartilhada entre os restaurantes parceiros.

Os dois programas são independentes: um consumidor pode participar de ambos, mas os saldos, regras, recompensas e ledgers não se misturam e não há conversão ou transferência automática.

**Regra aprovada parcialmente — D-12:** o Boraê Club acumula 1 ponto por R$ 1,00 gasto em compra elegível, com a mesma proporção em todos os restaurantes parceiros. Exemplo: R$ 50,00 em compra elegível geram 50 pontos Club. A regra local de cada restaurante não altera essa proporção compartilhada.

### Capacidades previstas
- Consumidor adere voluntariamente aos termos do Club.
- Durante o atendimento/fechamento, o garçom pode perguntar se o consumidor já participa ou deseja aderir; a identificação e adesão dependem da ação e do consentimento explícito do consumidor, sem inscrição automática.
- O método de identificação (telefone, QR Code ou outro) e o fluxo detalhado de adesão ainda serão definidos.
- Restaurante solicita/aceita participação.
- Consumidor consulta saldo, parceiros e benefícios.
- Consumidor inicia resgate em parceiro.
- Plataforma registra obrigação do parceiro, consumo do benefício, estorno e liquidação.
- Operação administrativa acompanha conciliação e disputas.

### Controles obrigatórios
- Ledger append-only (corrigir por reversão).
- Idempotency key em toda operação financeira/pontuação.
- Transação atômica e proteção contra saldo negativo.
- Controle de concorrência em resgates simultâneos.
- Limites e antifraude por usuário, parceiro e período.
- Auditoria de ações administrativas.
- Reconciliação periódica entre ledger e obrigações dos parceiros.
- Termos e versão contratual aceitos pelo consumidor e parceiro.

### Decisões pendentes que bloqueiam implementação
- Confirmação confiável da compra e momento de crédito dos pontos (não há pedidos, pagamentos ou POS definidos no MVP atual).
- Tratamento de cancelamentos, devoluções e estornos.
- Expiração dos pontos e ordem de consumo.
- Quem financia pontos/benefícios.
- Se pontos são comprados, ganhos, convertidos ou promocionais.
- Valor e unidade de referência.
- Quem assume custo quando um ponto é resgatado em parceiro diferente.
- Fórmula de compensação e calendário de liquidação.
- Tratamento de estorno, cancelamento e fraude.
- Limites, expiração e validade.
- Implicações legais, fiscais, contábeis e de proteção ao consumidor.
- Se há assinatura B2C ou acesso gratuito.
- Como o restaurante entra/sai do programa e trata saldo pendente.

## 3. Modelo de transação recomendado

Não usar apenas uma coluna `balance` editável como registro contábil. Usar ledger imutável com lançamentos de crédito, débito, reversão, expiração e ajuste. Uma projeção de saldo pode existir para leitura rápida, mas deve ser reconciliável com o ledger.

## 4. Critérios de aceite antes do piloto

- Simular acúmulo, resgate, estorno, expiração e concorrência.
- Demonstrar que retries não duplicam lançamento.
- Demonstrar que dois resgates concorrentes não gastam o mesmo saldo.
- Reconciliar saldo com soma do ledger.
- Separar completamente fidelidade individual e Club.
- Aprovar termos, contratos, responsabilidade financeira e suporte antes de transacionar valor real.
