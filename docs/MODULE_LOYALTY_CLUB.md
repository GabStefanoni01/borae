# Fidelidade individual e Boraê Club

**Status:** regras centrais do MVP aprovadas em D-12, D-13, D-14 e D-15. Implementação ainda planejada.

## 1. Fidelidade individual por restaurante

Cada restaurante terá um programa próprio, com:
- Regras de acúmulo independentes.
- Carteiras e saldos independentes por consumidor.
- Recompensas e resgates próprios.
- Histórico/ledger isolado.
- Validade definida pelo restaurante.

Não existe transferência, conversão ou resgate de pontos entre restaurantes no MVP.

## 2. Modelos de acúmulo — D-12

O restaurante escolhe uma das modalidades:
1. **Proporcional ao valor gasto:** taxa por valor ou faixas configuradas pelo restaurante. O cálculo dentro da faixa é proporcional ao valor efetivamente pago.
2. **Pontos fixos por compra:** quantidade definida pelo restaurante, respeitando o valor mínimo de compra já estabelecido na decisão de produto anterior.

Regras de cálculo:
- Base: valor efetivamente pago após descontos e cupons.
- Excluir taxas de entrega, serviço e outros encargos.
- Frações são calculadas e o total final é arredondado para baixo (ex.: 37,9 = 37 pontos).
- Parâmetros e versões da regra devem ser preservados por crédito.

## 3. Consumidor e adesão — D-13/D-17

- Uma conta global por consumidor, identificada por telefone verificado via SMS.
- Cadastro exige nome, telefone verificado, e-mail e senha.
- Login por telefone + senha; SMS não é exigido em todo login.
- Consentimento explícito para participar da fidelidade.
- Consentimento para ofertas e promoções é separado e opcional.
- A mesma conta pode ter carteiras independentes em vários restaurantes.
- Troca de telefone exige verificação por SMS do novo número e preserva conta, carteiras e histórico.
- O consumidor não pode criar créditos para si próprio.

## 4. Confirmação da compra e crédito — D-13

- Pontos só ficam disponíveis após confirmação efetiva do pagamento.
- Quando houver integração confiável, a confirmação poderá ocorrer automaticamente; sem integração, funcionário autorizado confirma manualmente.
- Garçons e gerentes autorizados podem confirmar a compra no fechamento e informar o valor efetivamente pago.
- Cada confirmação registra restaurante, funcionário responsável e referência única da compra.
- O identificador único da compra é a proteção principal contra duplicidade; dados como consumidor, valor, restaurante e horário podem apoiar a detecção complementar.
- Não há integração com PDV/ERP no lançamento (D-20); a operação inicial usa confirmação manual.

## 5. Cancelamentos, estornos e compensação — D-12/D-15

- Estorno/cancelamento remove pontos ainda disponíveis que tenham sido originados pela compra.
- Se pontos já tiverem sido usados, registrar pendência de compensação, sem saldo negativo.
- Próximos acúmulos compensam primeiro a pendência; o excedente passa a compor o saldo disponível.
- Estorno pode ser automático quando recebido de integração confiável ou manual por funcionário autorizado.
- Toda operação é idempotente, auditável e vinculada ao evento/crédito original.
- Não apagar lançamentos; usar lançamentos compensatórios.

## 6. Validade, consumo e expiração — D-15

- Cada restaurante define o prazo de validade dos pontos.
- Cada crédito/lote preserva sua data de expiração e a versão da regra aplicável.
- Consumir pontos por FEFO (*first expired, first out*): vencimento mais próximo primeiro. Em empate, consumir o crédito mais antigo.
- A expiração é automática, remove o saldo disponível e cria registro no histórico ligado ao crédito original, com quantidade e data.

## 7. Avisos de expiração — D-15

- Enviar pelo menos uma notificação diária durante os 14 dias anteriores ao vencimento.
- Canais: push e central interna do app.
- Um aviso diário agrupa os lotes próximos do vencimento, mas preserva restaurante, quantidade e data de cada lote.
- Horário inteligente conforme comportamento e fuso do consumidor; usar horário padrão seguro quando não houver dados suficientes e evitar períodos de descanso.
- Avisos transacionais de expiração não podem ser desativados nas preferências de marketing.
- Push desativado: manter o aviso na central interna; não usar SMS alternativo.

## 8. Alterações de regra e encerramento

- Nova regra só afeta pontos acumulados após sua data/hora de vigência.
- Pontos existentes permanecem vinculados à versão e aos parâmetros originais.
- Recompensas já conquistadas não podem ser retiradas por alteração posterior.
- Encerramento voluntário exige comunicação e prazo mínimo de 60 dias para resgate; o restaurante pode oferecer prazo maior.
- Em fechamento repentino, comunicar consumidores assim que possível. O Boraê não assume automaticamente o custo das recompensas nem permite resgate em outro restaurante.

## 9. Antifraude e auditoria

- Não exigir confirmação adicional do consumidor a cada compra.
- Manter trilha auditável das confirmações, créditos, débitos, estornos, expirações e ajustes.
- Detectar padrões suspeitos e gerar alertas para análise do gerente; não haverá bloqueio automático no MVP.
- Toda movimentação deve possuir referência e idempotência.
- Saldo deve ser reconciliável com o ledger; não alterar saldo diretamente sem lançamento correspondente.

## 10. Boraê Club — visão futura, fora do MVP

O Boraê Club compartilhado entre restaurantes permanece como visão de longo prazo, mas não será implementado no MVP. Não criar carteira compartilhada, saldo Club, resgate cruzado, compensação ou liquidação entre restaurantes nesta etapa.

Uma retomada futura exigirá nova decisão sobre financiamento, valor dos pontos, responsabilidades por recompensas, compensação, liquidação, estornos, fraude e obrigações legais/contábeis.

## 11. Critérios de aceite antes do piloto

- Simular acúmulo, resgate, estorno, compensação, expiração e concorrência.
- Demonstrar que retries não duplicam lançamentos.
- Demonstrar que dois resgates concorrentes não gastam o mesmo saldo.
- Validar FEFO e desempate por crédito mais antigo.
- Reconciliar saldo com soma do ledger.
- Garantir que alteração de regra não afete créditos anteriores.
- Garantir que notificações diárias preservem os detalhes de cada lote.
- Separar completamente fidelidade individual e Boraê Club.
- Aprovar termos, contratos e responsabilidades antes de transacionar valor real.
