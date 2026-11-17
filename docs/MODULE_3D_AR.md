# Especificação de módulo — Experiência 3D e realidade aumentada

**Status:** backend MVP implementado na branch da PR #27. Testes PostgreSQL reais com Testcontainers passaram no Backend CI; Docker CI do último commit ainda está em execução. Homologação ImageKit e frontend (painel Angular, visualizador público e AR) ainda não foram concluídos.

## 1. Objetivo

Permitir que o restaurante associe representação tridimensional aos produtos, para que o consumidor visualize o prato em 3D e, em dispositivos compatíveis, em realidade aumentada.

## 2. Fluxo do restaurante

1. Seleciona um produto cadastrado.
2. Envia um arquivo GLB 2.0.
3. Backend valida a estrutura do arquivo e registra os metadados.
4. Restaurante associa o modelo ao produto existente.
5. Informa largura, altura e profundidade físicas em milímetros.
6. Visualiza prévia no painel (etapa de frontend pendente).
7. Publica o modelo após conferência.

## 3. Fluxo do consumidor

1. Abre o detalhe do produto.
2. Se houver modelo publicado, carrega o visualizador 3D sob demanda (frontend pendente).
3. Pode rotacionar/zoom conforme capacidades do componente.
4. Se navegador/dispositivo suportar AR, apresenta ação para abrir experiência (frontend pendente).
5. Se não suportar AR, mantém visualização 3D ou imagem convencional.

## 4. Regras técnicas

- O formato aceito no backend MVP é GLB 2.0; glTF JSON separado não é aceito nesta etapa.
- O backend valida assinatura, versão, comprimento, chunks e JSON principal do GLB.
- Referências externas em `buffers` e URLs de imagem são rejeitadas; imagens embutidas por data URI são aceitas.
- A validação estrutural não substitui inspeção de geometria, polígonos, texturas ou análise antimalware. Esses controles ainda precisam ser definidos antes de produção.
- PostgreSQL guarda metadados e referência do arquivo; o binário é armazenado no ImageKit.
- ImageKit é o provedor oficial de mídia do Boraê nesta implementação. Não utilizar Cloudflare R2 nem migrar imagens existentes.
- O limite de upload de 10 MB é provisório e acompanha a configuração multipart atual do backend; precisa ser homologado com modelos reais e limites da conta ImageKit.
- O arquivo recebe MIME `model/gltf-binary` e é enviado ao ImageKit como `non-image`; upload e entrega reais ainda precisam ser homologados.
- OWNER e MANAGER podem enviar, substituir, publicar/despublicar e remover modelos; STAFF não administra modelos 3D.
- Cada produto pode ter um modelo associado por vez.
- A URL pública do modelo só é incluída na resposta pública quando o modelo está publicado e o restaurante, cardápio, categoria e produto estão elegíveis.
- A imagem convencional do produto permanece intacta e deve ser usada como fallback.
- Assets devem ser carregados sob demanda no frontend para não penalizar o carregamento inicial do cardápio.

## 5. Escala e precisão

O restaurante informa largura, altura e profundidade em milímetros. Esses valores são metadados de calibração fornecidos pelo operador; não comprovam que o arquivo 3D tenha escala real 1:1. A interface deverá explicar que a escala percebida pode variar conforme dispositivo e ambiente.

## 6. Critérios de aceite

- [x] Backend rejeita arquivos que não atendem à estrutura básica GLB 2.0.
- [x] Backend rejeita referências externas em buffers e imagens.
- [x] OWNER/MANAGER podem gerenciar o modelo; STAFF é bloqueado.
- [x] API pública só inclui modelo publicado de produto elegível.
- [ ] Upload e entrega GLB homologados com a conta ImageKit.
- [x] Testes de integração com PostgreSQL concluídos no CI com Testcontainers (6 testes, sem falhas ou skips). Falhas reais do storage ImageKit ainda dependem de homologação.
- [ ] Visualizador 3D e AR progressiva implementados no frontend.
- [ ] Imagem convencional permanece como fallback em falhas/incompatibilidade.
- [ ] Testes em dispositivos móveis representativos e acessibilidade concluídos.
