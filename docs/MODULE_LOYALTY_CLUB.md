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

### Decisão parcialmente aprovada — D-13: confirmação da compra
- Os pontos só podem ser gerados após a compra estar efetivamente concluída.
- No fluxo inicial, a confirmação é manual durante o fechamento da conta, sem exigir integração POS ou pagamento processado pelo Boraê.
- Garçons e gerentes podem confirmar a compra e registrar o valor efetivamente pago.
- A operação deve registrar o funcionário responsável e o restaurante, com trilha de auditoria.
- O consumidor não pode criar créditos para si mesmo; consulta ao cardápio ou simples pedido não gera pontos.
- O consumidor será localizado pelo telefone cadastrado; o método de verificação desse telefone ainda será definido.
- Permanecem pendentes o momento de disponibilidade dos pontos, a idempotência detalhada e o tratamento de cancelamentos/estornos.

### Decisões pendentes
- Parâmetros exatos de cada modelo (taxa por valor gasto ou quantidade fixa por compra).
- Valor monetário equivalente, se houver.
- Momento exato de disponibilidade do crédito após a confirmação da compra.
- Prazo de expiração e ordem de consumo dos pontos.
- Política de cancelamento/estorno.
- Controles antifraude, limites de confirmação e comprovação complementar da origem da compra.
- Limites de ajuste manual e dupla aprovação.
- Regras fiscais/contratuais aplicáveis.

## 2. Boraê Club — evolução futura, fora do MVP

Conforme D-14, o Boraê Club compartilhado entre restaurantes parceiros não será implementado nesta primeira versão. O MVP terá somente programas individuais: cada restaurante define suas recompensas e assume o custo dos benefícios que oferece. Os pontos não circulam entre estabelecimentos.

O Club poderá ser reavaliado depois da validação do produto e da adesão dos restaurantes. Antes de qualquer implementação, será necessário definir um modelo sustentável de financiamento, valor dos pontos, compensação entre parceiros, liquidação, estornos, fraude, termos e responsabilidades legais/contábeis. Não criar carteira compartilhada, saldo Club, resgate cruzado ou obrigações financeiras entre parceiros no MVP.

As ideias anteriores de acúmulo uniforme e carteira compartilhada são apenas hipóteses históricas de produto, não requisitos aprovados para implementação.

## 3. Modelo de transação recomendado

Não usar apenas uma coluna `balance` editável como registro contábil. Usar ledger imutável com lançamentos de crédito, débito, reversão, expiração e ajuste. Uma projeção de saldo pode existir para leitura rápida, mas deve ser reconciliável com o ledger.

## 4. Critérios de aceite antes do piloto

- Simular acúmulo, resgate, estorno, expiração e concorrência.
- Demonstrar que retries não duplicam lançamento.
- Demonstrar que dois resgates concorrentes não gastam o mesmo saldo.
- Reconciliar saldo com soma do ledger.
- Separar completamente fidelidade individual e Club.
- Aprovar termos, contratos, responsabilidade financeira e suporte antes de transacionar valor real.
