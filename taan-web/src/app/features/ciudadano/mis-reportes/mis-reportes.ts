import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ReporteResumenCiudadano, ReportesCiudadanosService } from '@taan/comun/api';
import { leerApiError } from '@taan/comun/errores/api-error';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';

const TAMANO_PAGINA = 20;

/** «Mis reportes» (CU-02): los reportes propios, del más reciente al más antiguo, por páginas. */
@Component({
  selector: 'taan-mis-reportes',
  imports: [RouterLink, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './mis-reportes.html',
  styleUrl: './mis-reportes.scss',
})
export class MisReportes {
  private readonly api = inject(ReportesCiudadanosService);
  private siguientePagina = 0;

  protected readonly reportes = signal<ReporteResumenCiudadano[]>([]);
  protected readonly hayMas = signal(false);
  protected readonly cargando = signal(false);
  protected readonly cargado = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.cargarMas();
  }

  protected cargarMas(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.api
      .listarMisReportes(this.siguientePagina, TAMANO_PAGINA)
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (pagina) => {
          this.reportes.update((actuales) => [...actuales, ...pagina.contenido]);
          this.siguientePagina = pagina.pagina + 1;
          this.hayMas.set(this.siguientePagina < pagina.totalPaginas);
          this.cargado.set(true);
        },
        error: (e) => this.error.set(mensajeDeError(leerApiError(e))),
      });
  }
}
