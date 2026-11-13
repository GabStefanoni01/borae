# Fluxos de tela e experiência

Este documento define comportamento e hierarquia funcional. Não fixa identidade visual final, cores ou layout pixel-perfect; isso deve ser validado em protótipos.

## 1. Aplicação e áreas

### Área pública
- Página pública do restaurante.
- Cardápio e detalhes de produtos.
- Descoberta pública (fase posterior).
- Visualização 3D/AR quando houver mídia válida.

### Área autenticada
- Login/cadastro.
- Seleção de restaurante quando o usuário tiver mais de um.
- Dashboard do restaurante selecionado.
- Perfil/configurações.
- Equipe e convites.
- Catálogo/cardápio.
- Configurações futuras de IA, fidelidade e mídia.

### Área administrativa Boraê
Futura e separada da área do restaurante, com autorização global própria.

## 2. Fluxo de autenticação

**Login**
1. Usuário informa e-mail e senha.
2. Frontend valida campos obrigatórios.
3. Backend autentica e devolve sessão/token conforme contrato atual.
4. Frontend mantém sessão e direciona para área autenticada.
5. Erro deve ser claro sem revelar se uma conta existe quando isso representar risco.

**Sessão expirada**
1. Requisição protegida recebe 401.
2. Frontend limpa a sessão local.
3. Redireciona para login.
4. Exibe aviso de sessão expirada.

**Critérios:** não renderizar dados privados sem sessão; não aceitar autorização baseada apenas em guard Angular.

## 3. Onboarding de restaurante

1. Usuário autenticado acessa área de restaurantes.
2. Escolhe criar restaurante.
3. Preenche os campos obrigatórios definidos pelo backend.
4. Backend cria restaurante e membership OWNER na mesma transação.
5. Frontend atualiza lista de restaurantes e seleciona o novo.
6. Próximo passo conduz ao preenchimento do perfil.

**Estados:** carregando, sucesso, validação por campo, erro de rede e erro de negócio.

## 4. Equipe e convites

### Convidar
1. OWNER abre Equipe.
2. Informa e-mail e papel MANAGER ou STAFF.
3. Backend valida permissão e duplicidade de convite.
4. Token é criado e exibido uma única vez para compartilhamento.
5. Interface informa validade e alerta para compartilhar apenas com o destinatário.

### Aceitar
1. Convidado autentica com conta cujo e-mail corresponda.
2. Informa/abre token.
3. Backend valida token, expiração, uso e e-mail.
4. Vínculo é criado ou reativado.
5. Restaurante passa a aparecer na lista do usuário.

### Gerenciar
- OWNER consulta, altera papel e remove membro.
- MANAGER consulta equipe conforme implementação atual.
- Ações não permitidas não devem aparecer como disponíveis, mas o backend também precisa rejeitá-las.

## 5. Perfil do restaurante (futuro)

Tela de edição com seções:
- Informações básicas: nome do restaurante, nome público/fantasia e descrição opcional.
- Endereço/localização: endereço obrigatório para estabelecimentos físicos e localização no mapa.
- Contato e links: telefone/WhatsApp, Instagram e site opcionais.
- Identidade visual: logo e capa opcionais.
- Categoria gastronômica: obrigatória.
- Horário de funcionamento: configuração por dia da semana.
- Identificador público: slug gerado automaticamente e editável se único e válido.
- Publicação/visibilidade: `DRAFT`, `PUBLISHED` ou `SUSPENDED`.

O proprietário pode começar a montar o cardápio sem preencher os campos opcionais. A tela deve mostrar prévia pública e indicar campos obrigatórios pendentes antes da publicação. D-01 aprovada em 13/11/2026.

## 6. Gestão de cardápio (futuro)

### Seleção e gestão de cardápios
- Um restaurante pode criar múltiplos cardápios, como Almoço, Jantar e Bebidas.
- Cada cardápio possui categorias e produtos próprios.
- Mais de um cardápio pode ficar publicado simultaneamente.
- A área autenticada permite listar, criar, editar, arquivar e abrir cada cardápio.
- No MVP, não alternar automaticamente o cardápio conforme horário.
- O QR Code principal abre a página pública do restaurante; nela, o consumidor escolhe qual cardápio consultar.

D-02 aprovada em 13/11/2026.

### Categorias
- Listar, criar, renomear, ordenar, ativar/desativar.
- Não apagar categoria com produtos sem regra de realocação/arquivamento definida.

### Produtos
- Listar com busca e filtro por categoria/status.
- Criar/editar dados, preço, imagem e disponibilidade.
- Na edição do produto, adicionar, editar, remover e ordenar ingredientes individualmente (ex.: pão, carne, queijo, cebola e bacon).\n- Cadastrar adicionais opcionais com acréscimo de preço (ex.: bacon, cheddar e ovo).\n- Definir quais ingredientes do produto poderão ser removidos pelo cliente (ex.: sem cebola ou sem tomate).
- Exibir a lista de ingredientes nos detalhes públicos do produto.

D-04 aprovada em 13/11/2026. O cadastro é livre e simples; não inclui formulário específico de alergênicos ou contaminação cruzada.\n\nD-05 aprovada em 13/11/2026. Adicionais são cadastrados com preço visível no cardápio. Remoções e ponto da carne, quando aplicável, são instruções informadas no momento do pedido e apresentadas ao garçom; não são opções pré-cadastradas no produto. Não haverá tamanhos ou combos nesta etapa.
- Arquivar sem perder histórico necessário.
- Pré-visualizar como consumidor.

### Publicação
- Publicação é controlada por cardápio, não individualmente por produto.
- Cardápio novo começa como `DRAFT` e não aparece publicamente.
- Antes de publicar, o restaurante pode revisar uma prévia.
- Publicar valida a consistência mínima e torna visíveis as categorias ativas e os produtos elegíveis.
- Em cardápio publicado, alterações salvas entram no ar imediatamente.
- O restaurante pode despublicar ou arquivar o cardápio sem apagar seus dados.
- Produto arquivado não aparece publicamente; produto indisponível pode continuar visível com indicação clara.
- Não há fluxo de aprovação/publicação por produto no MVP.

D-03 aprovada em 13/11/2026.

## 7. Cardápio público (futuro)

- Abrir por slug/URL pública.
- Mostrar nome, identidade visual, categorias e produtos publicados.
- Navegação por categoria e pesquisa simples quando houver volume.
- Detalhe do produto com imagem, descrição, preço, ingredientes e adicionais com seus preços.
- Indicar indisponibilidade com clareza.
- Se falhar mídia 3D, mostrar imagem normal.
- Não exigir login para consulta.

## 8. Assistente gastronômico (futuro)

- Disponível dentro do cardápio de um restaurante específico.
- Coletar intenção e filtros de forma conversacional.
- Mostrar recomendações com nome, preço e justificativa baseada em dados.
- Permitir abrir detalhe do item recomendado.
- Se não houver item compatível, dizer que não encontrou opções; não inventar.
- Mostrar aviso para confirmar alergênicos diretamente com o estabelecimento.

## 9. Estados transversais

Toda tela com dados remotos deve prever:
- Carregamento (sem duplicar ações).
- Erro recuperável e opção de tentar novamente.
- Estado vazio com orientação para próxima ação.
- Sucesso com feedback acessível.
- Sem permissão.
- Recurso não encontrado ou desativado.
- Layout mobile e teclado/acessibilidade.

## 10. Navegação e responsividade

- Mobile-first, principalmente no cardápio público.
- Área administrativa pode usar layout responsivo com navegação adaptada.
- Não depender exclusivamente de hover.
- Formulários devem preservar dados não sensíveis quando ocorrer erro recuperável.
- Evitar modais para fluxos longos; priorizar páginas ou painéis claros.


## 11. Conta e segurança do consumidor (D-13/D-17)

- Cadastro com nome, telefone, e-mail e senha.
- Verificar telefone por SMS antes de ativar a conta.
- Login por telefone + senha; não solicitar SMS em todo login.
- Recuperação de senha por SMS ou link de e-mail, com validade limitada e uso único.
- Troca de telefone exige verificação do novo número e preserva conta/carteiras/histórico.
- Novo login encerra a sessão anterior; somente um dispositivo/sessão ativo por vez.
- Após cinco tentativas consecutivas incorretas, bloquear por 15 minutos.
- Nome/e-mail editáveis; telefone/senha exigem validação de segurança.
- Sem foto de perfil; usar identidade visual padrão.
- Exibir notificações de segurança para novos logins, mudanças de credenciais/contato e atividade suspeita.
- Exclusão sob solicitação, com anonimização quando aplicável e retenção legal/auditável.

## 12. Descoberta e avaliações (D-16)

- Solicitar permissão de localização para usar posição atual como padrão; sempre oferecer busca manual de outra região.
- Permitir filtros combináveis e ordenação por relevância.
- Mostrar restaurantes abertos antes dos fechados e próxima abertura quando conhecida.
- Página de rede apresenta marca e unidades; abrir unidade para detalhes e avaliação específica.
- Avaliação só é habilitada após compra confirmada; nota geral, comentário opcional e critérios de atendimento, comida e ambiente.
- Restaurante pode responder, mas não editar/apagar avaliação.
- Favoritos e listas personalizadas podem ser compartilhados por link.
- Não oferecer mensagens internas, feed social ou listas colaborativas.

## 13. Assinatura do restaurante (D-18)

- Exibir plano único e periodicidades mensal, trimestral e anual.
- Apresentar 30 dias de teste gratuito.
- Ao final do trial, solicitar forma de pagamento e confirmação expressa antes de iniciar a cobrança.
- Exibir situação da assinatura, próxima renovação, histórico de cobranças e notas fiscais.
- Permitir cancelamento a qualquer momento, informando que o acesso permanece até o fim do período pago.
- Em falha de cobrança, informar tentativas e prazo de tolerância de sete dias.
- Alteração de periodicidade e reajustes entram em vigor na próxima renovação.

## 14. Administração global e suporte (D-19)

- Painel global separado dos painéis dos restaurantes.
- Exibir indicadores globais de restaurantes, consumidores, compras e receita; ocultar dados financeiros da equipe de suporte.
- Exibir ações administrativas com registro de auditoria.
- Suporte vê apenas dados técnicos e pode suspender restaurante temporariamente.
- Exclusão de restaurante exige confirmação e justificativa.
- Modo de manutenção deve informar aviso programado aos usuários.

## 15. Integrações e atendimento (D-20)

- Documentação Swagger/OpenAPI pública para API externa protegida por chaves e permissões.
- Interface para configurar webhooks e consultar entregas/tentativas; reenvio automático em falhas.
- Ambiente Sandbox separado de produção.
- Chat de suporte com IA e ação de encaminhar para atendimento humano.
- Notificações por e-mail.
- Não desenhar fluxo de pedido online nem integração POS/ERP no lançamento.
- Painel de restaurante e experiência do consumidor funcionam em web responsiva, sem apps nativos no lançamento.
