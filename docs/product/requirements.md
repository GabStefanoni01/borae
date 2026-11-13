# Requisitos funcionais e critérios de aceite

Este documento descreve o comportamento esperado do produto. A existência de um requisito aqui não significa que ele já esteja implementado. Consulte o [roadmap](../roadmap.md) para o status.

## 1. Perfis de usuário

### Consumidor
- Descobre restaurantes e consulta cardápios públicos.
- Visualiza pratos, opções, ingredientes declarados e preços.
- Usa o assistente para receber recomendações baseadas no cardápio.
- Participa de programas de fidelidade e benefícios para os quais for elegível.

### Proprietário (OWNER)
- Cadastra um restaurante e se torna seu proprietário inicial.
- Gerencia equipe, configurações e, futuramente, cardápio, campanhas e fidelidade.
- É o único papel que pode convidar membros, alterar papéis e remover membros na implementação atual.

### Gerente (MANAGER)
- Na implementação atual, pode consultar a equipe.
- No futuro, poderá receber permissões operacionais específicas, que devem ser definidas por módulo. Não presumir acesso total.

### Funcionário (STAFF)
- Papel de equipe com acesso operacional futuro definido por permissões específicas.
- Não presumir permissões administrativas.

### Administrador Boraê
- Papel global futuro, separado dos papéis de restaurante.
- Deve ter permissões explícitas, trilha de auditoria e acesso mínimo necessário.

## 2. Identidade e autenticação — implementado

- Cadastro com nome, e-mail e senha.
- Login por e-mail e senha.
- Senha armazenada com BCrypt.
- Emissão de access token JWT.
- Consulta do perfil autenticado.
- Listagem dos restaurantes em que o usuário possui vínculo ativo.
- Frontend persiste a sessão e envia o token nas chamadas protegidas.

Critérios:
- E-mail duplicado não pode criar uma segunda conta.
- Credenciais inválidas não autenticam.
- Rotas protegidas exigem token válido.
- Usuário inativo não deve acessar recursos protegidos.
- Frontend deve encerrar a sessão diante de resposta 401 em requisição protegida.

Limites atuais: refresh token, verificação de e-mail e recuperação de senha ainda não implementados.

## 3. Cadastro de restaurante — implementado

- Usuário autenticado informa nome e descrição opcional.
- Backend cria o restaurante e o vínculo OWNER na mesma transação.
- Slug é gerado pelo servidor e deve ser único.
- Usuário e papel não podem ser recebidos como autoridade do cliente.

Critérios:
- Não criar restaurante sem usuário autenticado.
- Não aceitar OWNER ou userId enviados pelo cliente como autoridade.
- Falha ao criar o vínculo deve reverter também a criação do restaurante.
- O proprietário deve aparecer na lista de restaurantes do usuário.

## 4. Equipe e convites — implementado na fundação atual

- OWNER convida um e-mail com papel MANAGER ou STAFF.
- Token aleatório é gerado e devolvido na resposta para compartilhamento manual.
- Apenas o hash do token é persistido.
- Convite expira em sete dias.
- Usuário autenticado aceita o convite com o token; o e-mail da conta deve corresponder ao e-mail convidado.
- OWNER e MANAGER podem listar membros.
- Somente OWNER altera papel ou remove membro.
- OWNER não pode ser convidado, rebaixado ou removido por esse fluxo.
- Remoção desativa o vínculo em vez de apagar o histórico.
- Vínculo anteriormente desativado pode ser reativado ao aceitar novo convite.

Limites atuais:
- Não há envio automático de e-mail.
- Não há interface de gestão de convites pendentes/resend/cancel.
- Não há rotina documentada de limpeza de convites expirados; a expiração é validada na aceitação.
- MANAGER ainda não tem permissões de catálogo ou operação implementadas.

Critérios:
- Token inválido, expirado ou já aceito deve ser rejeitado.
- Conta com e-mail diferente do convite não pode aceitá-lo.
- Convite não pode conceder OWNER.
- Usuário sem papel suficiente não pode alterar/remover membros.
- Operações precisam ser limitadas ao restaurante da URL e ao vínculo do usuário autenticado.

## 5. Cardápio digital — planejado

Cada restaurante poderá gerenciar:
- Categorias ordenadas e ativáveis/desativáveis.
- Itens com nome, descrição, preço, imagens e disponibilidade.
- Ingredientes e informações de alergênicos declaradas pelo estabelecimento.
- Variações, tamanhos, adicionais e observações.
- Identidade visual e informações públicas do restaurante.
- QR Code que abre o cardápio público; QR por mesa poderá ser posterior.

Critérios de aceite:
- Cliente não autenticado consegue consultar somente cardápios publicados.
- Itens indisponíveis são claramente identificados e não recomendados como disponíveis.
- Preço e disponibilidade são sempre obtidos do backend.
- Alterações administrativas exigem papel/permissão adequada e escopo do restaurante.
- Um restaurante não consegue alterar categorias ou itens de outro.

## 6. Visualização 3D e realidade aumentada — planejado

- Restaurante associa modelo GLB/glTF a um item do cardápio.
- Interface permite visualizar o modelo 3D.
- AR aparece apenas quando navegador/dispositivo suportar.
- Fallback visual deve existir quando AR não for suportada.
- Dimensões e escala devem ser calibradas; não afirmar escala real sem medição confiável.

Critérios:
- Validar tipo e tamanho de arquivo no upload.
- Guardar arquivo em object storage; banco armazena metadados e referência.
- Não aceitar modelo associado a item de outro restaurante.
- Página continua funcional sem suporte a AR.

## 7. Assistente gastronômico — planejado

Entrada possível:
- Tipo de refeição ou vontade do consumidor.
- Faixa de preço/orçamento.
- Preferências e itens que deseja evitar.
- Restrições informadas pelo consumidor.

Comportamento:
- Backend fornece ao assistente somente os itens relevantes do cardápio e dados necessários.
- Assistente recomenda somente itens retornados pelo backend.
- Backend valida novamente existência, preço e disponibilidade antes de apresentar o resultado.
- Explica de forma simples por que recomendou cada opção.

Segurança alimentar:
- Ingredientes e alergênicos precisam ser cadastrados pelo restaurante.
- Nunca garantir ausência de contaminação cruzada.
- Nunca substituir confirmação direta com o estabelecimento.
- Não inferir que um prato é seguro com base em informação ausente.

Limites:
- IA não acessa diretamente o banco.
- IA não altera cardápio/preço/estoque.
- IA não cria pedido, cobra, concede pontos ou resgata benefícios.

## 8. Fidelidade individual — planejado

Cada restaurante poderá configurar seu próprio programa:
- Regra de acúmulo.
- Recompensas e critérios de resgate.
- Campanhas e validade dos pontos.
- Histórico de movimentações.

Regras obrigatórias antes de produção:
- Saldo deve ser derivado de lançamentos rastreáveis (ledger).
- Emissão e resgate devem ser transacionais e idempotentes.
- Registrar origem, restaurante, consumidor, motivo e referência.
- Definir expiração, estorno, cancelamento, limites e prevenção a fraude.
- Carteira individual do restaurante não se confunde com pontos Boraê Club.

## 9. Boraê Club — planejado

- Rede compartilhada de benefícios entre restaurantes parceiros.
- Participação opcional por estabelecimento.
- Carteira/saldo compartilhado separado dos pontos individuais.
- Regras transparentes de acúmulo, resgate, financiamento e compensação entre parceiros.

Não implementar resgate com valor real antes de definir:
- Quem financia o benefício.
- Como o restaurante é reembolsado/compensado.
- Taxas, limites e validade.
- Tratamento de estorno e fraude.
- Conciliação e trilha de auditoria.

## 10. Descoberta de restaurantes — planejado

- Diretório com nome, localização, categorias e benefícios.
- Filtros por localização e tipo de culinária.
- Página pública do estabelecimento.
- Favoritos e avaliações podem ser etapas posteriores, com moderação e prevenção a abuso.

## 11. Pedidos, pagamentos e integrações — fora do MVP inicial

Pedidos completos, checkout, pagamento, integração com PDV, status em tempo real, notificações e aplicativo nativo não devem ser iniciados sem priorização explícita.
