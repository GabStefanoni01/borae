import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './shared/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />'
})
export class App {
  // Inicia o serviço de tema (aplica html[data-theme] e acompanha o tema do sistema).
  private readonly theme = inject(ThemeService);
}
