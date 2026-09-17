import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Plan, Subscription, SubscriptionStatus } from '../../core/models';
import { SubscriptionService } from '../../core/subscription.service';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, RouterLink, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <h1 class="page-title">Dashboard</h1>
    <p class="page-sub">Your subscription and usage at a glance.</p>

    @if (loading()) {
      <mat-spinner diameter="40"></mat-spinner>
    } @else if (subscription(); as sub) {
      <section class="hero surface">
        <div class="banner">
          <div class="banner-inner">
            <div class="plan">{{ sub.planName }} plan</div>
            <span class="chip {{ chip(sub.status) }}">{{ sub.status }}</span>
          </div>
        </div>

        <div class="body">
          <div class="stats">
            <div class="stat">
              <div class="label">Renews on</div>
              <div class="value">{{ sub.currentPeriodEnd | date: 'mediumDate' }}</div>
            </div>
            <div class="stat">
              <div class="label">Projects</div>
              <div class="value">{{ limit(planLimits()?.maxProjects) }}</div>
            </div>
            <div class="stat">
              <div class="label">Seats</div>
              <div class="value">{{ limit(planLimits()?.maxSeats) }}</div>
            </div>
          </div>

          <div class="actions">
            <a mat-flat-button color="primary" routerLink="/plans">Change plan</a>
            <button mat-stroked-button (click)="cancel()" [disabled]="busy()">Cancel subscription</button>
          </div>
        </div>
      </section>
    } @else {
      <section class="surface empty">
        <div class="empty-icon">✦</div>
        <h2>No active subscription</h2>
        <p>Choose a plan to get started with PayHub.</p>
        <a mat-flat-button color="primary" routerLink="/plans">Browse plans</a>
      </section>
    }
  `,
  styles: [
    `
      .hero { overflow: hidden; }
      .banner { background: var(--brand-grad); padding: 28px 28px; }
      .banner-inner { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
      .plan { color: #fff; font-size: 22px; font-weight: 700; }
      .banner .chip { background: rgba(255, 255, 255, 0.9); color: #0f172a; }
      .body { padding: 24px 28px 28px; }
      .actions { display: flex; gap: 12px; margin-top: 24px; flex-wrap: wrap; }
      .empty { text-align: center; padding: 56px 24px; }
      .empty-icon {
        width: 56px; height: 56px; margin: 0 auto 12px; display: grid; place-items: center;
        border-radius: 16px; background: #eef2ff; color: var(--brand-1); font-size: 26px;
      }
      .empty h2 { margin: 0 0 6px; color: var(--ink); }
      .empty p { color: var(--muted); margin: 0 0 20px; }
    `,
  ],
})
export class DashboardComponent implements OnInit {
  private readonly subscriptions = inject(SubscriptionService);
  private readonly snack = inject(MatSnackBar);

  readonly subscription = signal<Subscription | null>(null);
  readonly planLimits = signal<Plan | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);

  private plansList: Plan[] = [];

  ngOnInit(): void {
    this.subscriptions.getPlans().subscribe({
      next: (plans) => {
        this.plansList = plans;
        this.recomputeLimits();
      },
      error: () => {},
    });
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.subscriptions.getCurrent().subscribe({
      next: (sub) => {
        this.subscription.set(sub);
        this.recomputeLimits();
        this.loading.set(false);
      },
      error: () => {
        this.subscription.set(null);
        this.loading.set(false);
      },
    });
  }

  private recomputeLimits(): void {
    const code = this.subscription()?.planCode;
    this.planLimits.set(this.plansList.find((p) => p.code === code) ?? null);
  }

  chip(status: SubscriptionStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'green';
      case 'INCOMPLETE':
        return 'amber';
      case 'PAST_DUE':
        return 'red';
      default:
        return 'gray';
    }
  }

  limit(value: number | undefined): string {
    if (value === undefined || value === null) {
      return '—';
    }
    return value < 0 ? 'Unlimited' : String(value);
  }

  cancel(): void {
    this.busy.set(true);
    this.subscriptions.cancel().subscribe({
      next: () => {
        this.subscription.set(null);
        this.busy.set(false);
        this.snack.open('Subscription canceled', 'OK', { duration: 4000 });
      },
      error: () => {
        this.busy.set(false);
        this.snack.open('Could not cancel subscription', 'Dismiss', { duration: 4000 });
      },
    });
  }
}
