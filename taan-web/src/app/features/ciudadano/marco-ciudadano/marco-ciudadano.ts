import { ChangeDetectionStrategy, Component, LOCALE_ID, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { PerfilCiudadano, describirSuspension } from '@taan/comun/cuenta/perfil-ciudadano';
import { SesionService } from '@taan/comun/sesion/sesion.service';

/**
 * Marco del portal ciudadano. Provee el perfil a las pantallas hijas y avisa si la cuenta está
 * suspendida: puede consultar, pero no crear ni reabrir reportes (CU-04 FA-04, RF-043).
 */
@Component({
  selector: 'taan-marco-ciudadano',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  providers: [PerfilCiudadano],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './marco-ciudadano.html',
  styleUrl: './marco-ciudadano.scss',
})
export class MarcoCiudadano {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly perfil = inject(PerfilCiudadano);
  private readonly locale = inject(LOCALE_ID);

  protected readonly avisoSuspension = computed(() => {
    const texto = describirSuspension(this.perfil.perfil(), this.locale);
    return texto && `${texto} Puedes consultar tus reportes, pero no crear ni reabrir reportes.`;
  });

  protected salir(): void {
    this.sesion.salir().subscribe(() => this.router.navigate(['/acceso']));
  }
}
