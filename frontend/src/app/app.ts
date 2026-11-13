import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
    <main class="shell">
      <nav class="topbar" aria-label="Navegação principal">
        <a class="brand" href="/" aria-label="Boraê início">bor<span>aê</span></a>
        <span class="status">EM CONSTRUÇÃO</span>
      </nav>
      <section class="hero">
        <p class="eyebrow">UMA NOVA EXPERIÊNCIA À MESA</p>
        <h1>Descubra um jeito<br>mais inteligente de <em>comer bem.</em></h1>
        <p class="description">Cardápios digitais, experiências em 3D e recomendações que combinam com você. Estamos preparando algo especial.</p>
        <div class="hero-mark" aria-hidden="true">b.</div>
      </section>
      <footer><span>Boraê</span><span>Experiências gastronômicas mais inteligentes.</span></footer>
    </main>
  `,
  styleUrl: './app.css'
})
export class App {}
