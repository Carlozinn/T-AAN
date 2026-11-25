import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { Captcha } from '../../../compartido/captcha';
import { BASE, backend, error, escribir, proveedoresDePrueba, texto } from '../../../../testing/utilidades';
import { Registro } from './registro';

describe('Registro', () => {
  let fixture: ComponentFixture<Registro>;

  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: proveedoresDePrueba });
    fixture = TestBed.createComponent(Registro);
    await fixture.whenStable();
  });

  function llenar(): void {
    escribir(fixture, '#nombre', 'Ana López');
    escribir(fixture, '#correo', 'ana@correo.mx');
    escribir(fixture, '#password', 'secreta123');
    fixture.nativeElement.querySelector('#aviso').click();
  }

  const resolverCaptcha = (token: string | null) =>
    (fixture.debugElement.query(By.directive(Captcha)).componentInstance as Captcha).token.emit(token);

  const enviar = () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    TestBed.tick();
  };

  it('envía los datos con el token del CAPTCHA y el campo trampa vacío, y pide revisar el correo', async () => {
    llenar();
    resolverCaptcha('token-captcha');
    enviar();

    const peticion = backend().expectOne(`${BASE}/auth/ciudadanos/registro`);
    expect(peticion.request.body).toEqual({
      nombre: 'Ana López',
      correo: 'ana@correo.mx',
      password: 'secreta123',
      aceptaAvisoPrivacidad: true,
      campoAuxiliar: '',
      captchaToken: 'token-captcha',
    });
    peticion.flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Te enviamos un enlace a ana@correo.mx');
    expect(texto(fixture)).toContain('Enviar un nuevo enlace');
  });

  it('sin la verificación antiabuso no envía el registro', async () => {
    llenar();
    enviar();
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Completa la verificación para continuar.');
    backend().expectNone(`${BASE}/auth/ciudadanos/registro`);
  });

  it('sin aceptar el aviso de privacidad no envía el registro', async () => {
    escribir(fixture, '#nombre', 'Ana López');
    escribir(fixture, '#correo', 'ana@correo.mx');
    escribir(fixture, '#password', 'secreta123');
    resolverCaptcha('token-captcha');
    enviar();
    await fixture.whenStable();
    expect(texto(fixture)).toContain('necesitas aceptar el aviso de privacidad');
    backend().expectNone(`${BASE}/auth/ciudadanos/registro`);
  });

  it('si el correo ya existe lo informa, conserva lo capturado y pide una nueva verificación', async () => {
    llenar();
    resolverCaptcha('token-captcha');
    enviar();
    const { cuerpo, opciones } = error(409, 'CORREO_REGISTRADO');
    backend().expectOne(`${BASE}/auth/ciudadanos/registro`).flush(cuerpo, opciones);
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Ese correo ya tiene una cuenta.');
    expect(fixture.nativeElement.querySelector('#nombre').value).toBe('Ana López');

    enviar();
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Completa la verificación para continuar.');
  });
});
