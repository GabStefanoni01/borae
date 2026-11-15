import { HttpErrorResponse } from '@angular/common/http';
import { FORBIDDEN_MESSAGE, catalogErrorMessage } from './catalog-errors';

const err = (status: number) => new HttpErrorResponse({ status });

describe('catalogErrorMessage', () => {
  it('403 sempre informa falta de permissão', () => {
    expect(catalogErrorMessage(err(403), 'save-product')).toBe(FORBIDDEN_MESSAGE);
    expect(catalogErrorMessage(err(403), 'qrcode')).toBe('Você não tem permissão para realizar esta ação.');
  });

  it('409 explica a regra conforme a ação', () => {
    expect(catalogErrorMessage(err(409), 'save-category')).toBe('A categoria já existe neste cardápio. Escolha outro nome.');
    expect(catalogErrorMessage(err(409), 'publish-menu')).toContain('ao menos uma categoria ativa');
    expect(catalogErrorMessage(err(409), 'availability')).toContain('arquivado');
    expect(catalogErrorMessage(err(409), 'publish-restaurant')).toContain('suspenso');
  });

  it('400, 404, 413, conexão e erro interno', () => {
    expect(catalogErrorMessage(err(400), 'save-menu')).toBe('Confira as informações e tente novamente.');
    expect(catalogErrorMessage(err(404), 'load-menu')).toBe('Este conteúdo não está mais disponível.');
    expect(catalogErrorMessage(err(413), 'upload-image')).toContain('10 MB');
    expect(catalogErrorMessage(err(400), 'upload-image')).toBe('Não foi possível enviar a imagem. Confira o formato e o tamanho do arquivo.');
    expect(catalogErrorMessage(err(0), 'load-menus')).toContain('conectar');
    expect(catalogErrorMessage(err(500), 'save-menu')).toContain('servidor');
  });

  it('nunca expõe detalhes técnicos do servidor', () => {
    const detailed = new HttpErrorResponse({ status: 500, error: { message: 'NullPointerException at MenuService' } });
    expect(catalogErrorMessage(detailed, 'save-menu')).not.toContain('Exception');
  });
});
