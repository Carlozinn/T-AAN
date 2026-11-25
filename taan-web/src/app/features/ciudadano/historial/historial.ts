import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { EventoPublico } from '@taan/comun/api';

/** Historial público de un reporte: solo los eventos visibles para el ciudadano. */
@Component({
  selector: 'taan-historial',
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ol class="historial">
      @for (evento of eventos(); track $index) {
        <li>
          <time [attr.datetime]="evento.fecha">{{ evento.fecha | date: 'medium' }}</time>
          <strong>{{ evento.estadoTexto }}</strong>
          @if (evento.descripcion) {
            <span>{{ evento.descripcion }}</span>
          }
        </li>
      }
    </ol>
  `,
  styles: `
    .historial { margin: 0; padding-left: 1.25rem; display: grid; gap: 0.5rem; }
    li { display: grid; }
    time { color: var(--color-suave); font-size: 0.9rem; }
  `,
})
export class Historial {
  readonly eventos = input.required<EventoPublico[]>();
}
