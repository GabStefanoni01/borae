import { HttpErrorResponse } from '@angular/common/http';

export type CatalogAction =
  | 'load-menus' | 'load-menu' | 'save-menu' | 'publish-menu' | 'unpublish-menu' | 'archive-menu'
  | 'save-category' | 'reorder-categories'
  | 'load-products' | 'save-product' | 'availability' | 'archive-product' | 'restore-product' | 'reorder-products'
  | 'upload-image' | 'remove-image'
  | 'publish-restaurant' | 'unpublish-restaurant' | 'qrcode'
  | 'load-model' | 'upload-model' | 'publish-model' | 'delete-model';

export const FORBIDDEN_MESSAGE = 'Você não tem permissão para realizar esta ação.';

/** Status HTTP do erro (0 = sem conexão; -1 = erro desconhecido). */
export function httpStatus(error: unknown): number {
  return error instanceof HttpErrorResponse ? error.status : -1;
}

/**
 * Mensagens amigáveis para os erros do catálogo.
 * O backend não devolve o texto do erro (server.error.include-message=never), então a mensagem
 * é escolhida pelo status HTTP e pela ação. Os textos de 409 descrevem as regras documentadas
 * nos services do backend (PR #22); o cliente não as reaplica como autoridade.
 */
export function catalogErrorMessage(error: unknown, action: CatalogAction): string {
  const status = httpStatus(error);
  const apiMessage = error instanceof HttpErrorResponse && typeof error.error?.message === 'string' && error.error.message.trim()
    ? error.error.message.trim()
    : null;
  if (status === 0) return 'Não foi possível conectar ao Boraê. Verifique sua conexão e tente novamente.';
  if (status === 401) return 'Sua sessão expirou. Entre novamente para continuar.';
  if (status === 403) return FORBIDDEN_MESSAGE;
  if (status === 429) return apiMessage ?? 'Muitas solicitações em sequência. Aguarde alguns instantes e tente novamente.';
  if (status === 404) {
    if (action === 'remove-image') return 'Este produto já não possui imagem.';
    if (action === 'publish-model' || action === 'delete-model') return 'Este produto não possui mais um modelo 3D.';
    return 'Este conteúdo não está mais disponível.';
  }
  if (status === 413) return action === 'upload-model' ? 'O modelo 3D pode ter no máximo 10 MB.' : 'A imagem do produto pode ter no máximo 10 MB.';
  if (status >= 500) {
    if (action === 'upload-image') return 'Não foi possível enviar a imagem. Confira o formato e o tamanho do arquivo.';
    if (action === 'qrcode') return 'Não foi possível gerar o QR Code agora. Tente novamente em instantes.';
    if (action === 'upload-model') return 'Não foi possível armazenar o modelo 3D agora. Tente novamente em instantes.';
    return 'O servidor encontrou um problema. Tente novamente em instantes.';
  }

  if (status === 409) {
    switch (action) {
      case 'publish-menu':
        return 'Para publicar, o cardápio precisa ter ao menos uma categoria ativa com um produto que não esteja arquivado.';
      case 'save-category':
        return 'A categoria já existe neste cardápio. Escolha outro nome.';
      case 'save-product':
      case 'availability':
      case 'upload-image':
      case 'upload-model':
      case 'publish-model':
      case 'delete-model':
      case 'load-model':
        return 'Este produto está arquivado ou o cardápio foi arquivado. Restaure o produto para alterá-lo.';
      case 'publish-restaurant':
      case 'unpublish-restaurant':
        return 'Este restaurante está suspenso e não pode ser alterado pelo painel.';
      default:
        return 'Este cardápio está arquivado e não pode mais ser alterado.';
    }
  }

  if (status === 400) {
    switch (action) {
      case 'publish-restaurant':
        return apiMessage ?? 'Para publicar o restaurante, preencha os campos obrigatórios do perfil (nome, endereço público, categoria gastronômica e endereço).';
      case 'save-product':
        return 'Confira as informações: nome obrigatório, preço não negativo, ingredientes e adicionais sem repetição.';
      case 'reorder-categories':
      case 'reorder-products':
        return 'A ordem não foi aceita porque a lista mudou. Atualizamos os dados; tente novamente.';
      case 'upload-image':
        return 'Não foi possível enviar a imagem. Confira o formato e o tamanho do arquivo.';
      case 'upload-model':
        return 'O arquivo não foi aceito. Envie um modelo GLB 2.0 válido, sem recursos externos, e dimensões maiores que zero.';
      default:
        return 'Confira as informações e tente novamente.';
    }
  }

  switch (action) {
    case 'load-menus': return 'Não foi possível carregar seus cardápios.';
    case 'upload-image': return 'Não foi possível enviar a imagem. Confira o formato e o tamanho do arquivo.';
    default: return 'Não foi possível concluir a operação. Tente novamente.';
  }
}
