import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { PageResponse, Transaction, TransactionQuery } from './models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  searchTransactions(query: TransactionQuery): Observable<PageResponse<Transaction>> {
    let params = new HttpParams();
    if (query.q) {
      params = params.set('q', query.q);
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.from) {
      params = params.set('from', query.from);
    }
    if (query.to) {
      params = params.set('to', query.to);
    }
    params = params.set('page', String(query.page ?? 0));
    params = params.set('size', String(query.size ?? 10));

    return this.http.get<PageResponse<Transaction>>(`${this.base}/transactions`, { params });
  }
}
