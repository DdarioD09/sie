import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from './auth.models';

const TOKEN_KEY = 'stockapp.token';
const USERNAME_KEY = 'stockapp.username';
const ROLE_KEY = 'stockapp.role';

/**
 * Holds the JWT in memory (via signals) and mirrors it to localStorage so a page refresh doesn't
 * log you out. Learning note - trade-off: localStorage is readable by any script on the page, so a
 * successful XSS attack can steal the token; an httpOnly cookie set by the server would be safer
 * but needs the backend to set/read cookies instead of a bearer header. For a 2-person internal
 * tool behind your own login, this is a reasonable simplification - revisit it if this app ever
 * takes untrusted user input that gets rendered as HTML.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly usernameSignal = signal<string | null>(localStorage.getItem(USERNAME_KEY));
  private readonly roleSignal = signal<string | null>(localStorage.getItem(ROLE_KEY));

  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);
  readonly username = this.usernameSignal.asReadonly();
  readonly role = this.roleSignal.asReadonly();

  constructor(private readonly http: HttpClient) {}

  get token(): string | null {
    return this.tokenSignal();
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', request).pipe(
      tap((response) => {
        localStorage.setItem(TOKEN_KEY, response.token);
        localStorage.setItem(USERNAME_KEY, response.username);
        localStorage.setItem(ROLE_KEY, response.role);
        this.tokenSignal.set(response.token);
        this.usernameSignal.set(response.username);
        this.roleSignal.set(response.role);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    this.tokenSignal.set(null);
    this.usernameSignal.set(null);
    this.roleSignal.set(null);
  }
}
