import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule],
  template: `
    <header class="topbar">
      <div class="bar">
        <a class="brand" routerLink="/dashboard">
          <span class="logo">P</span>
          <span class="name">PayHub</span>
        </a>
        <nav class="nav">
          <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/plans" routerLinkActive="active">Plans</a>
          <a routerLink="/transactions" routerLinkActive="active">Transactions</a>
          <a routerLink="/settings" routerLinkActive="active">Settings</a>
        </nav>
        <span class="spacer"></span>
        @if (auth.currentUser(); as user) {
          <span class="user">{{ user.name || user.email }}</span>
        }
        <button mat-icon-button (click)="auth.logout()" aria-label="Log out" title="Log out">
          <mat-icon>logout</mat-icon>
        </button>
      </div>
    </header>

    <main class="content">
      <router-outlet />
    </main>
  `,
  styles: [
    `
      .topbar {
        position: sticky;
        top: 0;
        z-index: 10;
        background: rgba(255, 255, 255, 0.85);
        backdrop-filter: saturate(1.2) blur(8px);
        border-bottom: 1px solid var(--line);
      }
      .bar {
        max-width: 1040px;
        margin: 0 auto;
        height: 60px;
        padding: 0 16px;
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .brand { display: inline-flex; align-items: center; gap: 10px; text-decoration: none; }
      .logo {
        width: 32px; height: 32px; display: grid; place-items: center;
        border-radius: 9px; background: var(--brand-grad);
        color: #fff; font-weight: 700; font-size: 18px;
      }
      .name { font-weight: 700; font-size: 18px; color: var(--ink); }
      .nav { display: flex; gap: 2px; margin-left: 20px; }
      .nav a {
        padding: 7px 14px; border-radius: 999px; text-decoration: none;
        color: var(--muted); font-weight: 500; font-size: 14px; transition: background 0.12s, color 0.12s;
      }
      .nav a:hover { background: #eef2ff; color: var(--brand-1); }
      .nav a.active { background: #eef2ff; color: var(--brand-1); }
      .spacer { flex: 1 1 auto; }
      .user { color: var(--muted); font-size: 14px; margin-right: 4px; }
      .content { max-width: 1040px; margin: 0 auto; padding: 28px 16px 48px; }
      @media (max-width: 720px) {
        .nav { margin-left: 6px; }
        .nav a { padding: 7px 10px; }
        .name, .user { display: none; }
      }
    `,
  ],
})
export class ShellComponent implements OnInit {
  readonly auth = inject(AuthService);

  ngOnInit(): void {
    if (!this.auth.currentUser()) {
      this.auth.loadCurrentUser().subscribe({ error: () => {} });
    }
  }
}
