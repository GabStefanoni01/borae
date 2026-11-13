# Especificação de módulo — Experiência 3D e realidade aumentada

**Status:** planejado; depende do catálogo e de infraestrutura de mídia.

## 1. Objetivo

Permitir que o restaurante associe representação tridimensional aos produtos, para que o consumidor visualize o prato em 3D e, em dispositivos compatíveis, em realidade aumentada.

## 2. Fluxo do restaurante

1. Seleciona um produto cadastrado.
2. Envia arquivo GLB/glTF aceito.
3. Backend valida arquivo e registra MediaAsset.
4. Restaurante associa modelo ao produto.
5. Informa ou confirma dimensões físicas para calibração.
6. Visualiza prévia em desktop/mobile.
7. Publica modelo após validação.

## 3. Fluxo do consumidor

1. Abre detalhe do produto.
2. Se houver modelo válido, carrega visualizador 3D.
3. Pode rotacionar/zoom conforme capacidades do componente.
4. Se navegador/dispositivo suportar AR, apresenta ação para abrir experiência.
5. Se não suportar, mantém visualização 3D ou imagem convencional.

## 4. Regras técnicas

- Preferir GLB para distribuição compacta; validar glTF e dependências quando aplicável.
- Limitar tamanho, polígonos, texturas e dimensões para evitar travamentos.
- Processar/validar arquivo em ambiente isolado; não confiar em extensão.
- Guardar arquivo em serviço externo de object storage; PostgreSQL guarda somente metadados e referência.
- Formatos aceitos: GLB e glTF; validar MIME real, integridade e dependências.
- Usar CDN/URLs assinadas conforme política de acesso.
- Cloudinary é candidato para avaliação de imagens, mas o provedor definitivo precisa comprovar suporte e adequação a arquivos 3D, além de custos e limites compatíveis.
- Limites numéricos de tamanho, polígonos e texturas precisam ser definidos antes de habilitar upload em produção.
- Não permitir que um restaurante associe mídia de outro.
- Implementar carregamento sob demanda e poster/imagem fallback.
- Medir tamanho e latência do asset.

## 5. Escala e precisão

Não declarar escala real 1:1 apenas com base no modelo visual. Registrar dimensões calibradas e método de medição. Exibir aviso de que a escala pode variar por dispositivo quando aplicável.

## 6. Critérios de aceite
- Arquivo inválido ou excessivo é rejeitado com mensagem clara.
- Modelo não publicado não aparece ao consumidor.
- 3D funciona sem AR.
- Dispositivo sem AR recebe fallback funcional.
- Erro no asset não impede visualizar nome, preço, descrição e imagem.
- Acesso multi-tenant é validado.
- Modelo é carregado sob demanda para não penalizar o menu inteiro.
