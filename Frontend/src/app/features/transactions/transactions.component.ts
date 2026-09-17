import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import { Transaction, TransactionStatus } from '../../core/models';
import { PaymentService } from '../../core/payment.service';

@Component({
  selector: 'app-transactions',
  providers: [provideNativeDateAdapter()],
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  template: `
    <h1 class="page-title">Transactions</h1>
    <p class="page-sub">Search and filter your payment history.</p>

    <form class="surface filters" [formGroup]="form" (ngSubmit)="applyFilters()">
      <mat-form-field appearance="outline" class="grow">
        <mat-label>Search</mat-label>
        <input matInput formControlName="q" placeholder="Customer, email, description" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Status</mat-label>
        <mat-select formControlName="status">
          <mat-option value="">Any</mat-option>
          @for (s of statuses; track s) {
            <mat-option [value]="s">{{ s }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>From</mat-label>
        <input matInput [matDatepicker]="fromPicker" formControlName="from" />
        <mat-datepicker-toggle matIconSuffix [for]="fromPicker"></mat-datepicker-toggle>
        <mat-datepicker #fromPicker></mat-datepicker>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>To</mat-label>
        <input matInput [matDatepicker]="toPicker" formControlName="to" />
        <mat-datepicker-toggle matIconSuffix [for]="toPicker"></mat-datepicker-toggle>
        <mat-datepicker #toPicker></mat-datepicker>
      </mat-form-field>

      <div class="actions">
        <button mat-flat-button color="primary" type="submit">Search</button>
        <button mat-button type="button" (click)="reset()">Clear</button>
      </div>
    </form>

    @if (loading()) {
      <mat-spinner diameter="40"></mat-spinner>
    } @else {
      <div class="surface table-wrap">
        <table mat-table [dataSource]="transactions()" class="full">
          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Date</th>
            <td mat-cell *matCellDef="let t">{{ t.createdAt | date: 'medium' }}</td>
          </ng-container>
          <ng-container matColumnDef="customerName">
            <th mat-header-cell *matHeaderCellDef>Customer</th>
            <td mat-cell *matCellDef="let t">{{ t.customerName }}</td>
          </ng-container>
          <ng-container matColumnDef="description">
            <th mat-header-cell *matHeaderCellDef>Description</th>
            <td mat-cell *matCellDef="let t">{{ t.description }}</td>
          </ng-container>
          <ng-container matColumnDef="amount">
            <th mat-header-cell *matHeaderCellDef>Amount</th>
            <td mat-cell *matCellDef="let t">{{ amount(t) }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let t"><span class="chip {{ chip(t.status) }}">{{ t.status }}</span></td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table>

        @if (transactions().length === 0) {
          <p class="empty">No transactions found.</p>
        }

        <mat-paginator
          [length]="total()"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          [pageSizeOptions]="[5, 10, 25]"
          (page)="onPage($event)"
        ></mat-paginator>
      </div>
    }
  `,
  styles: [
    `
      .filters { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; padding: 16px 16px 4px; margin-bottom: 18px; }
      .filters mat-form-field { min-width: 160px; }
      .filters .grow { flex: 1 1 220px; }
      .actions { display: flex; gap: 8px; padding-bottom: 12px; }
      .table-wrap { padding: 4px 8px 8px; overflow: hidden; }
      table.full { width: 100%; background: transparent; }
      .empty { opacity: 0.7; padding: 24px 16px; text-align: center; }
    `,
  ],
})
export class TransactionsComponent implements OnInit {
  private readonly payments = inject(PaymentService);
  private readonly fb = inject(FormBuilder);

  readonly statuses: TransactionStatus[] = ['PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED'];
  readonly columns = ['createdAt', 'customerName', 'description', 'amount', 'status'];

  readonly transactions = signal<Transaction[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly loading = signal(true);

  readonly form = this.fb.group({
    q: [''],
    status: [''],
    from: [null as Date | null],
    to: [null as Date | null],
  });

  ngOnInit(): void {
    this.search();
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.search();
  }

  reset(): void {
    this.form.reset({ q: '', status: '', from: null, to: null });
    this.pageIndex.set(0);
    this.search();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.search();
  }

  amount(t: Transaction): string {
    return `${(t.amountCents / 100).toFixed(2)} ${t.currency}`;
  }

  chip(status: TransactionStatus): string {
    switch (status) {
      case 'SUCCEEDED':
        return 'green';
      case 'PENDING':
        return 'amber';
      case 'FAILED':
        return 'red';
      default:
        return 'gray';
    }
  }

  private search(): void {
    this.loading.set(true);
    const { q, status, from, to } = this.form.value;

    this.payments
      .searchTransactions({
        q: q ?? '',
        status: (status ?? '') as TransactionStatus | '',
        from: from ? new Date(from).toISOString() : undefined,
        to: to ? new Date(to).toISOString() : undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
      })
      .subscribe({
        next: (page) => {
          this.transactions.set(page.content);
          this.total.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.transactions.set([]);
          this.total.set(0);
          this.loading.set(false);
        },
      });
  }
}
