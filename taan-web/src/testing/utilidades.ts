import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideApi } from '@taan/comun/api';

export const BASE = 'http://api';

/** Proveedores comunes de las pruebas de pantallas: HTTP simulado, cliente de la API y router. */
export const proveedoresDePrueba = [provideHttpClient(), provideHttpClientTesting(), provideApi(BASE), provideRouter([])];

export const backend = () => TestBed.inject(HttpTestingController);

export function escribir(fixture: ComponentFixture<unknown>, selector: string, valor: string): void {
  const campo = fixture.nativeElement.querySelector(selector) as HTMLInputElement | HTMLTextAreaElement;
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
}

export function pulsar(fixture: ComponentFixture<unknown>, texto: string): void {
  const boton = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) =>
    b.textContent?.includes(texto),
  );
  if (!boton) {
    throw new Error(`No hay un botón «${texto}»`);
  }
  boton.click();
}

export const texto = (fixture: ComponentFixture<unknown>) => (fixture.nativeElement as HTMLElement).textContent ?? '';

export const error = (status: number, codigo: string, mensaje = '') => ({
  cuerpo: { codigo, mensaje },
  opciones: { status, statusText: codigo },
});
