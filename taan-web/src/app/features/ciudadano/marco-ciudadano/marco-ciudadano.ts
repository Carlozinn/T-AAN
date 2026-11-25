import { formatDate } from '@angular/common';
import { ChangeDetectionStrategy, Component, LOCALE_ID, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { CuentaCiudadanaService } from '@taan/comun/api';
import { SesionService } from '@taan/comun/sesion/sesion.service';

/**
 * Marco del portal ciudadano. Consulta el perfil al entrar para avisar si la cuenta está
 * suspendida: puede consultar, pero no crear ni reabrir reportes (CU-04 FA-04, RF-043).
 */
@Component({
  selector: 'taan-marco-ciudadano',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './marco-ciudadano.html',
  styleUrl: './marco-ciudadano.scss',
})
export class MarcoCiudadano {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly cuenta = inject(CuentaCiudadanaService);
  private readonly locale = inject(LOCALE_ID);

  private readonly perfil = rxResource({ stream: () => this.cuenta.obtenerPerfil() });
  protected readonly avisoSuspension = computed(() => {
    const perfil = this.perfil.hasValue() ? this.perfil.value() : undefined;
    if (perfil?.estado !== 'suspendida') {
      return null;
    }
    const { hasta, motivo } = perfil.suspension ?? {};
    const vigencia = hasta ? `hasta el ${formatDate(hasta, 'longDate', this.locale)}` : 'por tiempo indefinido';
    return (
      `Tu cuenta está suspendida ${vigencia}.` +
      (motivo ? ` Motivo: ${motivo}.` : '') +
      ' Puedes consultar tus reportes, pero no crear ni reabrir reportes.'
    );
  });

  protected salir(): void {
    this.sesion.salir().subscribe(() => this.router.navigate(['/acceso']));
  }
}
