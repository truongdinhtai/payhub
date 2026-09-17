import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../core/auth.service';

/**
 * Landing route after Google login. The backend redirects here with the JWT in
 * the URL fragment (#token=...). We read it, store it, clean the URL, then go to
 * the dashboard.
 */
@Component({
  selector: 'app-oauth-callback',
  imports: [MatProgressSpinnerModule],
  template: `
    <div class="wrap">
      <mat-spinner diameter="48"></mat-spinner>
      <p>Signing you in…</p>
    </div>
  `,
  styles: [`.wrap { min-height: 100vh; display: grid; place-items: center; gap: 16px; }`],
})
export class OauthCallbackComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    const token = this.extractToken();
    if (token) {
      this.auth.completeLogin(token);
      this.router.navigateByUrl('/dashboard');
    } else {
      this.router.navigateByUrl('/login');
    }
  }

  private extractToken(): string | null {
    const fragment = window.location.hash.startsWith('#')
      ? window.location.hash.substring(1)
      : window.location.hash;
    return new URLSearchParams(fragment).get('token');
  }
}
