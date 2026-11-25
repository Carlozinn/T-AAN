import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';

import { environment } from '../../environments/environment';

interface Turnstile {
  render(elemento: HTMLElement, opciones: object): string;
  reset(widgetId: string): void;
  remove(widgetId: string): void;
}

declare global {
  interface Window {
    turnstile?: Turnstile;
  }
}

const SCRIPT = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
let carga: Promise<Turnstile> | undefined;

function cargarTurnstile(): Promise<Turnstile> {
  carga ??= new Promise((resolver, rechazar) => {
    const script = document.createElement('script');
    script.src = SCRIPT;
    script.async = true;
    script.onload = () => (window.turnstile ? resolver(window.turnstile) : rechazar(new Error('Turnstile')));
    script.onerror = () => {
      carga = undefined;
      rechazar(new Error('Turnstile'));
    };
    document.head.append(script);
  });
  return carga;
}

/**
 * Verificación antiabuso de baja fricción (RNF-022) con Cloudflare Turnstile, que incluye su
 * alternativa accesible. Emite el token, o null cuando vence o falla.
 */
@Component({
  selector: 'taan-captcha',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div #widget></div>
    @if (sinCarga()) {
      <p class="error-campo">No pudimos cargar la verificación. Revisa tu conexión y recarga la página.</p>
    }
  `,
})
export class Captcha {
  readonly token = output<string | null>();
  protected readonly sinCarga = signal(false);

  private readonly widget = viewChild.required<ElementRef<HTMLDivElement>>('widget');
  private widgetId: string | undefined;
  private turnstile: Turnstile | undefined;

  constructor() {
    afterNextRender(() => {
      cargarTurnstile()
        .then((turnstile) => {
          this.turnstile = turnstile;
          this.widgetId = turnstile.render(this.widget().nativeElement, {
            sitekey: environment.turnstileSiteKey,
            language: 'es',
            callback: (token: string) => this.token.emit(token),
            'expired-callback': () => this.token.emit(null),
            'error-callback': () => this.token.emit(null),
          });
        })
        .catch(() => this.sinCarga.set(true));
    });
    inject(DestroyRef).onDestroy(() => this.widgetId && this.turnstile?.remove(this.widgetId));
  }

  /** Cada token sirve una sola vez: se pide otro después de un intento fallido. */
  reiniciar(): void {
    this.token.emit(null);
    if (this.widgetId) {
      this.turnstile?.reset(this.widgetId);
    }
  }
}
