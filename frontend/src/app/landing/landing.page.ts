import { Component, OnInit, inject } from '@angular/core';
import { HeaderComponent } from './header.component';
import { HeroComponent } from './hero.component';
import { ProposalComponent } from './proposal.component';
import { ExperienceComponent } from './experience.component';
import { RestaurantsComponent } from './restaurants.component';
import { ConsumersComponent } from './consumers.component';
import { HowItWorksComponent } from './how-it-works.component';
import { EcosystemComponent } from './ecosystem.component';
import { PurposeComponent } from './purpose.component';
import { BlogSectionComponent } from '../blog/blog-section.component';
import { FaqComponent } from './faq.component';
import { CtaComponent } from './cta.component';
import { FooterComponent } from './footer.component';
import { warmUpApi } from '../shared/api-warmup';
import { BlogSeoService } from '../blog/blog-seo.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    HeaderComponent,
    HeroComponent,
    ProposalComponent,
    ExperienceComponent,
    RestaurantsComponent,
    ConsumersComponent,
    HowItWorksComponent,
    EcosystemComponent,
    PurposeComponent,
    BlogSectionComponent,
    FaqComponent,
    CtaComponent,
    FooterComponent
  ],
  template: `
    <div class="landing-layout">
      <!-- Seção 1: Header e Navegação -->
      <app-header />

      <main id="main-content">
        <!-- Seção 2: Hero Principal -->
        <app-hero />

        <!-- Seção 3: Apresentação da Proposta -->
        <app-proposal />

        <!-- Seção 4: Experiência Gastronômica -->
        <app-experience />

        <!-- Seção 5: Para Restaurantes -->
        <app-restaurants />

        <!-- Seção 6: Para Consumidores -->
        <app-consumers />

        <!-- Seção 7: Como o Boraê Funcionará -->
        <app-how-it-works />

        <!-- Seção 8: O Ecossistema Boraê -->
        <app-ecosystem />

        <!-- Seção 9: Identidade e Propósito -->
        <app-purpose />

        <!-- Seção 10: Conteúdo Editorial / Blog Boraê -->
        <app-blog-section />

        <!-- Seção 11: Perguntas Frequentes -->
        <app-faq />

        <!-- Seção 12: CTA Final -->
        <app-cta />
      </main>

      <!-- Seção 13: Footer Institucional -->
      <app-footer />
    </div>
  `,
  styles: [`
    :host {
      display: block;
      background-color: var(--color-white);
      color: var(--color-text-main);
    }

    .landing-layout {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    main {
      flex: 1;
    }
  `]
})
export class LandingPage implements OnInit {
  private readonly seoService = inject(BlogSeoService);

  ngOnInit(): void {
    // Começa a reativar a API (plano gratuito do Render) antes de a pessoa ir para o login.
    warmUpApi();
    this.seoService.setLandingSeo();
  }
}
