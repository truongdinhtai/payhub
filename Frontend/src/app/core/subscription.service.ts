import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Plan, PlanCode, SubscribeResponse, Subscription } from './models';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  getPlans(): Observable<Plan[]> {
    return this.http.get<Plan[]>(`${this.base}/plans`);
  }

  getCurrent(): Observable<Subscription> {
    return this.http.get<Subscription>(`${this.base}/subscriptions/current`);
  }

  subscribe(planCode: PlanCode): Observable<SubscribeResponse> {
    return this.http.post<SubscribeResponse>(`${this.base}/subscriptions`, { planCode });
  }

  changePlan(planCode: PlanCode): Observable<SubscribeResponse> {
    return this.http.put<SubscribeResponse>(`${this.base}/subscriptions/current/plan`, { planCode });
  }

  cancel(): Observable<Subscription> {
    return this.http.delete<Subscription>(`${this.base}/subscriptions/current`);
  }
}
