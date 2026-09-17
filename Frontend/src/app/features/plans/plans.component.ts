import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Plan, PlanCode } from '../../core/models';
import { SubscriptionService } from '../../core/subscription.service';

@Component({
  selector: 'app-plans',
  imports: [MatButtonModule, MatProgressSpinnerModule],
  template: `
    <h1 class="page-title">Plans</h1>
    <p class="page-sub">Pick the plan that fits. Upgrade or downgrade anytime.</p>

    @if (loading()) {
      <mat-spinner diameter="40"></mat-spinner>
    } @else {
      <div class="grid">
        @for (plan of plans(); track plan.code) {
          <div class="surface plan" [class.popular]="plan.code === 'PRO'" [class.current]="plan.code === currentPlan()">
            @if (plan.code === 'PRO') {
              <span class="ribbon">Popular</span>
            }
            <div class="head">
              <div class="pname">{{ plan.name }}</div>
              <div class="price">
                @if (plan.priceCents === 0) {
                  <span class="amount">Free</span>
                } @else {
                  <span class="amount">\${{ (plan.priceCents / 100).toFixed(0) }}</span>
                  <span class="per">/mo</span>
                }
              </div>
              <div class="desc">{{ plan.description }}</div>
            </div>

            <ul class="features">
              <li>{{ limit(plan.maxProjects) }} projects</li>
              <li>{{ limit(plan.maxSeats) }} seats</li>
              <li>Full transaction history &amp; search</li>
            </ul>

            @if (plan.code === currentPlan()) {
              <button mat-stroked-button disabled class="cta">Current plan</button>
            } @else {
              <button
                mat-flat-button
                color="primary"
                class="cta"
                [disabled]="busy()"
                (click)="choose(plan.code)"
              >
                {{ currentPlan() ? 'Switch to ' + plan.name : 'Subscribe' }}
              </button>
            }
          </div>
        }
      </div>
    }

    @if (busy()) {
      <div class="overlay">
        <div class="overlay-card">
          <mat-spinner diameter="48"></mat-spinner>
          <p>Redirecting to secure checkout…</p>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 18px; align-items: stretch; }
      .plan { position: relative; padding: 24px 22px; display: flex; flex-direction: column; }
      .plan.popular { border-color: var(--brand-1); box-shadow: 0 8px 30px rgba(79, 70, 229, 0.16); }
      .plan.current { outline: 2px solid #cbd5e1; }
      .ribbon {
        position: absolute; top: 14px; right: 14px;
        background: var(--brand-grad); color: #fff; font-size: 11px; font-weight: 700;
        padding: 3px 10px; border-radius: 999px; letter-spacing: 0.03em;
      }
      .pname { font-weight: 700; color: var(--ink); font-size: 15px; }
      .price { margin: 10px 0 6px; }
      .amount { font-size: 34px; font-weight: 800; color: var(--ink); letter-spacing: -0.02em; }
      .per { color: var(--muted); font-weight: 600; margin-left: 2px; }
      .desc { color: var(--muted); font-size: 14px; min-height: 40px; }
      .features { list-style: none; padding: 0; margin: 14px 0 20px; flex: 1; }
      .features li { position: relative; padding: 6px 0 6px 26px; color: #334155; font-size: 14px; }
      .features li::before {
        content: "✓"; position: absolute; left: 0; top: 6px;
        color: var(--brand-1); font-weight: 800;
      }
      .cta { width: 100%; }

      .overlay {
        position: fixed;
        inset: 0;
        z-index: 1000;
        display: grid;
        place-items: center;
        background: rgba(15, 23, 42, 0.45);
        backdrop-filter: blur(3px);
        animation: fade 0.15s ease-out;
      }
      .overlay-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16px;
        padding: 32px 40px;
        background: #fff;
        border-radius: 16px;
        box-shadow: 0 20px 50px rgba(2, 6, 23, 0.25);
      }
      .overlay-card p { margin: 0; color: var(--ink); font-weight: 600; }
      @keyframes fade { from { opacity: 0; } to { opacity: 1; } }
    `,
  ],
})
export class PlansComponent implements OnInit {
  private readonly subscriptions = inject(SubscriptionService);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly plans = signal<Plan[]>([]);
  readonly currentPlan = signal<PlanCode | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);

  ngOnInit(): void {
    this.subscriptions.getPlans().subscribe({
      next: (plans) => {
        this.plans.set(plans);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.subscriptions.getCurrent().subscribe({
      next: (sub) => this.currentPlan.set(sub.planCode),
      error: () => this.currentPlan.set(null),
    });
  }

  limit(value: number): string {
    return value < 0 ? 'Unlimited' : String(value);
  }

  /**
   * Subscribe to the plan. Always tries POST subscribe first; if the user
   * already has a live subscription (409) it switches the plan instead. Paid
   * subscribes and paid upgrades hand off to Stripe Checkout.
   */
  choose(planCode: PlanCode): void {
    this.busy.set(true);
    this.subscriptions.subscribe(planCode).subscribe({
      next: (res) => {
        if (res.checkoutUrl) {
          window.location.href = res.checkoutUrl;
        } else {
          this.finish('Subscribed');
        }
      },
      error: (err) => {
        if (err?.status === 409) {
          this.changePlan(planCode);
        } else {
          this.fail();
        }
      },
    });
  }

  private changePlan(planCode: PlanCode): void {
    this.subscriptions.changePlan(planCode).subscribe({
      next: (res) => {
        if (res.checkoutUrl) {
          window.location.href = res.checkoutUrl; // upgrade -> Stripe Checkout
        } else {
          this.finish('Plan updated');
        }
      },
      error: () => this.fail(),
    });
  }

  private finish(message: string): void {
    this.busy.set(false);
    this.snack.open(message, 'OK', { duration: 3000 });
    this.router.navigateByUrl('/dashboard');
  }

  private fail(): void {
    this.busy.set(false);
    this.snack.open('Action failed', 'Dismiss', { duration: 4000 });
  }
}
