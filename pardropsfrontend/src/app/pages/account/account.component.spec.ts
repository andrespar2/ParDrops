/**
 * @file src/app/pages/account/account.component.spec.ts
 * @description Pruebas del avatar de Google y de su respaldo en la cuenta.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { computed, signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AccountComponent } from './account.component';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { User } from '../../core/models/user.model';

const BASE_USER: User = {
  id: 1,
  email: 'cliente@pardrops.com',
  fullName: 'Cliente Prueba',
  role: 'CUSTOMER',
  provider: 'GOOGLE'
};

describe('AccountComponent', () => {
  let userSignal: ReturnType<typeof signal<User | null>>;

  function setup(user: User): ComponentFixture<AccountComponent> {
    userSignal = signal<User | null>(user);
    const authStub = {
      user: userSignal.asReadonly(),
      isAdmin: computed(() => false),
      isSeller: computed(() => false),
      logout: vi.fn()
    };
    const orderStub = {
      myOrders: signal([]).asReadonly(),
      loadMyOrders: () => of([])
    };

    TestBed.configureTestingModule({
      imports: [AccountComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authStub },
        { provide: OrderService, useValue: orderStub }
      ]
    });

    const fixture = TestBed.createComponent(AccountComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('muestra la foto de Google cuando la cuenta tiene avatar', () => {
    const fixture = setup({
      ...BASE_USER,
      avatarUrl: 'https://foto.example/cliente'
    });

    const avatar = fixture.nativeElement.querySelector('img.profile__avatar') as HTMLImageElement;
    expect(avatar).toBeTruthy();
    expect(avatar.getAttribute('src')).toBe('https://foto.example/cliente');
    expect(avatar.getAttribute('alt')).toContain('Cliente Prueba');
  });

  it('usa la inicial como respaldo si no hay foto', () => {
    const fixture = setup(BASE_USER);

    const avatar = fixture.nativeElement.querySelector('div.profile__avatar') as HTMLElement;
    expect(avatar).toBeTruthy();
    expect(avatar.textContent?.trim()).toBe('C');
    expect(fixture.nativeElement.querySelector('img.profile__avatar')).toBeNull();
  });
});
