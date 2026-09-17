// API DTOs mirrored from the Spring Boot backend.

export type Role = 'USER' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  name: string | null;
  avatarUrl: string | null;
  role: Role;
  createdAt: string;
}

export type PlanCode = 'FREE' | 'PRO' | 'ENTERPRISE';

export interface Plan {
  code: PlanCode;
  name: string;
  description: string | null;
  priceCents: number;
  currency: string;
  maxProjects: number; // -1 = unlimited
  maxSeats: number; // -1 = unlimited
}

export type SubscriptionStatus = 'INCOMPLETE' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED';

export interface Subscription {
  id: string;
  planCode: PlanCode;
  planName: string;
  status: SubscriptionStatus;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
  canceledAt: string | null;
  createdAt: string;
}

export interface SubscribeResponse {
  subscription: Subscription;
  checkoutUrl: string | null;
}

export type TransactionStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED';

export interface Transaction {
  id: string;
  subscriptionId: string | null;
  amountCents: number;
  currency: string;
  status: TransactionStatus;
  description: string | null;
  customerName: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TransactionQuery {
  q?: string;
  status?: TransactionStatus | '';
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
