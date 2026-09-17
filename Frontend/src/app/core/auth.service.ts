import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { User } from './models';

const TOKEN_KEY = 'payhub_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = environment.apiBaseUrl;

  /** Current user profile, or null when unknown/logged out. */
  readonly currentUser = signal<User | null>(null);

  getToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /** Full-page redirect to the backend to start Google OAuth2 login. */
  startLogin(): void {
    window.location.href = `${this.base}/oauth2/authorization/google`;
  }

  /** Called by the OAuth callback route once it has read the token from the URL. */
  completeLogin(token: string): void {
    this.storeToken(token);
  }

  loadCurrentUser(): Observable<User> {
    return this.http
      .get<User>(`${this.base}/api/v1/me`)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  updateName(name: string): Observable<User> {
    return this.http
      .patch<User>(`${this.base}/api/v1/me`, { name })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  logout(): void {
    this.clearToken();
    this.currentUser.set(null);
    this.router.navigateByUrl('/login');
  }

  private storeToken(token: string): void {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch {
      /* ignore storage errors (private mode) */
    }
  }

  private clearToken(): void {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      /* ignore */
    }
  }
}
