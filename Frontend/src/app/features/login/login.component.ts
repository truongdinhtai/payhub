import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  template: `
    <div class="page">
      <div class="card">
        <div class="brand">
          <span class="logo">P</span>
          <span class="name">PayHub</span>
        </div>
        <h1>Welcome back</h1>
        <p class="sub">Manage your subscription and billing in one place.</p>

        <button class="google-btn" type="button" (click)="auth.startLogin()">
          <svg class="g" viewBox="0 0 48 48" aria-hidden="true">
            <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
            <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
            <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
            <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
          </svg>
          <span>Sign in with Google</span>
        </button>

        <p class="terms">By continuing you agree to our Terms and Privacy Policy.</p>
      </div>
    </div>
  `,
  styles: [
    `
      .page {
        min-height: 100vh;
        display: grid;
        place-items: center;
        padding: 24px;
        background: linear-gradient(135deg, #eef2ff 0%, #f8fafc 45%, #ecfeff 100%);
      }
      .card {
        width: 100%;
        max-width: 400px;
        background: #fff;
        border-radius: 16px;
        padding: 40px 32px;
        box-shadow: 0 10px 30px rgba(2, 6, 23, 0.08), 0 2px 8px rgba(2, 6, 23, 0.04);
        text-align: center;
      }
      .brand {
        display: inline-flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 24px;
      }
      .logo {
        width: 40px;
        height: 40px;
        display: grid;
        place-items: center;
        border-radius: 10px;
        background: linear-gradient(135deg, #4f46e5, #06b6d4);
        color: #fff;
        font-weight: 700;
        font-size: 22px;
      }
      .name {
        font-size: 22px;
        font-weight: 700;
        color: #0f172a;
      }
      h1 {
        margin: 0 0 6px;
        font-size: 24px;
        font-weight: 600;
        color: #0f172a;
      }
      .sub {
        margin: 0 0 28px;
        color: #64748b;
        font-size: 15px;
      }
      .google-btn {
        width: 100%;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        height: 48px;
        padding: 0 16px;
        border: 1px solid #dadce0;
        border-radius: 10px;
        background: #fff;
        color: #3c4043;
        font-family: 'Roboto', system-ui, sans-serif;
        font-size: 15px;
        font-weight: 500;
        cursor: pointer;
        transition: box-shadow 0.15s, background 0.15s, border-color 0.15s;
      }
      .google-btn:hover {
        background: #f8faff;
        border-color: #c9ccd1;
        box-shadow: 0 1px 3px rgba(60, 64, 67, 0.15);
      }
      .google-btn:active {
        background: #f1f5ff;
      }
      .g {
        width: 20px;
        height: 20px;
        flex: 0 0 auto;
      }
      .terms {
        margin: 24px 0 0;
        font-size: 12px;
        color: #94a3b8;
      }
    `,
  ],
})
export class LoginComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.router.navigateByUrl('/dashboard');
    }
  }
}
