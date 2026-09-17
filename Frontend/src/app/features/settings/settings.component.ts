import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../core/auth.service';
import { Subscription, User } from '../../core/models';
import { SubscriptionService } from '../../core/subscription.service';

@Component({
  selector: 'app-settings',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h1 class="page-title">Settings</h1>
    <p class="page-sub">Manage your profile and view billing details.</p>

    <mat-card class="section">
      <mat-card-header><mat-card-title>Profile</mat-card-title></mat-card-header>
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="save()">
          <mat-form-field appearance="outline" class="full">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" />
            @if (form.controls.name.hasError('required')) {
              <mat-error>Name is required</mat-error>
            }
          </mat-form-field>

          @if (user(); as u) {
            <p class="muted">Email: {{ u.email }} · Role: {{ u.role }}</p>
          }

          <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || busy()">
            Save
          </button>
        </form>
      </mat-card-content>
    </mat-card>

    <mat-card class="section">
      <mat-card-header><mat-card-title>Billing</mat-card-title></mat-card-header>
      <mat-card-content>
        @if (subscription(); as sub) {
          <p>Plan: <strong>{{ sub.planName }}</strong> ({{ sub.status }})</p>
          <p>Renews: {{ sub.cancelAtPeriodEnd ? 'Will not renew' : (sub.currentPeriodEnd | date: 'mediumDate') }}</p>
        } @else {
          <p class="muted">No active subscription.</p>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .section { margin-bottom: 16px; }
      .full { width: 100%; max-width: 420px; display: block; }
      .muted { opacity: 0.7; }
    `,
  ],
})
export class SettingsComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly subscriptions = inject(SubscriptionService);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);

  readonly user = signal<User | null>(null);
  readonly subscription = signal<Subscription | null>(null);
  readonly busy = signal(false);

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
  });

  ngOnInit(): void {
    this.auth.loadCurrentUser().subscribe({
      next: (u) => {
        this.user.set(u);
        this.form.patchValue({ name: u.name ?? '' });
      },
      error: () => {},
    });
    this.subscriptions.getCurrent().subscribe({
      next: (sub) => this.subscription.set(sub),
      error: () => this.subscription.set(null),
    });
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }
    this.busy.set(true);
    this.auth.updateName(this.form.controls.name.value!).subscribe({
      next: (u) => {
        this.user.set(u);
        this.busy.set(false);
        this.snack.open('Profile updated', 'OK', { duration: 3000 });
      },
      error: () => {
        this.busy.set(false);
        this.snack.open('Update failed', 'Dismiss', { duration: 4000 });
      },
    });
  }
}
